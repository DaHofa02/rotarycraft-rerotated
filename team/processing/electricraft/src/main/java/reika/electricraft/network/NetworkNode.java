/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.network;

import java.util.ArrayList;
import java.util.Collection;

import net.minecraft.core.Direction;

import reika.electricraft.content.blockentity.WiringBlockEntity;

/** Original: Reika.ElectriCraft.Network.NetworkNode - a wire junction with more than two connections. */
public final class NetworkNode {

	private final WiringBlockEntity wire;
	private final WireNetwork network;
	private final ArrayList<Direction> connections = new ArrayList<>();

	NetworkNode(WireNetwork w, WiringBlockEntity te, Collection<Direction> sides) {
		network = w;
		wire = te;
		connections.addAll(sides);
	}

	public boolean connectsToSide(Direction dir) {
		return connections.contains(dir);
	}

	@Override
	public String toString() {
		return "<"+wire.getBlockPos().toShortString()+">";
	}
}
