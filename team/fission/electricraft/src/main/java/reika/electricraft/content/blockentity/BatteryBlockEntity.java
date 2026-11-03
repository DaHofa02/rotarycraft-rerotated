/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.electricraft.content.block.BatteryBlock;
import reika.electricraft.network.WireEmitter;
import reika.electricraft.network.WireReceiver;
import reika.electricraft.registry.BatteryType;
import reika.electricraft.registry.ElectriBlockEntities;
import reika.electricraft.registry.ElectriComponents;

/**
 * Original: Reika.ElectriCraft.TileEntities.TileEntityBattery - charges from the network on every side but the top (V*A per tick, up to
 * its capacity) and, with a redstone signal, gives its tier's fixed voltage and current from the top. The charge is kept on the item.
 */
public class BatteryBlockEntity extends NetworkBlockEntity implements WireEmitter, WireReceiver {

	/** In J/20 (one tick of watts), as in the original. */
	private long energy;
	private long lastE;
	private boolean lastPower;

	public BatteryBlockEntity(BlockPos pos, BlockState state) {
		super(ElectriBlockEntities.BATTERY.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Battery";
	}

	public BatteryType getBatteryType() {
		return this.getBlockState().getBlock() instanceof BatteryBlock b ? b.type : BatteryType.REDSTONE;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		super.updateEntity(world, pos, state);
		if (world.isClientSide || network == null)
			return;
		if (world.getGameTime()%64 == 0)
			world.updateNeighbourForOutputSignal(pos, state.getBlock());
		if (this.canReceivePower()) {
			int v = network.getTerminalVoltage(this);
			int a = network.getTerminalCurrent(this);
			long p = (long)v*(long)a;
			if (energy/20 < this.getMaxEnergy())
				energy += p;
			if (energy/20 >= this.getMaxEnergy()) {
				energy = this.getMaxEnergy()*20;
				network.updateWires();
			}
		}
		boolean flag = world.hasNeighborSignal(pos);
		if (flag != lastPower)
			network.updateWires();
		if (this.canEmitPower()) {
			long p = (long)this.getGenVoltage()*(long)this.getGenCurrent();
			energy -= p;
			if (energy <= 0) {
				energy = 0;
				network.updateWires();
			}
			else if (lastE == 0) {
				network.updateWires();
			}
		}
		lastPower = flag;
		if (energy != lastE)
			this.setChanged();
		if ((energy > 0) != (lastE > 0))
			BatteryBlock.setCharged(world, pos, energy > 0);
		lastE = energy;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public boolean canEmitPower() {
		return energy > 0 && level != null && level.hasNeighborSignal(worldPosition) && network != null && network.getNumberPathsStartingAt(this) > 0;
	}

	public long getStoredEnergy() {
		return energy/20;
	}

	public long getMaxEnergy() {
		return this.getBatteryType().maxCapacity;
	}

	/** Stored energy in J. */
	public void setStoredEnergy(long joules) {
		energy = joules*20L;
		this.setChanged();
	}

	@Override
	public boolean canNetworkOnSide(Direction dir) {
		return true;
	}

	@Override
	public boolean canReceivePowerFromSide(Direction dir) {
		return dir != Direction.UP;
	}

	@Override
	public boolean canEmitPowerToSide(Direction dir) {
		return dir == Direction.UP;
	}

	@Override
	public int getGenVoltage() {
		return this.canEmitPower() ? this.getBatteryType().outputVoltage : 0;
	}

	@Override
	public int getGenCurrent() {
		return this.canEmitPower() ? this.getBatteryType().outputCurrent : 0;
	}

	@Override
	public boolean canReceivePower() {
		return energy < this.getMaxEnergy()*20;
	}

	@Override
	public int getRedstoneOverride() {
		return (int)(15D*this.getStoredEnergy()/this.getMaxEnergy());
	}

	@Override
	protected void applyImplicitComponents(DataComponentInput input) {
		super.applyImplicitComponents(input);
		Long e = input.get(ElectriComponents.BATTERY_ENERGY.get());
		energy = e != null ? e*20L : 0;
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		components.set(ElectriComponents.BATTERY_ENERGY.get(), this.getStoredEnergy());
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putLong("e", energy);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		energy = NBT.getLong("e");
	}
}
