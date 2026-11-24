/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.stacks.AEFluidKey;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;

import reika.rotarycraft.api.interfaces.Screwdriverable;
import reika.rotarycraft.api.power.PowerGenerator;
import reika.rotarycraft.api.power.ShaftMerger;
import reika.rotarycraft.content.blockentity.IOMachineBlockEntity;
import reika.rotarycraft.content.power.PowerSourceList;
import reika.rotarycraft.content.power.PowerSourceTracker;
import reika.rotarycraft.content.power.SimpleProvider;
import reika.rotarycraft.registry.RotaryFluids;

/**
 * ME kinetic output: gives shaft power at its front at the speed and power set in its GUI (torque = power/speed, both capped by the config),
 * paid from the ME network's kinetic energy without loss (power/20 J a tick). It needs lubricant, 1 mB/s per 64 kW by default, down to a
 * quarter of that with 4 acceleration cards (linear in the number of cards), from its own tank first and then from the network's fluid
 * storage; without enough energy or lubricant it stops. Needs a channel.
 */
public class KineticOutputBlockEntity extends IOMachineBlockEntity implements SimpleProvider, PowerGenerator, IInWorldGridNodeHost, IUpgradeableObject, Screwdriverable {

	public static final int TANK = 8000;
	/** Four acceleration cards and one redstone card. */
	public static final int CARD_SLOTS = 5;
	public static final int SPEED_CARDS = 4;

	private final MEGridHost me = new MEGridHost(this, 2);
	private final IUpgradeInventory upgrades;
	private final FluidTank tank = new FluidTank(TANK, fs -> fs.is(RotaryFluids.LUBRICANT.get()));
	private int setSpeed = 0;
	private int setTorque = 0;
	/** With a redstone card: true runs while it gets a signal, false while it gets none. */
	private boolean runWithSignal = true;
	private double energyCarry;
	private double lubeCarry;
	/** Why it is not running, for the GUI: 0 running or off, 1 no network, 2 no energy, 3 no lubricant. */
	public int problem;
	public long usedTotal;
	public long lubeFromNetwork;

	public KineticOutputBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryME.KINETIC_OUTPUT_BE.get(), pos, state);
		upgrades = UpgradeInventories.forMachine(state.getBlock(), CARD_SLOTS, this::setChanged);
	}

	@Override
	public IUpgradeInventory getUpgrades() {
		return upgrades;
	}

	public int getCards() {
		return upgrades.getInstalledUpgrades(RotaryME.speedCard());
	}

	/** Lubricant use relative to no cards; by default 1, 13/16, 10/16, 7/16, 4/16. */
	public double getLubricantFactor() {
		return 1-MEConfig.LUBRICANT_PER_CARD.get()*Math.min(SPEED_CARDS, this.getCards());
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public int getSetSpeed() {
		return setSpeed;
	}

	/** The output power that follows from the set speed and torque (shown in the GUI). */
	public long getSetPower() {
		return (long)setSpeed*setTorque;
	}

	/** Sets the output speed (rad/s) and torque (Nm), each clamped to the configured maximum. */
	public void setOutput(int speed, int torque) {
		setSpeed = Math.max(0, Math.min(MEConfig.MAX_SPEED.get(), speed));
		setTorque = Math.max(0, Math.min(MEConfig.MAX_TORQUE.get(), torque));
		this.syncAllData();
	}

	public int getSetTorque() {
		return setTorque;
	}

	public boolean hasRedstoneCard() {
		return upgrades.isInstalled(RotaryME.redstoneCard());
	}

	public boolean runsWithSignal() {
		return runWithSignal;
	}

	public void toggleRedstoneMode() {
		runWithSignal = !runWithSignal;
		this.syncAllData();
	}

	/** Whether a redstone card is in and the signal says off. */
	public boolean isStoppedByRedstone() {
		return level != null && this.hasRedstoneCard() && level.hasNeighborSignal(worldPosition) != runWithSignal;
	}

	/** Joules a tick the set output takes from the network. */
	public double getEnergyPerTick() {
		return (double)setSpeed*this.getSetTorque()/20D;
	}

	/** Millibuckets a tick of lubricant the set output uses. */
	public double getLubricantPerTick() {
		return (double)setSpeed*this.getSetTorque()/MEConfig.WATTS_PER_LUBRICANT.get()/20D*this.getLubricantFactor();
	}

	public int getLubricant() {
		return tank.getFluidAmount();
	}

	public void addLubricant(int amt) {
		tank.fill(new FluidStack(RotaryFluids.LUBRICANT.get(), amt), IFluidHandler.FluidAction.EXECUTE);
	}

	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return side == write && side != null ? null : tank;
	}

	public long getNetworkEnergy() {
		return me.stored(KineticKey.INSTANCE);
	}

	/** Lubricant (mB) the output can reach in the ME network's storage, or -1 without an active network (no network, power or channel). */
	public long getNetworkLubricant() {
		return me.isActive() ? me.stored(AEFluidKey.of(RotaryFluids.LUBRICANT.get())) : -1;
	}

	/** Whether this tick's lubricant comes from its own tank (true while the tank holds any) or from the ME network. */
	public boolean usesOwnTank() {
		return tank.getFluidAmount() > 0;
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
		me.tick(world, write);
		boolean run = this.payForTick();
		omega = run ? setSpeed : 0;
		torque = run ? this.getSetTorque() : 0;
		power = (long)omega*torque;
		this.basicPowerReceiver();
	}

	/** Takes this tick's energy and lubricant, all or nothing. */
	private boolean payForTick() {
		if (setSpeed <= 0 || this.getSetTorque() <= 0) {
			problem = 0;
			return false;
		}
		if (this.isStoppedByRedstone()) {
			problem = 4;
			return false;
		}
		if (!me.isActive()) {
			problem = 1;
			return false;
		}
		long j = (long)(energyCarry+this.getEnergyPerTick());
		long mb = (long)(lubeCarry+this.getLubricantPerTick());
		if (j > 0 && me.extract(KineticKey.INSTANCE, j, Actionable.SIMULATE) < j) {
			problem = 2;
			return false;
		}
		long fromTank = Math.min(mb, tank.getFluidAmount());
		long fromNet = mb-fromTank;
		AEFluidKey lube = AEFluidKey.of(RotaryFluids.LUBRICANT.get());
		if (fromNet > 0 && me.extract(lube, fromNet, Actionable.SIMULATE) < fromNet) {
			problem = 3;
			return false;
		}
		energyCarry += this.getEnergyPerTick()-j;
		lubeCarry += this.getLubricantPerTick()-mb;
		if (j > 0)
			usedTotal += me.extract(KineticKey.INSTANCE, j, Actionable.MODULATE);
		if (fromTank > 0)
			tank.drain((int)fromTank, IFluidHandler.FluidAction.EXECUTE);
		if (fromNet > 0)
			lubeFromNetwork += me.extract(lube, fromNet, Actionable.MODULATE);
		problem = 0;
		return true;
	}

	@Nullable
	@Override
	public IGridNode getGridNode(Direction dir) {
		return me.getNode(dir);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		me.destroy();
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		me.destroy();
	}

	@Override
	public boolean canProvidePower() {
		return true;
	}

	@Override
	public PowerSourceList getPowerSources(PowerSourceTracker io, ShaftMerger caller) {
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
		return (long)MEConfig.MAX_SPEED.get()*MEConfig.MAX_TORQUE.get();
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
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < upgrades.size(); i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), upgrades.getStackInSlot(i).copy());
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		me.save(NBT);
		upgrades.writeToNBT(NBT, "upgrades", registries);
		NBT.putInt("speed", setSpeed);
		NBT.putInt("torque", setTorque);
		NBT.putBoolean("rsoff", !runWithSignal);
		NBT.putInt("lube", tank.getFluidAmount());
		NBT.putInt("problem", problem);
		NBT.putLong("net", this.getNetworkEnergy());
		NBT.putLong("netlube", this.getNetworkLubricant());
	}

	public long networkEnergyClient;
	/** {@link #getNetworkLubricant()} as last synced, for the GUI. */
	public long networkLubricantClient = -1;

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		me.load(NBT);
		upgrades.readFromNBT(NBT, "upgrades", registries);
		setSpeed = NBT.getInt("speed");
		//saves from before 2026-11-19 held the power instead of the torque
		runWithSignal = !NBT.getBoolean("rsoff");
		setTorque = NBT.contains("torque") ? NBT.getInt("torque") : setSpeed <= 0 ? 0 : (int)Math.min(MEConfig.MAX_TORQUE.get(), NBT.getLong("power")/setSpeed);
		int l = NBT.getInt("lube");
		tank.setFluid(l > 0 ? new FluidStack(RotaryFluids.LUBRICANT.get(), l) : FluidStack.EMPTY);
		problem = NBT.getInt("problem");
		networkEnergyClient = NBT.getLong("net");
		networkLubricantClient = NBT.contains("netlube") ? NBT.getLong("netlube") : -1;
	}

	/** The screwdriver turns it a quarter round (its in/out boxes show for a moment, like RotaryCraft machines). */
	@Override
	public boolean onRightClick(Level world, BlockPos pos, Direction side) {
		this.setBlockMetadata((this.getBlockMetadata()+1)%4);
		this.getIOSides(this.getBlockMetadata());
		world.updateNeighborsAt(pos, this.getBlockState().getBlock());
		this.syncAllData();
		return true;
	}

	@Override
	public boolean onShiftRightClick(Level world, BlockPos pos, Direction side) {
		return false;
	}
}
