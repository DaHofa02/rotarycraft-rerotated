/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Original: Reika.RotaryCraft.Auxiliary.ShaftPowerBus - the power bus blocks run by one bus controller. The controller's power is
 * split evenly over every side of every bus block that can give power out.
 */
public class ShaftPowerBus {

	private final BusControllerBlockEntity hub;
	private final List<PowerBusBlockEntity> blocks = new ArrayList<>();
	private int sides;

	public ShaftPowerBus(BusControllerBlockEntity hub) {
		this.hub = hub;
	}

	public void removeBlock(PowerBusBlockEntity bus) {
		blocks.remove(bus);
		this.rebuild(bus.getBlockPos());
	}

	/** Original rebuild: keeps only the bus blocks still connected (the one being removed does not count; it is still in the world) (6-neighbour chains) to a horizontal side of the controller. */
	private void rebuild(BlockPos removed) {
		Set<BlockPos> found = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		for (Direction d : Direction.Plane.HORIZONTAL) {
			BlockPos p = hub.getBlockPos().relative(d);
			if (!p.equals(removed) && hub.getLevel().getBlockEntity(p) instanceof PowerBusBlockEntity && found.add(p))
				queue.add(p);
		}
		while (!queue.isEmpty()) {
			BlockPos c = queue.poll();
			for (Direction d : Direction.values()) {
				BlockPos n = c.relative(d);
				if (!n.equals(removed) && !found.contains(n) && hub.getLevel().getBlockEntity(n) instanceof PowerBusBlockEntity) {
					found.add(n);
					queue.add(n);
				}
			}
		}
		Iterator<PowerBusBlockEntity> it = blocks.iterator();
		while (it.hasNext()) {
			PowerBusBlockEntity te = it.next();
			if (!found.contains(te.getBlockPos())) {
				te.clearBus();
				it.remove();
			}
		}
	}

	public boolean addBlock(PowerBusBlockEntity bus) {
		if (blocks.contains(bus))
			return false;
		blocks.add(bus);
		this.update();
		return true;
	}

	public BusControllerBlockEntity getController() {
		return hub;
	}

	public long getInputPower() {
		return hub.power;
	}

	public int getSpeed() {
		return hub.omega;
	}

	public int getInputTorque() {
		return hub.torque;
	}

	public void recalcTotalOutputSides() {
		sides = 0;
		for (PowerBusBlockEntity te : blocks)
			for (Direction dir : Direction.Plane.HORIZONTAL)
				if (te.canOutputToSide(dir))
					sides++;
	}

	public int getTotalOutputSides() {
		return Math.max(sides, 1);
	}

	public void update() {
		this.recalcTotalOutputSides();
	}

	public void clear() {
		for (PowerBusBlockEntity te : blocks)
			te.clearBus();
		blocks.clear();
	}

	public Collection<PowerBusBlockEntity> getBlocks() {
		return Collections.unmodifiableCollection(blocks);
	}

	public int getSize() {
		return blocks.size();
	}
}
