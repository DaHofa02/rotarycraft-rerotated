/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import reika.electricraft.content.blockentity.WiringBlockEntity;

/**
 * Original: Reika.ElectriCraft.Network.WirePath - one chain of wiring pieces from a source to a sink. Its voltage drop is the summed
 * resistance; its current is the source current split between all paths of that source (limited paths give their surplus to the rest).
 */
public final class WirePath {

	final LinkedList<WiringBlockEntity> nodes = new LinkedList<>();
	final Collection<WireFuse> fuses = new ArrayList<>();
	final Collection<ToggledConnection> toggles = new ArrayList<>();
	final WireEmitter start;
	final WireReceiver end;
	private final WireNetwork net;
	public final int resistance;
	private final int currentLimit;

	public WirePath(List<BlockPos> points, Level world, WireEmitter start, WireReceiver end, WireNetwork net) {
		this.start = start;
		this.end = end;
		this.net = net;
		int maxcurrent = Integer.MAX_VALUE;
		int r = 0;
		for (BlockPos loc : points) {
			if (!(world.getBlockEntity(loc) instanceof WiringBlockEntity te))
				continue;
			nodes.addLast(te);
			r += te.getResistance();
			if (te instanceof CurrentThrottle ct)
				maxcurrent = Math.min(maxcurrent, ct.getCurrentLimit());
			if (te instanceof WireFuse f)
				fuses.add(f);
			if (te instanceof ToggledConnection tc)
				toggles.add(tc);
		}
		resistance = r;
		currentLimit = maxcurrent;
	}

	public int getLength() {
		return nodes.size();
	}

	public int getVoltageAt(WiringBlockEntity wire) {
		return start.canEmitPower() && start.getGenVoltage() > 0 ? Math.max(0, start.getGenVoltage()-this.getResistanceTo(nodes.indexOf(wire))) : 0;
	}

	private int getResistanceTo(int index) {
		int r = 0;
		for (int i = 0; i < index; i++)
			r += nodes.get(i).getResistance();
		return r;
	}

	public boolean containsBlock(WiringBlockEntity te) {
		return nodes.contains(te);
	}

	boolean tick() {
		int current = this.getPathCurrent();
		boolean flag = false;
		for (WireFuse f : fuses) {
			if (current > f.getMaxCurrent()) {
				f.overload(current);
				flag = true;
			}
		}
		return flag;
	}

	public boolean startsAt(BlockPos p) {
		return start.getPos().equals(p);
	}

	public boolean endsAt(BlockPos p) {
		return end.getPos().equals(p);
	}

	public int getTerminalVoltage() {
		int v = start.getGenVoltage() > 0 ? start.getGenVoltage()-this.getVoltageLoss() : 0;
		return Math.max(v, 0);
	}

	public int getVoltageLoss() {
		return resistance;
	}

	public boolean isLimitedCurrent() {
		return currentLimit < Integer.MAX_VALUE;
	}

	public int getPathCurrent() {
		if (!start.canEmitPower())
			return 0;
		int total = start.getGenCurrent();
		ArrayList<WirePath> li = net.getPathsStartingAt(start);
		int num = li.size();
		if (num == 0)
			return 0;
		int frac = total/num;
		int bonus = 0;
		int unfilled = 0;
		for (WirePath path : li) {
			if (path.isLimitedCurrent()) {
				int max = path.currentLimit;
				if (max <= frac)
					bonus += frac-max;
				else
					unfilled++;
			}
			else {
				unfilled++;
			}
		}
		int frac2 = unfilled > 0 ? bonus/unfilled : 0;
		return Math.min(frac+frac2, currentLimit);
	}

	public boolean canConduct() {
		for (ToggledConnection tc : toggles)
			if (!tc.canConnect())
				return false;
		return true;
	}

	@Override
	public String toString() {
		return "("+start.getPos().toShortString()+") > ("+end.getPos().toShortString()+") {"+this.hashCode()+"}";
	}
}
