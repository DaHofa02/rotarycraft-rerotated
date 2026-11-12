/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Farming.TileEntityGroundHydrator - needs no power; fed with water from pipes into its
 * 1000 mB tank. Every tick with a chance of 1/2 (and at least 25 mB in the tank) it picks a spot on its own level in the 13x13 area
 * around it, weighted towards the middle (AREA: 9 next to it, 1 in the corners, never itself); farmland there gets one step wetter,
 * for 25 mB of water. Not ported: Forestry humus and bog earth, Mystcraft's EnvAccel.
 */
public class GroundHydratorBlockEntity extends RotaryCraftBlockEntity implements PipeConnector {

	public static final int CAPACITY = 1000;
	public static final int FLUID_PER_BLOCK = 25;

	private static final int[][] AREA = {
			{1, 1, 1, 1, 2, 2, 3, 2, 2, 1, 1, 1, 1},
			{1, 1, 1, 2, 2, 3, 4, 3, 2, 2, 1, 1, 1},
			{1, 1, 2, 3, 5, 6, 6, 6, 5, 3, 2, 1, 1},
			{1, 2, 3, 4, 6, 7, 7, 7, 6, 4, 3, 2, 1},
			{2, 2, 4, 6, 7, 8, 8, 8, 7, 6, 4, 2, 2},
			{2, 3, 6, 7, 8, 9, 9, 9, 8, 7, 6, 3, 2},
			{3, 4, 6, 7, 8, 9, 0, 9, 8, 7, 6, 4, 3},
			{2, 3, 6, 7, 8, 9, 9, 9, 8, 7, 6, 3, 2},
			{2, 2, 4, 6, 7, 8, 8, 8, 7, 6, 4, 2, 2},
			{1, 2, 3, 5, 6, 7, 7, 7, 6, 5, 3, 2, 1},
			{1, 1, 2, 3, 4, 6, 6, 6, 4, 3, 2, 1, 1},
			{1, 1, 1, 2, 2, 3, 4, 3, 2, 2, 1, 1, 1},
			{1, 1, 1, 1, 2, 2, 3, 2, 2, 1, 1, 1, 1}
	};
	private static final int TOTAL_WEIGHT;
	static {
		int sum = 0;
		for (int[] row : AREA)
			for (int w : row)
				sum += w;
		TOTAL_WEIGHT = sum;
	}

	private final FluidTank tank = new FluidTank(CAPACITY, fs -> fs.is(Fluids.WATER));

	public GroundHydratorBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.HYDRATOR.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Ground Hydrator";
	}

	public static int getRange() {
		return (AREA.length-1)/2;
	}

	/** The weight of the spot (dx, dz) from the hydrator (original WeightedRandom.fromArray: x along the rows). */
	public static int getWeight(int dx, int dz) {
		int r = getRange();
		return Math.abs(dx) > r || Math.abs(dz) > r ? 0 : AREA[dx+r][dz+r];
	}

	public int getWaterLevel() {
		return tank.getFluidAmount();
	}

	@Nullable
	public Fluid getFluid() {
		return tank.isEmpty() ? null : tank.getFluid().getFluid();
	}

	public void addWater(int amt) {
		tank.fill(new FluidStack(Fluids.WATER, amt), IFluidHandler.FluidAction.EXECUTE);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (world.isClientSide || tank.getFluidAmount() < FLUID_PER_BLOCK || rand.nextInt(2) != 0)
			return;
		int pick = rand.nextInt(TOTAL_WEIGHT);
		int r = getRange();
		for (int i = 0; i < AREA.length; i++) {
			for (int k = 0; k < AREA[i].length; k++) {
				pick -= AREA[i][k];
				if (pick < 0) {
					if (this.hydrate(world, pos.offset(i-r, 0, k-r))) {
						tank.drain(FLUID_PER_BLOCK, IFluidHandler.FluidAction.EXECUTE);
						this.syncAllData();
					}
					return;
				}
			}
		}
	}

	/** Original ReikaWorldHelper.hydrateFarmland(..., false): one step wetter, up to 7. */
	private static boolean hydrate(Level world, BlockPos c) {
		BlockState s = world.getBlockState(c);
		if (!s.is(Blocks.FARMLAND))
			return false;
		int m = s.getValue(FarmBlock.MOISTURE);
		if (m >= FarmBlock.MAX_MOISTURE)
			return false;
		world.setBlock(c, s.setValue(FarmBlock.MOISTURE, m+1), 3);
		return true;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p);
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return Flow.INPUT;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (!resource.is(Fluids.WATER))
			return 0;
		int amt = tank.fill(resource, action);
		if (amt > 0 && action.execute())
			this.setChanged();
		return amt;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		return FluidStack.EMPTY;
	}

	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 1;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return tank.getFluid();
			}

			@Override
			public int getTankCapacity(int t) {
				return CAPACITY;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return stack.is(Fluids.WATER);
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return GroundHydratorBlockEntity.this.fill(side, resource, action);
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				return FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int maxDrain, FluidAction action) {
				return FluidStack.EMPTY;
			}
		};
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
	}
}
