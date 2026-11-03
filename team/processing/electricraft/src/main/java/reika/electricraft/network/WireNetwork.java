/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.electricraft.ElectriCraft;
import reika.electricraft.content.blockentity.WireBlockEntity;
import reika.electricraft.content.blockentity.WiringBlockEntity;
import reika.rotarycraft.api.power.ShaftMerger;
import reika.rotarycraft.content.power.PowerSourceList;
import reika.rotarycraft.content.power.PowerSourceTracker;

/**
 * Original: Reika.ElectriCraft.Network.WireNetwork. A connected set of wires, sources and sinks. For every (source, sink) pair the
 * shortest wire path is found; a path delivers the source voltage minus the summed wire resistance, and the source current is shared
 * between the paths that start at it (current-limited paths pass their surplus to the others). Wires carrying more current than their
 * type allows melt. Recalculation is queued and done at the end of the server tick (original repath event).
 * Port notes: networks stay in one dimension (ChromatiCraft rifts are not ported); elements are keyed by GlobalPos.
 */
public final class WireNetwork {

	private final Map<GlobalPos, WiringBlockEntity> wires = new HashMap<>();
	private final Map<GlobalPos, WireReceiver> sinks = new HashMap<>();
	private final Map<GlobalPos, WireEmitter> sources = new HashMap<>();
	private final Map<BlockPos, NetworkNode> nodes = new HashMap<>();
	private final Collection<WirePath> paths = new ArrayList<>();

	private final Map<WiringBlockEntity, Integer> pointVoltages = new HashMap<>();
	private final Map<WiringBlockEntity, Integer> pointCurrents = new HashMap<>();
	private final Map<WireReceiver, Integer> terminalVoltages = new HashMap<>();
	private final Map<WireReceiver, Integer> terminalCurrents = new HashMap<>();
	private final Map<WireReceiver, Integer> avgCurrents = new HashMap<>();
	private final Map<WireReceiver, Float> numSources = new HashMap<>();

	private int interWireConnections;
	private boolean shorted = false;
	private boolean reUpdateThisTick;

	public static final int TORQUE_PER_AMP = 8;
	private static final int MAX_PATHS = 500;
	private static final int MAX_INTERWIRE = 500;

	public WireNetwork() {
		ElectriNetworkManager.addNetwork(this);
	}

	public void clearCache() {
		terminalCurrents.clear();
		terminalVoltages.clear();
		pointCurrents.clear();
		pointVoltages.clear();
		avgCurrents.clear();
		numSources.clear();
	}

	private int getMaxInputVoltage() {
		int max = 0;
		for (WireEmitter e : sources.values())
			max = Math.max(max, e.getGenVoltage());
		return max;
	}

	public boolean isLive() {
		return this.getMaxInputVoltage() > 0;
	}

	public int getNumberSources() {
		return sources.size();
	}

	public int getNumberSinks() {
		return sinks.size();
	}

	/** Original tick (server tick start): fuses, wire overload, discarding empty networks. */
	void tick() {
		boolean flag = false;
		for (WirePath path : paths) {
			if (path.tick()) {
				this.updateWires(false);
				flag = true;
			}
		}
		for (WireEmitter src : sources.values()) {
			if (src instanceof WrappedSource ws && ws.needsUpdate()) {
				this.updateWires(false);
				flag = true;
			}
		}
		if (flag)
			return;
		for (WiringBlockEntity w : new ArrayList<>(wires.values())) {
			if (w instanceof WireBlockEntity te) {
				int current = this.getPointCurrent(w);
				if (current > te.getMaxCurrent())
					te.overload(current);
			}
		}
		if (this.isEmpty()) {
			this.clear(true);
			ElectriNetworkManager.scheduleNetworkDiscard(this);
		}
		if (shorted) {
			shorted = false;
			this.updateWires(false);
		}
	}

	/** Original repath (server tick end). */
	void repath() {
		if (reUpdateThisTick) {
			this.doRepath();
			reUpdateThisTick = false;
		}
	}

	private void doRepath() {
		this.recalculatePaths();
		for (WiringBlockEntity w : new ArrayList<>(wires.values()))
			w.onNetworkChanged();
		for (WireEmitter w : new ArrayList<>(sources.values()))
			w.onNetworkChanged();
		for (WireReceiver w : new ArrayList<>(sinks.values()))
			w.onNetworkChanged();
	}

	public int getPointVoltage(WiringBlockEntity te) {
		if (shorted)
			return 0;
		return pointVoltages.computeIfAbsent(te, this::calcPointVoltage);
	}

	public int getPointCurrent(WiringBlockEntity te) {
		if (shorted)
			return 0;
		return pointCurrents.computeIfAbsent(te, this::calcPointCurrent);
	}

	private int calcPointVoltage(WiringBlockEntity te) {
		int sv = Integer.MAX_VALUE;
		for (WirePath path : paths) {
			if (path.containsBlock(te) && path.start.getGenVoltage() > 0) {
				int v = path.getVoltageAt(te);
				if (v < sv)
					sv = v;
			}
		}
		return sv == Integer.MAX_VALUE ? 0 : sv;
	}

	private int calcPointCurrent(WiringBlockEntity te) {
		int sa = 0;
		for (WirePath path : paths)
			if (path.containsBlock(te))
				sa += path.getPathCurrent();
		return sa;
	}

	public boolean isEmpty() {
		return wires.isEmpty() && sinks.isEmpty() && sources.isEmpty();
	}

	public void merge(WireNetwork n) {
		if (n != this) {
			ArrayList<NetworkTile> li = new ArrayList<>();
			for (WiringBlockEntity wire : new ArrayList<>(n.wires.values())) {
				wire.setNetwork(this);
				li.add(wire);
			}
			for (WireReceiver r : new ArrayList<>(n.sinks.values())) {
				if (!li.contains(r)) {
					r.setNetwork(this);
					li.add(r);
				}
			}
			for (WireEmitter s : new ArrayList<>(n.sources.values())) {
				if (!li.contains(s)) {
					s.setNetwork(this);
					li.add(s);
				}
			}
			nodes.putAll(n.nodes);
			interWireConnections += n.interWireConnections;
			n.clear(false);
		}
		this.updateWires(true);
	}

	private void clear(boolean clearTiles) {
		if (clearTiles) {
			for (NetworkTile te : this.allElements())
				te.resetNetwork();
		}
		wires.clear();
		sinks.clear();
		sources.clear();
		nodes.clear();
		paths.clear();
		this.clearCache();
	}

	private ArrayList<NetworkTile> allElements() {
		ArrayList<NetworkTile> li = new ArrayList<>(wires.values());
		for (NetworkTile te : sinks.values())
			if (!li.contains(te))
				li.add(te);
		for (NetworkTile te : sources.values())
			if (!li.contains(te))
				li.add(te);
		return li;
	}

	private static GlobalPos key(NetworkTile te) {
		return GlobalPos.of(te.getWorld().dimension(), te.getPos());
	}

	public void addElement(NetworkTile te) {
		boolean changed = false;
		if (!te.isConnectable())
			return;
		if (te instanceof WireEmitter e)
			changed |= !te.equals(sources.put(key(te), e));
		if (te instanceof WireReceiver r)
			changed |= !te.equals(sinks.put(key(te), r));
		if (te instanceof WiringBlockEntity wire) {
			changed |= !te.equals(wires.put(key(te), wire));
			Level world = wire.getLevel();
			for (Direction side : Direction.values()) {
				//never load a chunk from here: networks also rebuild while chunks unload
				if (world != null && world.isLoaded(wire.getBlockPos().relative(side)) && wire.getAdjacentTileEntity(side) instanceof WiringBlockEntity wire2) {
					ArrayList<Direction> sides = new ArrayList<>();
					for (Direction dir : Direction.values()) {
						BlockEntity adj = world.isLoaded(wire2.getBlockPos().relative(dir)) ? wire2.getAdjacentTileEntity(dir) : null;
						if (adj instanceof NetworkTile nw && nw.canNetworkOnSide(dir.getOpposite()))
							sides.add(dir);
					}
					if (sides.size() > 2 && !this.hasNode(wire2.getBlockPos())) {
						nodes.put(wire2.getBlockPos(), new NetworkNode(this, wire2, sides));
						interWireConnections += sides.size()-2;
					}
				}
			}
		}
		if (changed)
			this.updateWires(true);
	}

	public void updateWires() {
		this.updateWires(false);
	}

	void updateWires(boolean force) {
		if (force)
			this.doRepath();
		else
			reUpdateThisTick = true;
	}

	private void recalculatePaths() {
		paths.clear();
		interWireConnections = 0;
		this.clearCache();
		for (WireEmitter src : sources.values()) {
			for (WireReceiver sink : sinks.values()) {
				if (src != sink) {
					PathCalculator pc = new PathCalculator(src, sink, this);
					pc.calculatePaths();
					WirePath path = pc.getShortestPath();
					if (path != null) {
						paths.add(path);
						this.validatePathLimit();
					}
				}
			}
		}
		ElectriCraft.LOGGER.debug("Remapped network {}", this);
	}

	/** Original: too many paths or crossings melt every wire of the network. */
	private void validatePathLimit() {
		if (paths.size() >= MAX_PATHS || interWireConnections > MAX_INTERWIRE) {
			for (WiringBlockEntity w : wires.values())
				if (w instanceof WireBlockEntity wire)
					wire.overload(Integer.MAX_VALUE);
		}
	}

	public float getNumberSourcesPer(WireReceiver te) {
		if (shorted)
			return 0;
		return numSources.computeIfAbsent(te, this::calcNumberSourcesPer);
	}

	public int getAverageCurrent(WireReceiver te) {
		if (shorted)
			return 0;
		return avgCurrents.computeIfAbsent(te, this::calcAvgCurrent);
	}

	public int getTerminalCurrent(WireReceiver te) {
		if (shorted)
			return 0;
		return terminalCurrents.computeIfAbsent(te, this::calcTerminalCurrent);
	}

	public int getTerminalVoltage(WireReceiver te) {
		if (shorted)
			return 0;
		return terminalVoltages.computeIfAbsent(te, this::getLowestVoltageOfPaths);
	}

	private int calcTerminalCurrent(WireReceiver te) {
		int a = 0;
		for (WirePath path : paths)
			if (path.endsAt(te.getPos()))
				a += path.getPathCurrent();
		return a;
	}

	private int calcAvgCurrent(WireReceiver te) {
		int a = 0;
		int c = 0;
		for (WirePath path : paths) {
			if (path.endsAt(te.getPos())) {
				a += path.getPathCurrent();
				c++;
			}
		}
		return c > 0 ? a/c : 0;
	}

	private float calcNumberSourcesPer(WireReceiver te) {
		float f = 0;
		for (WirePath path : paths)
			if (path.canConduct() && path.endsAt(te.getPos()))
				f += 1F/this.getNumberPathsStartingAt(path.start);
		return f;
	}

	public int getNumberPaths() {
		return paths.size();
	}

	private int getLowestVoltageOfPaths(WireReceiver te) {
		int v = Integer.MAX_VALUE;
		boolean someV = false;
		for (WirePath path : paths) {
			if (path.endsAt(te.getPos())) {
				int pv = path.getTerminalVoltage();
				if (pv != 0) {
					someV = true;
					if (pv < v)
						v = pv;
				}
			}
		}
		return someV ? v : 0;
	}

	public boolean hasNode(BlockPos pos) {
		return nodes.containsKey(pos);
	}

	@Nullable
	NetworkNode getNodeAt(WiringBlockEntity w) {
		return nodes.get(w.getBlockPos());
	}

	public void shortNetwork() {
		shorted = true;
		this.updateWires(false);
	}

	@Override
	public String toString() {
		return sources.size()+"SR/"+wires.size()+"W/"+sinks.size()+"SN-#"+paths+";{"+this.hashCode()+"}";
	}

	public void removeElement(NetworkTile te) {
		GlobalPos k = key(te);
		if (te instanceof WireEmitter)
			sources.remove(k);
		if (te instanceof WireReceiver)
			sinks.remove(k);
		if (te instanceof WiringBlockEntity)
			wires.remove(k);
		this.rebuild();
	}

	private void rebuild() {
		ArrayList<NetworkTile> li = this.allElements();
		this.clear(true);
		for (NetworkTile te : li)
			te.findAndJoinNetwork(te.getWorld(), te.getPos());
	}

	ArrayList<WirePath> getPathsStartingAt(WireEmitter start) {
		ArrayList<WirePath> li = new ArrayList<>();
		for (WirePath path : paths)
			if (path.startsAt(start.getPos()))
				li.add(path);
		return li;
	}

	ArrayList<WirePath> getPathsEndingAt(WireReceiver end) {
		ArrayList<WirePath> li = new ArrayList<>();
		for (WirePath path : paths)
			if (path.endsAt(end.getPos()))
				li.add(path);
		return li;
	}

	public int getNumberPathsStartingAt(WireEmitter start) {
		return this.getPathsStartingAt(start).size();
	}

	public PowerSourceList getInputSources(PowerSourceTracker io, ShaftMerger caller, @Nullable WireReceiver terminus) {
		PowerSourceList p = new PowerSourceList();
		if (terminus != null) {
			for (WirePath path : this.getPathsEndingAt(terminus))
				if (path.canConduct() && path.start instanceof PowerSourceTracker pst)
					p.addAll(pst.getPowerSources(io, caller));
		}
		else {
			for (WireEmitter w : sources.values())
				if (w instanceof PowerSourceTracker pst)
					p.addAll(pst.getPowerSources(io, caller));
		}
		return p;
	}

	public int getTotalCurrent() {
		int val = 0;
		for (WirePath p : paths)
			val += p.getPathCurrent();
		return val;
	}
}
