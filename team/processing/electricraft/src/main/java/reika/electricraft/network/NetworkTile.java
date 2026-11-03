/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Original: Reika.ElectriCraft.Auxiliary.Interfaces.NetworkTile - anything that is part of a wire network. */
public interface NetworkTile {

	void findAndJoinNetwork(Level world, BlockPos pos);

	WireNetwork getNetwork();

	void setNetwork(WireNetwork n);

	void removeFromNetwork();

	void resetNetwork();

	boolean isConnectable();

	void onNetworkChanged();

	boolean canNetworkOnSide(Direction dir);

	Level getWorld();

	BlockPos getPos();
}
