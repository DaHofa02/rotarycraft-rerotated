/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.electricraft.ElectriCraft;
import reika.electricraft.network.NetworkTile;
import reika.electricraft.network.WireNetwork;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/**
 * Original: Reika.ElectriCraft.Base.NetworkTileEntity - joins (or founds) a wire network on its first tick, merging with the networks of
 * connectable neighbours; leaves it when removed or unloaded. Built on the ported RotaryCraft tile base (DragonAPI TileEntityBase).
 */
public abstract class NetworkBlockEntity extends RotaryCraftBlockEntity implements NetworkTile {

	@Nullable
	protected WireNetwork network;
	private boolean isConnectable = true;

	protected NetworkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if ((this.getTicksExisted() == 0 || network == null) && !world.isClientSide)
			this.findAndJoinNetwork(world, pos);
	}

	/** Original onInvalidateOrUnload. */
	private void leave() {
		if (!isConnectable)
			return;
		isConnectable = false;
		if (network != null && level != null && !level.isClientSide)
			network.removeElement(this);
	}

	@Override
	public void setRemoved() {
		this.leave();
		super.setRemoved();
	}

	@Override
	public void onChunkUnloaded() {
		this.leave();
		super.onChunkUnloaded();
	}

	@Override
	public final boolean isConnectable() {
		return isConnectable && level != null && !level.isClientSide && level.isLoaded(worldPosition);
	}

	@Override
	public void onNetworkChanged() {}

	@Override
	public final void findAndJoinNetwork(Level world, BlockPos pos) {
		network = null;
		if (!isConnectable)
			return;
		network = new WireNetwork();
		network.addElement(this);
		for (Direction dir : Direction.values()) {
			if (this.canNetworkOnSide(dir) && world.isLoaded(pos.relative(dir)))
				this.linkTile(world.getBlockEntity(pos.relative(dir)), dir);
		}
		this.onJoinNetwork();
	}

	private void linkTile(@Nullable BlockEntity te, Direction dir) {
		if (te instanceof NetworkBlockEntity n && n.canNetworkOnSide(dir.getOpposite())) {
			WireNetwork w = n.network;
			if (w != null)
				w.merge(network);
		}
	}

	protected void onJoinNetwork() {}

	@Override
	public final WireNetwork getNetwork() {
		return network;
	}

	@Override
	public final void setNetwork(WireNetwork n) {
		if (n == null) {
			ElectriCraft.LOGGER.error("{} was told to join a null network!", this);
		}
		else {
			network = n;
			network.addElement(this);
		}
	}

	@Override
	public final void removeFromNetwork() {
		if (network != null)
			network.removeElement(this);
	}

	public final void rebuildNetwork() {
		this.removeFromNetwork();
		this.resetNetwork();
		if (level != null)
			this.findAndJoinNetwork(level, worldPosition);
	}

	@Override
	public final void resetNetwork() {
		network = null;
	}

	@Override
	public final Level getWorld() {
		return level;
	}

	@Override
	public final BlockPos getPos() {
		return worldPosition;
	}
}
