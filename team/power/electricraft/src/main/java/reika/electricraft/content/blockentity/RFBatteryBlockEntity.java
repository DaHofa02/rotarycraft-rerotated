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

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import reika.electricraft.registry.ElectriBlockEntities;
import reika.electricraft.registry.ElectriComponents;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/**
 * Original: TileEntityRFBattery (BatteryTileBase) - stores up to 6*10^13 RF (here FE), takes energy on every side but the top, gives it
 * out only through the top and, with a redstone signal, pushes it into the block above; comparator round(15*(stored/max)^0.1).
 */
public class RFBatteryBlockEntity extends RotaryCraftBlockEntity {

	public static final long CAPACITY = 60000000000000L;

	private long energy;

	public RFBatteryBlockEntity(BlockPos pos, BlockState state) {
		super(ElectriBlockEntities.RF_BATTERY.get(), pos, state);
	}

	@Override
	public String getName() {
		return "RF Storage Battery";
	}

	public long getStoredEnergy() {
		return energy;
	}

	public void setStoredEnergy(long e) {
		energy = e;
		this.setChanged();
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (world.isClientSide)
			return;
		if (world.getGameTime()%64 == 0) {
			this.syncAllData();
			world.updateNeighbourForOutputSignal(pos, state.getBlock());
		}
		if (world.hasNeighborSignal(pos)) {
			int exp = (int)Math.min(energy, Integer.MAX_VALUE);
			if (exp > 0) {
				IEnergyStorage ier = world.getCapability(Capabilities.EnergyStorage.BLOCK, pos.above(), Direction.DOWN);
				if (ier != null && ier.canReceive()) {
					energy -= ier.receiveEnergy(exp, false);
					this.setChanged();
				}
			}
		}
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return (int)Math.round(15D*Math.pow(energy/(double)CAPACITY, 0.1));
	}

	/** Original IEnergyHandler with the side rules. */
	public IEnergyStorage getEnergyStorage(Direction side) {
		boolean top = side == Direction.UP;
		return new IEnergyStorage() {
			@Override
			public int receiveEnergy(int amt, boolean simulate) {
				long ret = !top ? Math.min(amt, CAPACITY-energy) : 0;
				if (ret > 0 && !simulate) {
					energy += ret;
					RFBatteryBlockEntity.this.setChanged();
				}
				return (int)ret;
			}

			@Override
			public int extractEnergy(int amt, boolean simulate) {
				long ret = top ? Math.min(amt, energy) : 0;
				if (ret > 0 && !simulate) {
					energy -= ret;
					RFBatteryBlockEntity.this.setChanged();
				}
				return (int)ret;
			}

			@Override
			public int getEnergyStored() {
				return energy == CAPACITY ? Integer.MAX_VALUE : (int)Math.min(energy, Integer.MAX_VALUE-1);
			}

			@Override
			public int getMaxEnergyStored() {
				return Integer.MAX_VALUE;
			}

			@Override
			public boolean canExtract() {
				return top;
			}

			@Override
			public boolean canReceive() {
				return !top;
			}
		};
	}

	@Override
	protected void applyImplicitComponents(DataComponentInput input) {
		super.applyImplicitComponents(input);
		Long e = input.get(ElectriComponents.BATTERY_ENERGY.get());
		energy = e != null ? e : 0;
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		components.set(ElectriComponents.BATTERY_ENERGY.get(), energy);
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
