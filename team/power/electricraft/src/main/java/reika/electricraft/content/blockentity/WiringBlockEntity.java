/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Original: Reika.ElectriCraft.Base.WiringTile - a piece of a wire path; knows the voltage and current flowing through it. */
public abstract class WiringBlockEntity extends NetworkBlockEntity {

	private int voltage;
	private int current;

	protected WiringBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public abstract int getResistance();

	/** Original TileEntityWireComponent.canConnect (relays); plain wires always connect. */
	public boolean canConnect() {
		return true;
	}

	@Override
	protected void onJoinNetwork() {
		current = network.getPointCurrent(this);
		voltage = network.getPointVoltage(this);
	}

	@Override
	public void onNetworkChanged() {
		current = network.getPointCurrent(this);
		voltage = network.getPointVoltage(this);
	}

	public final int getWireVoltage() {
		return voltage;
	}

	public final int getWireCurrent() {
		return current;
	}

	public final long getWirePower() {
		return (long)current*(long)voltage;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("a", current);
		NBT.putInt("v", voltage);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		voltage = NBT.getInt("v");
		current = NBT.getInt("a");
	}
}
