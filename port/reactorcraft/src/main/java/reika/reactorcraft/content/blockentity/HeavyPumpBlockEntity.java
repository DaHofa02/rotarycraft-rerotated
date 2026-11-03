/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.api.power.ShaftPowerReceiver;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.math.ReikaMathLibrary;

/**
 * Original: Reika.ReactorCraft.TileEntities.TileEntityHeavyPump (Fluid Extractor). Powered from above or below (min. 512 Nm, 65536 W),
 * every max(1, 20 - 2*log2(omega)) ticks it extracts from the source blocks on at least three of its four sides (all the same fluid):
 * <ul><li>water: 200 mB heavy water, below y 45 in an ocean biome with water on all four diagonals for 16 blocks up and above it;</li>
 * <li>lava: 10-40 mB (Nether 10-80) molten lithium when it sits directly on the lava surface (Nether: y 31; overworld: the 1.7.10 lava lake
 * level y 10 became the 1.21 cave lava level, y -54).</li></ul>
 * 8000 mB tank, emptied by pipes and fluid handlers on its four sides.
 */
public class HeavyPumpBlockEntity extends RotaryCraftBlockEntity implements ShaftPowerReceiver, PipeConnector {

	public static final int MINPOWER = 65536;
	public static final int MINTORQUE = 512;
	public static final int HEAVY_WATER_MAXY = 45;
	public static final int HEAVY_WATER_MINDEPTH = 16;

	private int torque;
	private int omega;
	private long power;
	private int iotick;
	private int timer;

	private final FluidTank tank = new FluidTank(8000);

	public HeavyPumpBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.HEAVY_PUMP.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Fluid Extractor";
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (power >= MINPOWER && torque >= MINTORQUE)
			phi += 10F;
		iotick -= 8;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (world.isClientSide)
			return;
		if (!reika.rotarycraft.api.power.PowerTransferHelper.checkPowerFrom(this, Direction.DOWN) && !reika.rotarycraft.api.power.PowerTransferHelper.checkPowerFrom(this, Direction.UP))
			this.noInputMachine();
		if (power >= MINPOWER && torque >= MINTORQUE) {
			int cap = Math.max(1, 20-2*(int)ReikaMathLibrary.logbase(omega, 2));
			Fluid f = this.getExtraction(world, pos);
			if (f != null) {
				timer++;
				if (timer >= cap) {
					timer = 0;
					this.harvest(world, pos, f);
				}
			}
			else {
				timer = 0;
			}
		}
	}

	/** Original getExtraction: water or lava source blocks on at least three sides, none of another fluid. */
	@Nullable
	private Fluid getExtraction(Level world, BlockPos pos) {
		Fluid f = null;
		int c = 0;
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			FluidState fs = world.getFluidState(pos.relative(dir));
			if (!fs.isEmpty() && fs.isSource()) {
				Fluid f2 = fs.getType();
				if (f == null || f2.isSame(f)) {
					c++;
					f = f2;
				}
				else {
					return null;
				}
			}
		}
		if (f == null || c < 3)
			return null;
		if (f.isSame(Fluids.WATER) || f.isSame(Fluids.LAVA))
			return f;
		return null;
	}

	private void harvest(Level world, BlockPos pos, Fluid f) {
		if (f.isSame(Fluids.WATER)) {
			if (pos.getY() < HEAVY_WATER_MAXY && world.getBiome(pos).is(BiomeTags.IS_OCEAN) && isOceanFloor(world, pos))
				tank.fill(new FluidStack(ReactorFluids.get("heavy_water"), 200), IFluidHandler.FluidAction.EXECUTE);
		}
		else {
			int surface = world.dimension() == Level.NETHER ? 31 : world.dimension() == Level.OVERWORLD ? -54 : Integer.MIN_VALUE;
			if (pos.getY() == surface && world.getFluidState(pos.below()).is(FluidTags.LAVA) && !world.getFluidState(pos.above()).is(FluidTags.LAVA)) {
				int amt = world.dimension() == Level.NETHER ? 10+rand.nextInt(21)+rand.nextInt(51) : 10+rand.nextInt(31);
				tank.fill(new FluidStack(ReactorFluids.get("lithium"), amt), IFluidHandler.FluidAction.EXECUTE);
			}
		}
		this.setChanged();
	}

	/** Original isOceanFloor: still water on the four diagonals for 16 blocks and above the machine. */
	private static boolean isOceanFloor(Level world, BlockPos pos) {
		for (int i = 0; i < HEAVY_WATER_MINDEPTH; i++) {
			for (int a = -1; a <= 1; a += 2)
				for (int b = -1; b <= 1; b += 2)
					if (!isStillWater(world, pos.offset(a, i, b)))
						return false;
			if (i >= 1 && !isStillWater(world, pos.above(i)))
				return false;
		}
		return true;
	}

	private static boolean isStillWater(Level world, BlockPos p) {
		BlockState s = world.getBlockState(p);
		return s.is(Blocks.WATER) && s.getFluidState().isSource();
	}

	public FluidTank getTank() {
		return tank;
	}

	public int getTankLevel() {
		return tank.getFluidAmount();
	}

	/** Fluid handler for the four horizontal sides (drain only). */
	@Nullable
	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		if (side != null && side.getAxis().isVertical())
			return null;
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
				return tank.getCapacity();
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return false;
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return 0;
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				FluidStack d = tank.drain(resource, action);
				if (action.execute() && !d.isEmpty())
					HeavyPumpBlockEntity.this.setChanged();
				return d;
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				FluidStack d = tank.drain(max, action);
				if (action.execute() && !d.isEmpty())
					HeavyPumpBlockEntity.this.setChanged();
				return d;
			}
		};
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p) && side.getAxis().isHorizontal();
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		return 0;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (from != null && from.getAxis().isVertical())
			return FluidStack.EMPTY;
		FluidStack d = tank.drain(maxDrain, action);
		if (action.execute() && !d.isEmpty())
			this.setChanged();
		return d;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side.getAxis().isHorizontal() ? Flow.OUTPUT : Flow.NONE;
	}

	@Override
	public void setOmega(int omega) {
		this.omega = omega;
	}

	@Override
	public void setTorque(int torque) {
		this.torque = torque;
	}

	@Override
	public void setPower(long power) {
		this.power = power;
	}

	@Override
	public int getOmega() {
		return omega;
	}

	@Override
	public int getTorque() {
		return torque;
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public boolean canReadFrom(Direction dir) {
		return dir.getAxis().isVertical();
	}

	@Override
	public boolean isReceiving() {
		return true;
	}

	@Override
	public void noInputMachine() {
		omega = torque = 0;
		power = 0;
	}

	@Override
	public int getMinTorque(int available) {
		return MINTORQUE;
	}

	@Override
	public int getIORenderAlpha() {
		return iotick;
	}

	@Override
	public void setIORenderAlpha(int io) {
		iotick = io;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.putInt("speed", omega);
		NBT.putInt("trq", torque);
		NBT.putLong("pwr", power);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		omega = NBT.getInt("speed");
		torque = NBT.getInt("trq");
		power = NBT.getLong("pwr");
	}
}
