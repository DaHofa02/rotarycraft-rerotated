/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.network;

import java.util.ArrayList;
import java.util.LinkedList;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import reika.electricraft.content.blockentity.WiringBlockEntity;

/**
 * Original: Reika.ElectriCraft.Network.PathCalculator - depth-first search from the source through wiring pieces to the sink; every
 * branch stops at the first arrival, the shortest of the found paths is used.
 */
public class PathCalculator {

	private final WireEmitter start;
	private final WireReceiver end;
	private final WireNetwork net;
	private final ArrayList<WirePath> paths = new ArrayList<>();

	public PathCalculator(WireEmitter start, WireReceiver end, WireNetwork w) {
		if (start == end)
			throw new IllegalArgumentException("Cannot connect an object to itself!");
		this.start = start;
		this.end = end;
		net = w;
	}

	public void calculatePaths() {
		BlockPos p = start.getPos();
		Level world = start.getWorld();
		for (Direction dir : Direction.values()) {
			LinkedList<BlockPos> li = new LinkedList<>();
			if (start.canEmitPowerToSide(dir)) {
				BlockPos d = p.relative(dir);
				if (!world.isLoaded(d))
					return;
				if (this.isEnd(d)) {
					if (end.canReceivePower() && end.canReceivePowerFromSide(dir.getOpposite())) {
						paths.add(new WirePath(li, world, start, end, net));
						return;
					}
				}
				else if (world.getBlockEntity(d) instanceof WiringBlockEntity tile && tile.canNetworkOnSide(dir.getOpposite())) {
					this.recursiveCalculate(world, d, li);
				}
			}
		}
	}

	private void recursiveCalculate(Level world, BlockPos p, LinkedList<BlockPos> li) {
		if (li.contains(p))
			return;
		li.addLast(p);
		WiringBlockEntity te = (WiringBlockEntity)world.getBlockEntity(p);
		for (Direction dir : Direction.values()) {
			if (te.canNetworkOnSide(dir)) {
				BlockPos d = p.relative(dir);
				if (world.isLoaded(d)) {
					if (this.isEnd(d)) {
						if (end.canReceivePower() && end.canReceivePowerFromSide(dir.getOpposite())) {
							paths.add(new WirePath(li, world, start, end, net));
							return;
						}
					}
					else if (world.getBlockEntity(d) instanceof WiringBlockEntity tile && tile.canConnect() && tile.canNetworkOnSide(dir.getOpposite())) {
						this.recursiveCalculate(world, d, li);
					}
				}
			}
		}
		li.removeLast();
	}

	private boolean isEnd(BlockPos p) {
		return end.getPos().equals(p);
	}

	@Nullable
	public WirePath getShortestPath() {
		WirePath best = null;
		for (WirePath path : paths)
			if (best == null || path.getLength() < best.getLength())
				best = path;
		return best;
	}
}
