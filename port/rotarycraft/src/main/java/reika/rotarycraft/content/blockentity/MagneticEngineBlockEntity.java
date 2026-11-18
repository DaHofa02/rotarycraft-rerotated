/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.rotarycraft.api.power.PowerGenerator;
import reika.rotarycraft.content.item.EngineUpgradeItem;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.content.power.PowerSourceList;
import reika.rotarycraft.api.power.ShaftMerger;
import reika.rotarycraft.content.power.SimpleProvider;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryFluids;

/**
 * The magnetostatic engine, written for the port from the original's behaviour: turns Forge Energy (FE, which replaces RF) taken in at its
 * back into shaft power at its front. Six tiers (raised with the magnet upgrades): torque 8*4^tier Nm, speeds 2^0 to 2^(8+tier) rad/s chosen
 * in its GUI. It runs up to the chosen speed (4*log2(speed+1) rad/s per tick, using half again as much energy while it speeds up) and uses
 * power/520 FE per tick divided by its efficiency (90 % - 8 % per tier, with the efficiency upgrade 100 % - 4 % * tier^1.4). It heats by
 * 0.14*sqrt(power) + log2(tier+1) a second, only 0.00275*sqrt(power) with at least 50 mB of liquid nitrogen (which boils off); at 500 C it
 * explodes. A redstone mode can make it run only with or only without a signal.
 * Changes: FE instead of RF, any amount is accepted (the original refused transfers below a per-tier minimum).
 */
public class MagneticEngineBlockEntity extends IOMachineBlockEntity implements SimpleProvider, PowerGenerator, UpgradeableMachine, PipeConnector {

	public static final int TIERS = 6;
	public static final int MAXTEMP = 500;
	public static final int WATTS_PER_FE = 520;
	private static final int[] STORAGE_STEP = {1, 3, 24, 187, 1491, 11925};

	public enum RedstoneMode {
		IGNORE, ON_SIGNAL, WITHOUT_SIGNAL;

		public RedstoneMode next() {
			return values()[(this.ordinal()+1)%values().length];
		}
	}

	private int tier;
	private boolean efficient;
	/** Chosen speed as a power of two; -1 = off. */
	private int speedStep = -1;
	private RedstoneMode redstone = RedstoneMode.IGNORE;
	private int stored;
	private int temperature = -1000;
	private final FluidTank tank = new FluidTank(24000, fs -> fs.is(RotaryFluids.LIQUID_NITROGEN.get()));
	private final IEnergyStorage energy = new IEnergyStorage() {
		@Override
		public int receiveEnergy(int max, boolean simulate) {
			int amt = Math.max(0, Math.min(max, getMaxStorage()-stored));
			if (!simulate && amt > 0) {
				stored += amt;
				setChanged();
			}
			return amt;
		}

		@Override
		public int extractEnergy(int max, boolean simulate) {
			return 0;
		}

		@Override
		public int getEnergyStored() {
			return stored;
		}

		@Override
		public int getMaxEnergyStored() {
			return getMaxStorage();
		}

		@Override
		public boolean canExtract() {
			return false;
		}

		@Override
		public boolean canReceive() {
			return true;
		}
	};

	public MagneticEngineBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.MAGNETIC_ENGINE.get(), pos, state);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public static int torqueOf(int tier) {
		return 8*ReikaMathLibrary.intpow2(4, tier);
	}

	public static int maxStepOf(int tier) {
		return 8+tier;
	}

	public static double efficiencyOf(int tier, boolean efficient) {
		return efficient ? 1-Math.pow(tier, 1.4)*0.04 : 0.9-tier*0.08;
	}

	public int getTier() {
		return tier;
	}

	public void setTier(int t) {
		tier = Math.max(0, Math.min(TIERS-1, t));
		speedStep = Math.min(speedStep, maxStepOf(tier));
		this.syncAllData();
	}

	public boolean isEfficient() {
		return efficient;
	}

	public int getSpeedStep() {
		return speedStep;
	}

	public void setSpeedStep(int s) {
		speedStep = Math.max(-1, Math.min(maxStepOf(tier), s));
		this.syncAllData();
	}

	public int getTargetSpeed() {
		return speedStep < 0 ? 0 : 1 << speedStep;
	}

	public RedstoneMode getRedstoneMode() {
		return redstone;
	}

	public void stepRedstoneMode() {
		redstone = redstone.next();
		this.syncAllData();
	}

	public int getStoredEnergy() {
		return stored;
	}

	public int getMaxStorage() {
		return 1+STORAGE_STEP[tier]*200;
	}

	public int getTemperature() {
		return temperature;
	}

	public int getCoolant() {
		return tank.getFluidAmount();
	}

	public void addCoolant(int amt) {
		tank.fill(new FluidStack(RotaryFluids.LIQUID_NITROGEN.get(), amt), IFluidHandler.FluidAction.EXECUTE);
	}

	@Nullable
	public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
		return side == null || write == null || side == write.getOpposite() ? energy : null;
	}

	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return tank;
	}

	/** FE per tick at the current speed, after efficiency. */
	public int getConsumption() {
		long ideal = (long)this.getTargetSpeed()*torqueOf(tier)/WATTS_PER_FE;
		return (int)Math.ceil(ideal/efficiencyOf(tier, efficient));
	}

	public boolean isEnabled(Level world) {
		return switch (redstone) {
			case IGNORE -> true;
			case ON_SIGNAL -> world.hasNeighborSignal(worldPosition);
			case WITHOUT_SIGNAL -> !world.hasNeighborSignal(worldPosition);
		};
	}

	public void getIOSides(int meta) {
		write = switch (meta) {
			case 0 -> Direction.WEST;
			case 1 -> Direction.EAST;
			case 2 -> Direction.NORTH;
			default -> Direction.SOUTH;
		};
		read = write.getOpposite();
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSides(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		if (temperature == -1000)
			temperature = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		int target = this.isEnabled(world) ? this.getTargetSpeed() : 0;
		int use = this.getConsumption();
		boolean speedingUp = omega < target;
		if (target > 0 && omega <= target && stored > use) {
			omega = Math.min(target, omega+(int)(4*ReikaMathLibrary.logbase(target+1, 2)));
		}
		else if (omega > 0) {
			omega -= omega/256+1;
			if (omega < 0)
				omega = 0;
		}
		torque = omega > 0 ? torqueOf(tier) : 0;
		power = (long)omega*torque;
		if (power > 0)
			stored = Math.max(0, stored-(int)(use*(speedingUp ? 1.5F : 1F)));
		if (this.getTicksExisted()%20 == 0)
			this.updateTemperature(world, pos);
		this.basicPowerReceiver();
	}

	private void updateTemperature(Level world, BlockPos pos) {
		int amb = tank.isEmpty() ? AmbientTemperature.getAmbientTemperatureAt(world, pos) : 25;
		if (power > 0) {
			double d = tank.getFluidAmount() >= 50 ? 0.00275 : 0.14;
			temperature += (int)(d*Math.sqrt(power)+ReikaMathLibrary.logbase(tier+1, 2));
			if (temperature > amb && !tank.isEmpty())
				tank.drain(Math.max(2, 50*temperature/MAXTEMP), IFluidHandler.FluidAction.EXECUTE);
		}
		temperature -= (temperature-amb)/16;
		if (temperature > amb && temperature-amb <= 16)
			temperature--;
		if (temperature < amb)
			temperature = amb;
		if (temperature > MAXTEMP && this.isOverheatAllowed()) {
			world.removeBlock(pos, false);
			world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 3F, true, Level.ExplosionInteraction.BLOCK);
		}
	}

	/** Tests switch the explosion off and check the temperature. */
	public static boolean allowOverheat = true;

	private boolean isOverheatAllowed() {
		return allowOverheat;
	}

	@Override
	public boolean canUpgradeWith(net.minecraft.world.item.ItemStack is) {
		if (EngineUpgradeItem.is(is, EngineUpgradeItem.Upgrades.EFFICIENCY))
			return !efficient;
		return is.getItem() instanceof EngineUpgradeItem u && u.getUpgrade().magnetTier() == tier+1;
	}

	@Override
	public void upgrade(net.minecraft.world.item.ItemStack is) {
		if (EngineUpgradeItem.is(is, EngineUpgradeItem.Upgrades.EFFICIENCY))
			efficient = true;
		else
			tier++;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public boolean canProvidePower() {
		return true;
	}

	@Override
	public PowerSourceList getPowerSources(reika.rotarycraft.content.power.PowerSourceTracker io, ShaftMerger caller) {
		PowerSourceList psl = new PowerSourceList();
		if (power > 0)
			psl.addSource(this);
		return psl;
	}

	@Override
	public void getAllOutputs(Collection<BlockEntity> c, Direction dir) {
		c.add(this.getAdjacentTileEntity(write));
	}

	@Override
	public long getMaxPower() {
		return (long)torqueOf(tier)*(1L << maxStepOf(tier));
	}

	@Override
	public long getCurrentPower() {
		return power;
	}

	@Override
	public BlockPos getEmittingPos() {
		return worldPosition.relative(write == null ? Direction.WEST : write);
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
		return this.canConnectToPipe(p) && side != write;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return Flow.INPUT;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		return tank.fill(resource, action);
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		return FluidStack.EMPTY;
	}

	public static boolean isCoolant(Fluid f) {
		return f.isSame(RotaryFluids.LIQUID_NITROGEN.get());
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("tier", tier);
		NBT.putBoolean("eff", efficient);
		NBT.putInt("step", speedStep);
		NBT.putInt("rs", redstone.ordinal());
		NBT.putInt("energy", stored);
		NBT.putInt("temp", temperature);
		NBT.putInt("coolant", tank.getFluidAmount());
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		tier = Math.max(0, Math.min(TIERS-1, NBT.getInt("tier")));
		efficient = NBT.getBoolean("eff");
		speedStep = NBT.contains("step") ? Math.max(-1, Math.min(maxStepOf(tier), NBT.getInt("step"))) : -1;
		redstone = RedstoneMode.values()[Math.floorMod(NBT.getInt("rs"), RedstoneMode.values().length)];
		stored = NBT.getInt("energy");
		temperature = NBT.contains("temp") ? NBT.getInt("temp") : -1000;
		int c = NBT.getInt("coolant");
		tank.setFluid(c > 0 ? new FluidStack(RotaryFluids.LIQUID_NITROGEN.get(), c) : FluidStack.EMPTY);
	}
}
