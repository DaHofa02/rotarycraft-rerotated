/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.powergen;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.reactorcraft.reactor.ReactorBoilerBlockEntity;
import reika.reactorcraft.reactor.ReactorType;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.api.power.PowerTransferHelper;
import reika.rotarycraft.api.power.ShaftPowerReceiver;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.TemperatureMachine;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.world.AmbientTemperature;

/**
 * Original: Reika.ReactorCraft.TileEntities.PowerGen.TileEntityHeatExchanger with TankedReactorPowerReceiver - with shaft power from
 * below (at least 8192 W and 512 rad/s) it cools 100 mB of a hot coolant per tick into the cold one (2000 mB tanks: in from the top,
 * out at the sides) and heats itself by heat capacity * 100 (less the closer it is to the coolant's maximum); once a second it passes
 * a quarter of its temperature difference to reactor boilers beside it (recording the coolant's reactor type) and moves towards the
 * ambient temperature (water halves it, ice quarters it and melts, fire +200, lava +600 - a hot exchanger explodes by fire or lava;
 * above 100 C it melts snow and ice). Coolants: hot sodium (breeder, max 600 C), hot CO2 (HTGR, 800 C), hot LiFBe (thorium, 1000 C),
 * liquid oxygen (4x gas, cools, 500 C), warm sodium (solar, 400 C); liquid nitrogen exists only with other mods. Cooling fins cool it.
 */
public class HeatExchangerBlockEntity extends RotaryCraftBlockEntity implements ShaftPowerReceiver, TemperatureMachine, PipeConnector {

	public static final int CAPACITY = 2000;
	public static final int MINTEMP = -140;
	public static final int MAXTEMP = 1500;
	public static final int COOL_AMOUNT = 100;
	public static final int MINPOWER = 8192;
	public static final int MINSPEED = 512;

	/** Original Exchange (ReikaThermoHelper heat capacities). */
	public enum Exchange {
		SODIUM("hot_sodium", "sodium", 1, 1.23, 600, ReactorType.BREEDER),
		CO2("hot_co2", "co2", 1, 1.168, 800, ReactorType.HTGR),
		LIFBE("hot_lifbe", "lifbe", 1, 1.102, 1000, ReactorType.THORIUM),
		OXYGEN("liquid_oxygen", "oxygen", 4, -0.92-3.41/32, 500, ReactorType.NONE),
		SOLARSODIUM("warm_sodium", "sodium", 1, 1.23*0.375F, 400, ReactorType.SOLAR);

		public final String hotFluid;
		public final String coldFluid;
		public final int expansionRatio;
		public final double heatCapacity;
		public final int maxTemperature;
		public final ReactorType type;

		Exchange(String hot, String cold, int r, double c, int max, ReactorType t) {
			hotFluid = hot;
			coldFluid = cold;
			expansionRatio = r;
			heatCapacity = c;
			maxTemperature = max;
			type = t;
		}

		public Fluid getHotFluid() {
			return ReactorFluids.get(hotFluid);
		}

		public Fluid getColdFluid() {
			return ReactorFluids.get(coldFluid);
		}

		@Nullable
		public static Exchange of(Fluid f) {
			for (Exchange e : values())
				if (f.isSame(e.getHotFluid()))
					return e;
			return null;
		}
	}

	private final FluidTank tank = new FluidTank(CAPACITY, f -> Exchange.of(f.getFluid()) != null);
	private final FluidTank output = new FluidTank(CAPACITY);
	private Exchange currentRecipe;
	private int temperature;
	private int tempTimer;
	private int omega;
	private int torque;
	private long power;
	private int iotick;

	public HeatExchangerBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.HEAT_EXCHANGER.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Heat Exchanger";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (!PowerTransferHelper.checkPowerFrom(this, Direction.DOWN))
			this.noInputMachine();
		currentRecipe = tank.isEmpty() ? null : Exchange.of(tank.getFluid().getFluid());
		if (this.canCool())
			this.cool();
		tempTimer++;
		if (tempTimer >= 20 && !world.isClientSide) {
			tempTimer = 0;
			this.distributeHeat(world, pos);
			this.updateTemperature(world, pos);
		}
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (iotick > 0)
			iotick -= 8;
	}

	@Nullable
	public Exchange getCurrentRecipe() {
		return currentRecipe;
	}

	public boolean sufficientPower() {
		return power >= MINPOWER && omega >= MINSPEED && torque >= 1;
	}

	private boolean canCool() {
		if (currentRecipe == null || !this.sufficientPower())
			return false;
		return temperature < currentRecipe.maxTemperature && tank.getFluidAmount() >= COOL_AMOUNT && output.getSpace() >= COOL_AMOUNT*currentRecipe.expansionRatio
				&& (output.isEmpty() || output.getFluid().getFluid().isSame(currentRecipe.getColdFluid()));
	}

	private void cool() {
		tank.drain(COOL_AMOUNT, IFluidHandler.FluidAction.EXECUTE);
		output.fill(new FluidStack(currentRecipe.getColdFluid(), COOL_AMOUNT*currentRecipe.expansionRatio), IFluidHandler.FluidAction.EXECUTE);
		double eff = Math.min(1, Math.max(0.1, 1-(temperature-100)/(currentRecipe.maxTemperature-100D)));
		temperature += currentRecipe.heatCapacity*COOL_AMOUNT*eff;
		temperature = Math.max(MINTEMP, Math.min(MAXTEMP, temperature));
		this.setChanged();
	}

	private void distributeHeat(Level world, BlockPos pos) {
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			if (world.getBlockEntity(pos.relative(dir)) instanceof ReactorBoilerBlockEntity te) {
				int dT = temperature-te.getTemperature();
				if (dT > 0) {
					temperature -= dT/4;
					int add = dT/4;
					te.setTemperature(te.getTemperature()+add);
					te.setReactorType(currentRecipe != null ? currentRecipe.type : ReactorType.NONE, add);
				}
			}
		}
	}

	private void updateTemperature(Level world, BlockPos pos) {
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		if (this.adjacentFluid(world, pos, FluidTags.WATER) != null)
			Tamb /= 2;
		Direction ice = this.adjacentBlock(world, pos, Blocks.ICE);
		if (ice != null) {
			if (Tamb > 0)
				Tamb /= 4;
			world.setBlockAndUpdate(pos.relative(ice), Fluids.FLOWING_WATER.getFlowing(8, false).createLegacyBlock());
		}
		Direction fire = this.adjacentBlock(world, pos, Blocks.FIRE);
		if (fire != null) {
			Tamb += 200;
			if (temperature < 100)
				world.setBlockAndUpdate(pos.relative(fire), Blocks.AIR.defaultBlockState());
			else {
				world.removeBlock(pos, false);
				world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 6, Level.ExplosionInteraction.BLOCK);
				return;
			}
		}
		Direction lava = this.adjacentFluid(world, pos, FluidTags.LAVA);
		if (lava != null) {
			Tamb += 600;
			if (temperature < 100)
				world.setBlockAndUpdate(pos.relative(lava), Blocks.STONE.defaultBlockState());
			else {
				world.removeBlock(pos, false);
				world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 6, Level.ExplosionInteraction.BLOCK);
				return;
			}
		}
		if (temperature > Tamb)
			temperature--;
		if (temperature > Tamb*2)
			temperature--;
		if (temperature < Tamb)
			temperature++;
		if (temperature*2 < Tamb)
			temperature++;
		temperature = Math.max(MINTEMP, Math.min(MAXTEMP, temperature));
		if (temperature > 100) {
			Direction snow = this.adjacentBlock(world, pos, Blocks.SNOW);
			if (snow != null)
				world.setBlockAndUpdate(pos.relative(snow), Blocks.AIR.defaultBlockState());
			Direction ice2 = this.adjacentBlock(world, pos, Blocks.ICE);
			if (ice2 != null)
				world.setBlockAndUpdate(pos.relative(ice2), Fluids.FLOWING_WATER.getFlowing(8, false).createLegacyBlock());
		}
	}

	@Nullable
	private Direction adjacentBlock(Level world, BlockPos pos, net.minecraft.world.level.block.Block b) {
		for (Direction d : Direction.values())
			if (world.getBlockState(pos.relative(d)).is(b))
				return d;
		return null;
	}

	@Nullable
	private Direction adjacentFluid(Level world, BlockPos pos, net.minecraft.tags.TagKey<Fluid> tag) {
		for (Direction d : Direction.values())
			if (world.getFluidState(pos.relative(d)).is(tag))
				return d;
		return null;
	}

	public FluidTank getTank() {
		return tank;
	}

	public FluidTank getOutputTank() {
		return output;
	}

	@Override
	public void addTemperature(int temp) {
		temperature += temp;
	}

	@Override
	public int getTemperature() {
		return temperature;
	}

	@Override
	public void setTemperature(int temp) {
		temperature = temp;
	}

	@Override
	public int getMaxTemperature() {
		return MAXTEMP;
	}

	@Override
	public boolean canBeCooledWithFins() {
		return true;
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
		return 16;
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
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return side != Direction.DOWN && this.canConnectToPipe(p);
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		if (side == Direction.DOWN)
			return Flow.NONE;
		return side == Direction.UP ? Flow.INPUT : Flow.OUTPUT;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (from != Direction.UP)
			return 0;
		int n = tank.fill(resource, action);
		if (n > 0 && action.execute())
			this.setChanged();
		return n;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (from == null || from.getAxis() == Direction.Axis.Y)
			return FluidStack.EMPTY;
		FluidStack d = output.drain(maxDrain, action);
		if (!d.isEmpty() && action.execute())
			this.setChanged();
		return d;
	}

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
				return t == 0 && tank.isFluidValid(stack);
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return HeatExchangerBlockEntity.this.fill(side, resource, action);
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				if (resource.isEmpty() || !output.getFluid().getFluid().isSame(resource.getFluid()))
					return FluidStack.EMPTY;
				return HeatExchangerBlockEntity.this.drain(side, resource.getAmount(), action);
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				return HeatExchangerBlockEntity.this.drain(side, max, action);
			}
		};
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.put("output", output.writeToNBT(registries, new CompoundTag()));
		NBT.putInt("temp", temperature);
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
		temperature = NBT.getInt("temp");
		omega = NBT.getInt("speed");
		torque = NBT.getInt("trq");
		power = NBT.getLong("pwr");
	}
}
