/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity.engine;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.api.power.PowerGenerator;
import reika.rotarycraft.api.power.ShaftMerger;
import reika.rotarycraft.content.blockentity.EngineControllerBlockEntity;
import reika.rotarycraft.content.blockentity.IOMachineBlockEntity;
import reika.rotarycraft.content.blockentity.MachineSounds;
import reika.rotarycraft.content.blockentity.TemperatureMachine;
import reika.rotarycraft.content.power.PowerSourceList;
import reika.rotarycraft.content.power.PowerSourceTracker;
import reika.rotarycraft.content.power.SimpleProvider;
import reika.rotarycraft.lib.data.ParallelTicker;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.EngineType;
import reika.rotarycraft.registry.RotaryFluids;

/**
 * Original: Reika.RotaryCraft.Base.TileEntity.TileEntityEngine, including the Engine Control Unit below (above when flipped).
 * The integrated gearbox upgrade (IntegratedGearboxItem) multiplies torque and divides speed or the other way round. Not ported yet:
 * the oxygen boost (air fluids); without it the engine behaves like an original engine without oxygen.
 */
public abstract class EngineBlockEntity extends IOMachineBlockEntity implements TemperatureMachine, SimpleProvider, PowerGenerator, net.minecraft.world.Clearable, reika.rotarycraft.content.piping.PipeConnector {

	/** Vanilla Clearable: commands such as /setblock and /fill empty the inventory before replacing the block, so nothing drops. */
	@Override
	public void clearContent() {
		for (int i = 0; i < inv.getSlots(); i++)
			inv.setStackInSlot(i, ItemStack.EMPTY);
	}


	/** Water capacity */
	public static final int CAPACITY = 60*1000;

	/** Fuel capacity */
	public static final int FUELCAP = 240*1000;
	public static final int LUBECAP = 24*1000;

	protected final FluidTank lubricant = new FluidTank(LUBECAP, fs -> fs.is(RotaryFluids.LUBRICANT.get()));
	protected final FluidTank water = new FluidTank(CAPACITY, fs -> fs.is(Fluids.WATER));
	protected final FluidTank fuel = new FluidTank(FUELCAP);

	public int temperature;

	/** For timing control */
	public int soundtick = 2000;

	protected final EngineType type;

	private boolean isOn;

	protected long lastpower = 0;

	private int integratedGear = 0;

	protected final ParallelTicker timer = new ParallelTicker().addTicker("fuel").addTicker("sound").addTicker("temperature", 20);

	protected final ItemStackHandler inv = new ItemStackHandler(2) {
		@Override
		public int getSlotLimit(int slot) {
			return type.allowInventoryStacking() ? 64 : 1;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return EngineBlockEntity.this.isItemValidForSlot(slot, stack);
		}

		@Override
		protected void onContentsChanged(int slot) {
			EngineBlockEntity.this.setChanged();
		}
	};

	protected EngineBlockEntity(BlockEntityType<?> beType, BlockPos pos, BlockState state, EngineType type) {
		super(beType, pos, state);
		this.type = type;
	}

	@Override
	public String getName() {
		return "Engine";
	}

	public int getMaxTemperature() {
		return 1000;
	}

	public final EngineType getEngineType() {
		return type;
	}

	public boolean hasTemperature() {
		return type.isCooled();
	}

	public final int getWaterScaled(int par1) {
		return water.getFluidAmount()*par1/CAPACITY;
	}

	public final int getTempScaled(int par1) {
		return temperature*par1/this.getMaxTemperature();
	}

	public final int getFuelScaled(int par1) {
		return this.getFuelLevel()*par1/FUELCAP;
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	protected abstract void consumeFuel(float scale);

	protected int getConsumedFuel() {
		return 10;
	}

	protected abstract void internalizeFuel();

	protected abstract boolean getRequirements(Level world, BlockPos pos, int meta);

	protected final boolean hasAir(Level world, BlockPos pos) {
		return !this.isDrowned(world, pos);
	}

	/** True when every neighbour is fluid or solid, with at least one fluid (original isDrowned). */
	public final boolean isDrowned(Level world, BlockPos pos) {
		boolean flag = false;
		for (Direction dir : Direction.values()) {
			BlockPos p = pos.relative(dir);
			BlockState b = world.getBlockState(p);
			boolean fluid = !b.getFluidState().isEmpty();
			flag = flag || fluid;
			if (b.isAir())
				return false;
			if (!fluid && b.canBeReplaced())
				return false;
		}
		return flag;
	}

	public void updateTemperature(Level world, BlockPos pos) {
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		temperature = Math.max(temperature, Tamb);
		if (temperature > Tamb && !isOn) {
			this.offlineCooldown(world, pos, Tamb);
		}
	}

	protected void offlineCooldown(Level world, BlockPos pos, int Tamb) {
		temperature -= Math.max(1, (temperature-Tamb)/256);
	}

	public final boolean isOn() {
		return isOn;
	}

	public void overheat(Level world, BlockPos pos) {

	}

	private void setPowerData(Level world, BlockPos pos, int meta) {
		int speed = getIntegratedGearSpeed(this.getMaxSpeed(world, pos, meta), integratedGear);
		this.updateSpeed(speed, speed >= omega && (omega > 0 || this.canStart()));
		torque = getIntegratedGearTorque(this.getGenTorque(world, pos, meta), integratedGear);
	}

	static int getIntegratedGearTorque(int torque, int gear) {
		if (gear != 0) {
			return gear > 0 ? gear*torque : -torque/gear;
		}
		return torque;
	}

	static int getIntegratedGearSpeed(int speed, int gear) {
		if (gear != 0) {
			return gear < 0 ? -gear*speed : speed/gear;
		}
		return speed;
	}

	protected boolean canStart() {
		return true;
	}

	protected int getMaxSpeed(Level world, BlockPos pos, int meta) {
		return type.getSpeed();
	}

	protected int getGenTorque(Level world, BlockPos pos, int meta) {
		return type.getTorque();
	}

	protected abstract void affectSurroundings(Level world, BlockPos pos, int meta);

	protected final int getSoundLength() {
		return this.getSoundLength(1);
	}

	protected int getSoundLength(float factor) {
		if (factor == 2.5F && type.carNoise())
			factor = 1.81F;
		if (factor == 2.5F && type.turbineNoise()) {
			factor = 2F;
		}
		if (type.jetNoise()) {
			factor += 0.0125F;
		}
		return (int)(type.getSoundLength()*factor);
	}

	private void initialize(Level world, BlockPos pos, int meta) {
		timer.setCap("sound", this.getSoundLength());

		if (timer.checkCap("temperature")) {
			this.updateTemperature(world, pos);
		}

		boolean on = !type.isAirBreathing() || this.hasAir(world, pos);

		if (on && this.getRequirements(world, pos, meta)) {
			isOn = true;
			this.setPowerData(world, pos, meta);
		}
		else {
			isOn = false;
			this.updateSpeed(0, false);
			if (omega == 0)
				torque = 0;
			if (soundtick == 0 && omega == 0)
				soundtick = 2000;
		}
	}

	private void updateSpeed(int maxspeed, boolean revup) {
		if (this.hasECU() && this.canBeThrottled()) {
			EngineControllerBlockEntity te = this.getECU();
			if (te != null) {
				maxspeed *= te.getSpeedMultiplier();
			}
			if (omega > maxspeed)
				revup = false;
		}
		if (revup) {
			if (omega < maxspeed) {
				omega += (int)(4*ReikaMathLibrary.logbase(maxspeed+1, 2));
				timer.setCap("fuel", Math.max(type.getFuelUnitDuration()/4, 1)); //4x fuel burn while spinning up
				if (omega > maxspeed)
					omega = maxspeed;
			}
		}
		else {
			if (omega > 0) {
				omega -= omega/256+1;
			}
		}
	}

	private boolean hasECU() {
		return this.getECU() != null;
	}

	@Nullable
	private EngineControllerBlockEntity getECU() {
		return this.getAdjacentTileEntity(isFlipped ? Direction.UP : Direction.DOWN) instanceof EngineControllerBlockEntity te ? te : null;
	}

	protected abstract void playSounds(Level world, BlockPos pos, float pitchMultiplier, float vol);

	/** Original isMuffled: wool (or an ECU) above and below, or wool on every side except the shaft axis. */
	protected final boolean isMuffled(Level world, BlockPos pos) {
		boolean above = MachineSounds.isMufflingBlock(world, pos.above()) || this.getAdjacentTileEntity(Direction.UP) instanceof EngineControllerBlockEntity;
		boolean below = MachineSounds.isMufflingBlock(world, pos.below()) || this.getAdjacentTileEntity(Direction.DOWN) instanceof EngineControllerBlockEntity;
		if (above && below)
			return true;
		for (Direction dir : Direction.values()) {
			if (dir != Direction.DOWN) {
				if ((dir != write.getOpposite() && dir != write) || dir == Direction.UP) {
					if (!MachineSounds.isMufflingBlock(world, pos.relative(dir)))
						return false;
				}
			}
		}
		return true;
	}

	@Override
	protected final void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		int meta = this.getBlockMetadata();
		this.getIOSides(meta);

		timer.updateTicker("temperature");
		if (this.isShutdown()) {
			omega = torque = 0;
			power = 0;
		}
		else {
			if (!world.isClientSide) {
				timer.setCap("fuel", type.getFuelUnitDuration());
				this.initialize(world, pos, meta);
			}
			power = (long)torque*(long)omega;
			if (power > 0)
				this.affectSurroundings(world, pos, meta);
		}

		if (world.isClientSide)
			return;

		float pitch = 1F;
		float soundfactor = 1F;
		if (type.isECUControllable() && this.hasECU()) {
			EngineControllerBlockEntity te = this.getECU();
			if (te.canProducePower()) {
				if (this.canBeThrottled()) {
					int fueltime = type.getFuelUnitDuration();
					if (omega < type.getSpeed()*te.getSpeedMultiplier()) {
						fueltime = Math.max(type.getFuelUnitDuration()/4, 1);
					}
					timer.setCap("fuel", fueltime);
					int fuelcap = timer.getCapOf("fuel");
					fuelcap = fuelcap*te.getFuelMultiplier(type.type);
					timer.setCap("fuel", fuelcap);
					pitch = te.getSoundStretch();
					soundfactor = 1F/te.getSoundStretch();
					int soundcap = timer.getCapOf("sound");
					soundcap = (int)(soundcap*soundfactor);
					timer.setCap("sound", soundcap);
					int tempcap = timer.getCapOf("temperature");
					tempcap *= soundfactor;
					timer.setCap("temperature", tempcap);
				}
			}
			else {
				this.resetPower();
				soundtick = 0;
			}
		}

		this.basicPowerReceiver();

		this.internalizeFuel();
		if (power > 0) {
			timer.updateTicker("fuel");
			if (type.burnsFuel() && timer.checkCap("fuel") && this.canConsumeFuel()) {
				this.consumeFuel(1);
			}
		}

		if (power > 0) {
			this.playSounds(world, pos, pitch, 1);
		}
		else if (soundtick < this.getSoundLength(soundfactor))
			soundtick = 2000;

		lastpower = power;
	}

	protected void resetPower() {
		if (omega == 0)
			torque = 0;
		power = (long)omega*(long)torque;
		soundtick = 2000;
		lastpower = power;
	}

	/** Original: TileEntityEngine.canBeThrottled (by the Engine Control Unit). */
	protected boolean canBeThrottled() {
		return true;
	}

	/** Set while an upgrade replaces this engine with another one (the original cleared the inventory before swapping). */
	protected boolean replacing;

	/** Original BlockTEBase.breakBlock: the inventory drops when the engine is removed. */
	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		if (replacing)
			return;
		for (int i = 0; i < inv.getSlots(); i++)
			net.minecraft.world.Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
		if (integratedGear != 0) { //original breakBlock: an unfilled gear of that ratio
			ItemStack is = reika.rotarycraft.content.item.IntegratedGearboxItem.getIntegratedGearItem(integratedGear, null);
			net.minecraft.world.Containers.dropItemStack(world, pos.getX()+rand.nextDouble(), pos.getY()+rand.nextDouble(), pos.getZ()+rand.nextDouble(), is);
		}
	}

	protected final void dropItem(Level world, BlockPos pos, ItemStack is) {
		net.minecraft.world.Containers.dropItemStack(world, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, is);
	}

	protected boolean canConsumeFuel() {
		return this.getFuelLevel() > 0;
	}

	public final void getIOSides(int metadata) {
		switch(metadata) {
			case 0 -> write = Direction.WEST;
			case 1 -> write = Direction.EAST;
			case 2 -> write = Direction.NORTH;
			case 3 -> write = Direction.SOUTH;
		}
	}

	/** The block behind the engine (original backx/backz), where water and lubricant enter. */
	protected final BlockPos getBackPos() {
		return worldPosition.relative(write == null ? Direction.EAST : write.getOpposite());
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);

		if (this.hasTemperature())
			NBT.putInt("temperature", temperature);

		if (type.needsWater())
			NBT.put("water", water.writeToNBT(registries, new CompoundTag()));
		if (type.isEthanolFueled() || type.isJetFueled())
			NBT.put("fuel", fuel.writeToNBT(registries, new CompoundTag()));
		if (type.requiresLubricant())
			NBT.put("lube", lubricant.writeToNBT(registries, new CompoundTag()));

		if (type.burnsFuel())
			NBT.putInt("fueltimer", timer.getCapOf("fuel"));

		NBT.putInt("gear", integratedGear);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);

		if (this.hasTemperature())
			temperature = NBT.getInt("temperature");

		if (type.requiresLubricant())
			lubricant.readFromNBT(registries, NBT.getCompound("lube"));
		if (type.needsWater())
			water.readFromNBT(registries, NBT.getCompound("water"));
		if (type.isEthanolFueled() || type.isJetFueled())
			fuel.readFromNBT(registries, NBT.getCompound("fuel"));

		if (NBT.contains("fueltimer"))
			timer.setCap("fuel", NBT.getInt("fueltimer"));

		integratedGear = NBT.getInt("gear");
	}

	@Override
	protected void writeAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeAdditional(NBT, registries);
		timer.writeToNBT(NBT, "engine");
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readAdditional(NBT, registries);
		timer.readFromNBT(NBT, "engine");
		inv.deserializeNBT(registries, NBT.getCompound("inv"));

		if (omega > type.getSpeed())
			omega = type.getSpeed();
		if (torque > type.getTorque())
			torque = type.getTorque();
	}

	public final boolean isItemValidForSlot(int i, ItemStack is) {
		if (!type.isValidFuel(is))
			return false;
		if (i >= type.getSizeInventory())
			return false;
		return switch(type) {
			case GAS, AC -> true;
			case SPORT -> (i == 0 && is.is(reika.rotarycraft.registry.RotaryItems.ethanol())) || (i == 1 && type.isAdditive(is));
			default -> false;
		};
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		double pow = 1.05;
		double mult = 1;
		if (type == EngineType.JET)
			pow = 1.1;
		if (type == EngineType.HYDRO)
			mult = 256F/type.getSpeed();
		phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase((int)(mult*omega+1), 2), pow);
	}

	@Override
	public final boolean canProvidePower() {
		return true;
	}

	public final int getThermalDamage() {
		if (type.canHurtPlayer() && this.hasTemperature())
			return (temperature)/100;
		return 0;
	}

	@Override
	public final int getRedstoneOverride() {
		if (type.burnsFuel()) {
			if (type.isEthanolFueled())
				return 15*fuel.getFluidAmount()/FUELCAP;
			if (type.isJetFueled())
				return 15*fuel.getFluidAmount()/FUELCAP;
			else
				return 15*water.getFluidAmount()/FUELCAP;
		}
		return 0;
	}

	public abstract int getFuelLevel();

	@Override
	public final void setTemperature(int temp) {
		temperature = temp;
	}

	public final int getFuelCapacity() {
		if (type.isEthanolFueled())
			return FUELCAP;
		if (type.isJetFueled())
			return FUELCAP;
		if (type == EngineType.STEAM)
			return CAPACITY;
		return 0;
	}

	/** In seconds */
	public final int getFuelDuration() {
		if (!type.burnsFuel())
			return -1;
		int fuel = this.getFuelLevel();
		float burnprogress = 0;
		if (fuel > 0)
			burnprogress = 1F-timer.getPortionOfCap("fuel")/fuel;
		float factor = type.getFuelUnitDuration()/(float)timer.getCapOf("fuel"); //to compensate for 4x burn during spinup
		if (factor <= 0)
			return 0;
		return (int)((fuel*type.getFuelUnitDuration()*(burnprogress))*5/factor/1000*10D/this.getConsumedFuel());
	}

	/** In seconds */
	public final int getFullTankDuration() {
		if (!type.burnsFuel())
			return -1;
		return this.getFuelCapacity()*type.getFuelUnitDuration()*5;
	}

	@Override
	public final void addTemperature(int temp) {
		temperature += temp;
	}

	@Override
	public final int getTemperature() {
		return temperature;
	}

	@Override
	public final void onEMP() {
		if (!type.isEMPImmune())
			super.onEMP();
	}

	@Override
	public PowerSourceList getPowerSources(PowerSourceTracker io, ShaftMerger caller) {
		PowerSourceList psl = new PowerSourceList();
		if (power > 0)
			psl.addSource(this);
		return psl;
	}

	@Override
	public final void getAllOutputs(Collection<BlockEntity> c, Direction dir) {
		c.add(this.getAdjacentTileEntity(write));
	}

	@Override
	public final long getMaxPower() {
		return type.getPower();
	}

	@Override
	public final long getCurrentPower() {
		return power;
	}

	private Direction getFuelInputDirection() {
		return isFlipped ? Direction.UP : Direction.DOWN;
	}

	/** Original canFill: water and lubricant from the back, fuels from below (above when flipped). Nothing can be drained. */
	public final boolean canFill(@Nullable Direction from, Fluid fluid) {
		if (!type.canReceiveFluid(fluid))
			return false;
		if (fluid == Fluids.WATER || fluid == RotaryFluids.LUBRICANT.get()) {
			return from != null && worldPosition.relative(from).equals(this.getBackPos());
		}
		if (fluid == RotaryFluids.JET_FUEL.get() || fluid == RotaryFluids.ETHANOL.get()) {
			return from == this.getFuelInputDirection();
		}
		return false;
	}

	@Override
	public final int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		Fluid f = resource.getFluid();
		if (!this.canFill(from, f))
			return 0;
		int amt;
		if (f == Fluids.WATER)
			amt = water.fill(resource, action);
		else if (f == RotaryFluids.LUBRICANT.get())
			amt = lubricant.fill(resource, action);
		else
			amt = fuel.fill(resource, action);
		if (amt > 0 && action.execute())
			this.setChanged();
		return amt;
	}

	/** Original canConnectToPipe: liquid pipes, fuel lines and hoses. */
	@Override
	public final boolean canConnectToPipe(reika.rotarycraft.content.piping.PipeType m) {
		return m.isStandardPipe() || m == reika.rotarycraft.content.piping.PipeType.FUEL_LINE || m == reika.rotarycraft.content.piping.PipeType.HOSE;
	}

	/** Original canConnectToPipeOnSide: fuel lines (and bedrock pipes) below the fuel engines, water pipes and lubricant hoses at the back. */
	@Override
	public final boolean canConnectToPipeOnSide(reika.rotarycraft.content.piping.PipeType p, Direction side) {
		if (type == null)
			return false;
		if (type.isJetFueled() || type.isEthanolFueled())
			if ((p == reika.rotarycraft.content.piping.PipeType.FUEL_LINE || p == reika.rotarycraft.content.piping.PipeType.BEDROCK_PIPE) && side == this.getFuelInputDirection())
				return true;
		if ((type.isWaterPiped() && p.isStandardPipe()) || (type.requiresLubricant() && (p == reika.rotarycraft.content.piping.PipeType.HOSE || p == reika.rotarycraft.content.piping.PipeType.BEDROCK_PIPE))) {
			return switch (side) {
				case EAST -> this.getBlockMetadata() == 0;
				case SOUTH -> this.getBlockMetadata() == 2;
				case WEST -> this.getBlockMetadata() == 1;
				case NORTH -> this.getBlockMetadata() == 3;
				default -> false;
			};
		}
		return false;
	}

	@Override
	public final FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		return FluidStack.EMPTY;
	}

	@Override
	public final reika.rotarycraft.content.piping.PipeConnector.Flow getFlowForSide(Direction side) {
		return reika.rotarycraft.content.piping.PipeConnector.Flow.INPUT;
	}

	public IFluidHandler getFluidHandler(@Nullable Direction from) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 3;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return switch(t) {
					case 0 -> water.getFluid();
					case 1 -> fuel.getFluid();
					default -> lubricant.getFluid();
				};
			}

			@Override
			public int getTankCapacity(int t) {
				return switch(t) {
					case 0 -> CAPACITY;
					case 1 -> FUELCAP;
					default -> LUBECAP;
				};
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return type.canReceiveFluid(stack.getFluid());
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return EngineBlockEntity.this.fill(from, resource, action);
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

	public final void addFuel(int amt) {
		Fluid f = type.getFuelType();
		if (f != null)
			fuel.fill(new FluidStack(f, amt), IFluidHandler.FluidAction.EXECUTE);
	}

	public final void subtractFuel(int amt) {
		fuel.drain(amt, IFluidHandler.FluidAction.EXECUTE);
	}

	public final void addLubricant(int amt) {
		lubricant.fill(new FluidStack(RotaryFluids.LUBRICANT.get(), amt), IFluidHandler.FluidAction.EXECUTE);
	}

	public final void removeLubricant(int amt) {
		lubricant.drain(amt, IFluidHandler.FluidAction.EXECUTE);
	}

	public final int getLube() {
		return lubricant.getFluidAmount();
	}

	public final void addWater(int amt) {
		water.fill(new FluidStack(Fluids.WATER, amt), IFluidHandler.FluidAction.EXECUTE);
	}

	public final int getWater() {
		return water.getFluidAmount();
	}

	@Override
	public final BlockPos getEmittingPos() {
		return worldPosition.relative(write);
	}

	public boolean isBroken() {
		return false;
	}

	public final int getIntegratedGear() {
		return integratedGear;
	}

	/** Original applyIntegratedGear (IntegratedGearboxable): only a filled upgrade, on a stopped engine without one. */
	public final boolean applyIntegratedGear(ItemStack is) {
		if (is.isEmpty() || !(is.getItem() instanceof reika.rotarycraft.content.item.IntegratedGearboxItem))
			return false;
		if (integratedGear != 0)
			return false;
		if (omega > 0 || power > 0)
			return false;
		integratedGear = reika.rotarycraft.content.item.IntegratedGearboxItem.getRatioFromIntegratedGearItem(is, true);
		this.syncAllData();
		return integratedGear != 0;
	}
}
