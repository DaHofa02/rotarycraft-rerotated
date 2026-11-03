/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.powergen;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.api.interfaces.SodiumSolarUpgrades;
import reika.rotarycraft.api.power.PowerTransferHelper;
import reika.rotarycraft.api.power.ShaftPowerReceiver;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;

/**
 * Original: Reika.ReactorCraft.TileEntities.PowerGen.TileEntitySolarExchanger (TankedReactorPowerReceiver) - sits under a RotaryCraft
 * solar tower running on sodium and takes the sodium the tower uses up as warm sodium (1000 mB tank), while it gets at least 65536 W at
 * 2048 rad/s from below. The warm sodium is drained from the sides; it takes nothing in.
 */
public class SolarExchangerBlockEntity extends RotaryCraftBlockEntity implements ShaftPowerReceiver, PipeConnector, SodiumSolarUpgrades.SodiumSolarOutput {

	public static final int MINPOWER = 65536;
	public static final int MINSPEED = 2048;
	public static final int CAPACITY = 1000;

	private long power;
	private int omega;
	private int torque;
	private int iotick;

	private final FluidTank tank = new FluidTank(CAPACITY) {
		@Override
		protected void onContentsChanged() {
			SolarExchangerBlockEntity.this.setChanged();
		}
	};

	public SolarExchangerBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.SOLAR_EXCHANGER.get(), pos, state);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (!PowerTransferHelper.checkPowerFrom(this, Direction.DOWN))
			this.noInputMachine();
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (iotick > 0)
			iotick -= 8;
	}

	public boolean sufficientPower() {
		return power >= MINPOWER && omega >= MINSPEED && torque >= 1;
	}

	@Override
	public boolean isActive() {
		return this.sufficientPower();
	}

	@Override
	public int receiveSodium(int amt) {
		amt = Math.min(amt, tank.getSpace());
		if (amt > 0)
			tank.fill(new FluidStack(ReactorFluids.get("warm_sodium"), amt), IFluidHandler.FluidAction.EXECUTE);
		return amt;
	}

	public FluidTank getTank() {
		return tank;
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
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		return 0;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (from == null || from.getAxis().isVertical())
			return FluidStack.EMPTY;
		return tank.drain(maxDrain, action);
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side.getAxis().isHorizontal() ? Flow.OUTPUT : Flow.NONE;
	}

	/** Capability: drained from the sides only. */
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
				return false;
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return 0;
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				if (tank.isEmpty() || !tank.getFluid().getFluid().isSame(resource.getFluid()))
					return FluidStack.EMPTY;
				return SolarExchangerBlockEntity.this.drain(side, resource.getAmount(), action);
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				return SolarExchangerBlockEntity.this.drain(side, max, action);
			}
		};
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
		return dir == Direction.DOWN;
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
		return 1;
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
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		omega = NBT.getInt("speed");
		torque = NBT.getInt("trq");
		power = NBT.getLong("pwr");
	}
}
