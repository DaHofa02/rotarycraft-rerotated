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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.reactorcraft.reactor.ReactorBlockEntity;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.api.power.PowerTransferHelper;
import reika.rotarycraft.api.power.ShaftPowerReceiver;
import reika.rotarycraft.content.piping.PipeBlockEntity;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.content.piping.PipingBlockEntity;

/**
 * Original: Reika.ReactorCraft.TileEntities.PowerGen.TileEntityReactorPump (ReactorTiles.PUMP, "Pressurizer", a TankedReactorPowerReceiver).
 * With shaft power from below (at least 16384 W, 1024 Nm and 1 rad/s) it turns the low pressure water or ammonia that turbines leave
 * back into water or ammonia: each tick as much as the 12000 mB output tank can take (the input tank, also 12000 mB, empties at once),
 * as long as the output tank is empty or holds the matching fluid. Input only from the top (pipes and other fluid handlers, only low
 * pressure water/ammonia). Every tick, with or without power, the output goes to the four horizontal sides: into a standard or bedrock
 * pipe there a quarter of the level difference (the original pushed into any TileEntityPipe, connected or not), into any other fluid
 * handler as much as it takes. Pipes connect on every side but the bottom; the rotor turns 15 degrees a tick while powered.
 * Original quirk kept: other machines may drain the output from the sides only while the INPUT tank holds fluid (and, when they ask
 * for a fluid, only for the input tank's fluid - so a fluid-specific drain never yields anything) - canDrain looked at the wrong tank.
 */
public class ReactorPumpBlockEntity extends ReactorBlockEntity implements ShaftPowerReceiver, PipeConnector {

	public static final long MINPOWER = 16384;
	public static final int MINTORQUE = 1024;
	public static final int MINSPEED = 1;
	public static final int CAPACITY = 12000;

	private final FluidTank tank = new FluidTank(CAPACITY, s -> isValidFluid(s.getFluid()));
	private final FluidTank output = new FluidTank(CAPACITY);
	private int omega;
	private int torque;
	private long power;
	private int iotick;

	public ReactorPumpBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.PRESSURIZER.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Pressurizer";
	}

	/** Original isValidFluid: rc lowpwater and rc lowpammonia. */
	public static boolean isValidFluid(Fluid f) {
		return f.isSame(ReactorFluids.get("low_pressure_water")) || f.isSame(ReactorFluids.get("low_pressure_ammonia"));
	}

	/** What the input fluid becomes (original convertFluids), or null. */
	@Nullable
	public static Fluid getOutputFluid(Fluid in) {
		if (in.isSame(ReactorFluids.get("low_pressure_water")))
			return Fluids.WATER;
		if (in.isSame(ReactorFluids.get("low_pressure_ammonia")))
			return ReactorFluids.get("ammonia");
		return null;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (!PowerTransferHelper.checkPowerFrom(this, Direction.DOWN))
			this.noInputMachine();
		if (world.isClientSide)
			return;
		if (this.canConvert())
			this.convertFluids();
		if (!output.isEmpty())
			this.dumpFluids(world, pos);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (iotick > 0)
			iotick -= 8;
		if (power > 0)
			phi += 15;
	}

	public boolean sufficientPower() {
		return power >= MINPOWER && omega >= MINSPEED && torque >= MINTORQUE;
	}

	private boolean canConvert() {
		if (!this.sufficientPower() || tank.isEmpty())
			return false;
		if (output.isEmpty())
			return true;
		if (output.getSpace() == 0)
			return false;
		Fluid out = getOutputFluid(tank.getFluid().getFluid());
		return out != null && output.getFluid().getFluid().isSame(out);
	}

	private void convertFluids() {
		int amt = Math.min(tank.getFluidAmount(), output.getSpace());
		if (amt > 0) {
			Fluid out = getOutputFluid(tank.getFluid().getFluid());
			if (out != null)
				output.fill(new FluidStack(out, amt), IFluidHandler.FluidAction.EXECUTE);
			tank.drain(amt, IFluidHandler.FluidAction.EXECUTE);
			this.setChanged();
		}
	}

	/** Original dumpFluids: north, south, west, east. */
	private void dumpFluids(Level world, BlockPos pos) {
		for (int i = 2; i < 6 && !output.isEmpty(); i++) {
			Direction dir = Direction.values()[i];
			BlockPos dp = pos.relative(dir);
			BlockEntity te = world.getBlockEntity(dp);
			Fluid f = output.getFluid().getFluid();
			if (te instanceof PipeBlockEntity p) {
				if (p.canIntakeFluid(f)) {
					int dL = output.getFluidAmount()-p.getFluidLevel();
					if (dL/4 > 0 && p.addFluid(f, dL/4)) {
						output.drain(dL/4, IFluidHandler.FluidAction.EXECUTE);
						this.setChanged();
					}
				}
			}
			else if (te != null && !(te instanceof PipingBlockEntity)) { //the original pipes were no IFluidHandler
				IFluidHandler fl = world.getCapability(Capabilities.FluidHandler.BLOCK, dp, dir.getOpposite());
				if (fl != null) {
					int amt = fl.fill(output.getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
					if (amt > 0) {
						output.drain(amt, IFluidHandler.FluidAction.EXECUTE);
						this.setChanged();
					}
				}
			}
		}
	}

	public FluidTank getTank() {
		return tank;
	}

	public FluidTank getOutputTank() {
		return output;
	}

	/** Original canReceiveFrom: only from the top. */
	private static boolean canReceiveFrom(@Nullable Direction from) {
		return from == Direction.UP;
	}

	/** Original canDrain: horizontal sides (or no side), and only while the input tank holds the asked fluid (any, when none is asked). */
	private boolean canDrain(@Nullable Direction from, @Nullable Fluid f) {
		return (from == null || from.getAxis() != Direction.Axis.Y) && !tank.isEmpty() && (f == null || tank.getFluid().getFluid().isSame(f));
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return side != Direction.DOWN && this.canConnectToPipe(p);
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return canReceiveFrom(side) ? Flow.INPUT : Flow.NONE;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (!canReceiveFrom(from) || resource.isEmpty() || !isValidFluid(resource.getFluid()))
			return 0;
		int n = tank.fill(resource, action);
		if (n > 0 && action.execute())
			this.setChanged();
		return n;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (!this.canDrain(from, null))
			return FluidStack.EMPTY;
		FluidStack d = output.drain(maxDrain, action);
		if (!d.isEmpty() && action.execute())
			this.setChanged();
		return d;
	}

	/** Fluid handler for every side (the original IFluidHandler; fill only from the top, drain only from the sides). */
	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 2;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return (t == 0 ? tank : output).getFluid();
			}

			@Override
			public int getTankCapacity(int t) {
				return CAPACITY;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return t == 0 && isValidFluid(stack.getFluid());
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return ReactorPumpBlockEntity.this.fill(side, resource, action);
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				//original: canDrain checked the asked fluid against the input tank; the drained fluid must also be what was asked for
				if (resource.isEmpty() || !ReactorPumpBlockEntity.this.canDrain(side, resource.getFluid()) || !output.getFluid().getFluid().isSame(resource.getFluid()))
					return FluidStack.EMPTY;
				return ReactorPumpBlockEntity.this.drain(side, resource.getAmount(), action);
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				return ReactorPumpBlockEntity.this.drain(side, max, action);
			}
		};
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
	public boolean canReadFrom(Direction dir) {
		return dir == Direction.DOWN;
	}

	@Override
	public boolean isReceiving() {
		return true;
	}

	@Override
	public void noInputMachine() {
		torque = omega = 0;
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
		NBT.put("output", output.writeToNBT(registries, new CompoundTag()));
		NBT.putInt("speed", omega);
		NBT.putInt("trq", torque);
		NBT.putLong("pwr", power);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		output.setFluid(FluidStack.EMPTY);
		output.readFromNBT(registries, NBT.getCompound("output"));
		omega = NBT.getInt("speed");
		torque = NBT.getInt("trq");
		power = NBT.getLong("pwr");
	}
}
