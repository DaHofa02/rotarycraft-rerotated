/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original TileEntityFlooder (MachineRegistry.SPILLER "Liquid Spiller", PowerReceivers: 1024 W): pours the liquid from its 4000 mB tank
 * into the hollow below it as source blocks, 1000 mB each. Takes any liquid that has a block, through every side but the bottom.
 * Needs max(128, 512 * viscosity / 1000) W on top of the 1024 W minimum, and the operation time scales with the viscosity
 * (between a quarter and four times the base time).
 */
public class FlooderBlockEntity extends AreaFillerBlockEntity implements PipeConnector {

	public static final int CAPACITY = 4000;

	private final FluidTank tank = new FluidTank(CAPACITY);

	public FlooderBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.SPILLER.get(), pos, state, 1024);
	}

	@Override
	public String getName() {
		return "Liquid Spiller";
	}

	private static boolean canBePlacedInWorld(Fluid f) {
		return !f.defaultFluidState().createLegacyBlock().isAir();
	}

	private int getViscosity() {
		return tank.getFluid().getFluid().getFluidType().getViscosity(tank.getFluid());
	}

	public int getFluidLevel() {
		return tank.getFluidAmount();
	}

	public FluidStack getFluid() {
		return tank.getFluid();
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public boolean canConnectToPipe(PipeType m) {
		return m.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p) && side != Direction.DOWN;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side != Direction.DOWN ? Flow.INPUT : Flow.NONE;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (from == Direction.DOWN || resource.isEmpty() || !canBePlacedInWorld(resource.getFluid()))
			return 0;
		int added = tank.fill(resource, action);
		if (added > 0 && action.execute())
			this.setChanged();
		return added;
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
				return side != Direction.DOWN && canBePlacedInWorld(stack.getFluid());
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return FlooderBlockEntity.this.fill(side, resource, action);
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
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	protected boolean hasRemainingBlocks() {
		return tank.getFluidAmount() >= 1000;
	}

	@Override
	protected void onBlockPlaced() {
		tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
	}

	@Nullable
	@Override
	protected BlockState getNextPlacedBlock() {
		if (tank.isEmpty())
			return null;
		BlockState b = tank.getFluid().getFluid().defaultFluidState().createLegacyBlock();
		return b.isAir() ? null : b;
	}

	@Override
	public long getRequiredPower() {
		return Math.max(128, 512*this.getViscosity()/1000);
	}

	@Override
	public int getOperationTime() {
		int base = super.getOperationTime();
		return tank.isEmpty() ? base : Mth.clamp(base*this.getViscosity()/1000, base/4, base*4);
	}

	@Override
	protected boolean allowFluidOverwrite() {
		return false;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
	}
}
