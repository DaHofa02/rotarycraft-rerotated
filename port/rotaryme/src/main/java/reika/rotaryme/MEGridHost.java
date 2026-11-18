/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.config.Actionable;
import appeng.api.storage.StorageHelper;

/** The ME side of the kinetic input and output: one grid node (needs a channel) on every side but the shaft side. */
public final class MEGridHost implements IActionHost {

	private static final IGridNodeListener<BlockEntity> LISTENER = (owner, node) -> owner.setChanged();

	private final BlockEntity owner;
	private final IManagedGridNode node;
	private final IActionSource source;
	private boolean created;

	public MEGridHost(BlockEntity owner, double idlePower) {
		this.owner = owner;
		node = GridHelper.createManagedNode(owner, LISTENER).setFlags(GridFlags.REQUIRE_CHANNEL).setIdlePowerUsage(idlePower)
				.setInWorldNode(true).setVisualRepresentation(owner.getBlockState().getBlock());
		source = IActionSource.ofMachine(this);
	}

	/** Called every server tick; creates the node on the first one and keeps the shaft side unconnected. */
	public void tick(Level level, @Nullable Direction shaftSide) {
		if (!created) {
			created = true;
			EnumSet<Direction> sides = EnumSet.allOf(Direction.class);
			if (shaftSide != null)
				sides.remove(shaftSide);
			node.setExposedOnSides(sides);
			node.create(level, owner.getBlockPos());
		}
	}

	public void destroy() {
		node.destroy();
		created = false;
	}

	@Nullable
	public IGridNode getNode(@Nullable Direction side) {
		return node.getNode();
	}

	@Override
	public IGridNode getActionableNode() {
		return node.getNode();
	}

	public boolean isActive() {
		return node.isActive();
	}

	@Nullable
	public IGrid grid() {
		IGridNode n = node.getNode();
		return n != null && n.isActive() ? n.getGrid() : null;
	}

	/** Inserts into the network's storage (paying AE energy for it), returns the amount taken. */
	public long insert(AEKey key, long amount, Actionable mode) {
		IGrid g = this.grid();
		return g == null || amount <= 0 ? 0 : StorageHelper.poweredInsert(g.getEnergyService(), g.getStorageService().getInventory(), key, amount, source, mode);
	}

	public long extract(AEKey key, long amount, Actionable mode) {
		IGrid g = this.grid();
		return g == null || amount <= 0 ? 0 : StorageHelper.poweredExtraction(g.getEnergyService(), g.getStorageService().getInventory(), key, amount, source, mode);
	}

	/** What the network holds of that key (0 without a network). */
	public long stored(AEKey key) {
		IGrid g = this.grid();
		return g == null ? 0 : g.getStorageService().getCachedInventory().get(key);
	}

	public void save(CompoundTag tag) {
		node.saveToNBT(tag);
	}

	public void load(CompoundTag tag) {
		node.loadFromNBT(tag);
	}
}
