/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import reika.electricraft.ElectriCraft;
import reika.electricraft.content.blockentity.RFCableBlockEntity;

/**
 * Original: Reika.ElectriCraft.Network.RF.RFNetwork, with NeoForge FE (IEnergyStorage, the successor of RF) in place of the CoFH RF API.
 * Every tick it collects from all connected providers up to min(network limit, what it could deliver), then hands its buffer out to
 * the receivers in even shares. The limit (set in the cable GUI, 0 = nothing moves) is shared by all cables of the network.
 */
public class RFNetwork {

	private final Collection<RFCableBlockEntity> cables = new ArrayList<>();
	private final Map<GlobalPos, Endpoint> endpoints = new HashMap<>();
	private int energy;
	private int networkLimit;
	private boolean disabled;
	private int tick;

	public RFNetwork() {
		ElectriNetworkManager.addRFNetwork(this);
	}

	public void setIOLimit(int limit) {
		if (networkLimit != limit) {
			networkLimit = limit;
			for (RFCableBlockEntity cable : cables)
				if (cable.getRFLimit() != networkLimit)
					cable.setRFLimit(networkLimit);
		}
	}

	public int getIOLimit() {
		return networkLimit;
	}

	public int getBufferedEnergy() {
		return energy;
	}

	void tick() {
		tick++;
		if (tick%1000 == 0)
			this.checkValidity();
		if (disabled || cables.isEmpty() || endpoints.isEmpty())
			return;
		ArrayList<Endpoint> collectibles = new ArrayList<>();
		ArrayList<Endpoint> insertibles = new ArrayList<>();
		long maxCanPush = energy;
		Iterator<Endpoint> it = endpoints.values().iterator();
		while (it.hasNext()) {
			Endpoint ei = it.next();
			if (ei.valid()) {
				int ins = ei.getTotalInsertible();
				maxCanPush += ins;
				if (ei.getTotalCollectible() > 0)
					collectibles.add(ei);
				if (ins > 0)
					insertibles.add(ei);
			}
			else {
				it.remove();
			}
		}
		int max = (int)Math.min(this.getIOLimit(), maxCanPush);
		for (int i = 0; i < collectibles.size() && energy < max; i++)
			energy += collectibles.get(i).collectEnergy(max-energy);
		for (int i = 0; i < insertibles.size() && energy > 0; i++) {
			int add = Math.min(energy, 1+energy/insertibles.size());
			energy -= insertibles.get(i).addEnergy(add);
		}
	}

	private void checkValidity() {
		cables.removeIf(RFCableBlockEntity::isRemoved);
		if (cables.isEmpty())
			this.clear(false);
	}

	public void addElement(RFCableBlockEntity te) {
		if (!cables.contains(te)) {
			cables.add(te);
			if (te.getRFLimit() > 0 && te.getRFLimit() != networkLimit)
				this.setIOLimit(Math.min(te.getRFLimit(), networkLimit));
		}
	}

	public void removeElement(RFCableBlockEntity te) {
		cables.remove(te);
		this.rebuild();
	}

	private void rebuild() {
		ElectriCraft.LOGGER.debug("Remapping RF network {}", this);
		ArrayList<RFCableBlockEntity> li = new ArrayList<>(cables);
		this.clear(true);
		for (RFCableBlockEntity te : li)
			if (!te.isRemoved() && te.getLevel() != null)
				te.findAndJoinNetwork(te.getLevel(), te.getBlockPos());
	}

	/** The block at pos accepts energy through its side 'side' (the face towards the cable). */
	public void addConnection(Level world, BlockPos pos, Direction side) {
		if (world.getBlockEntity(pos) instanceof RFCableBlockEntity)
			return;
		endpoints.computeIfAbsent(GlobalPos.of(world.dimension(), pos), k -> new Endpoint(world, pos)).sides.add(side);
	}

	public void merge(RFNetwork n) {
		if (n == this)
			return;
		ArrayList<RFCableBlockEntity> li = new ArrayList<>(n.cables);
		for (var e : n.endpoints.entrySet()) {
			Endpoint has = endpoints.get(e.getKey());
			if (has == null)
				endpoints.put(e.getKey(), e.getValue());
			else
				has.sides.addAll(e.getValue().sides);
		}
		int lim = n.getIOLimit();
		n.clear(false);
		for (RFCableBlockEntity wire : li)
			wire.setNetwork(this);
		if (lim != 0 && lim != networkLimit)
			this.setIOLimit(Math.min(lim, networkLimit));
	}

	private void clear(boolean clearTiles) {
		if (clearTiles)
			for (RFCableBlockEntity cable : cables)
				cable.resetNetwork();
		cables.clear();
		endpoints.clear();
		energy = 0;
		disabled = true;
		ElectriNetworkManager.scheduleRFNetworkDiscard(this);
	}

	@Override
	public String toString() {
		return cables.size()+": "+endpoints.keySet();
	}

	public int drainEnergy(int maxReceive, boolean simulate) {
		maxReceive = Math.min(maxReceive, this.getIOLimit());
		int drain = Math.min(maxReceive, energy);
		if (!simulate)
			energy -= drain;
		return drain;
	}

	public int addEnergy(int maxAdd, boolean simulate) {
		if (energy >= this.getIOLimit())
			return 0;
		maxAdd = Math.min(this.getIOLimit(), maxAdd);
		if (!simulate)
			energy += maxAdd;
		return maxAdd;
	}

	/** Original EnergyInteraction: one neighbouring energy block and the sides of it the network touches. */
	private static final class Endpoint {
		private final Level world;
		private final BlockPos pos;
		private final Set<Direction> sides = EnumSet.noneOf(Direction.class);

		Endpoint(Level world, BlockPos pos) {
			this.world = world;
			this.pos = pos;
		}

		@Nullable
		private IEnergyStorage get(Direction side) {
			if (!world.isLoaded(pos))
				return null;
			return world.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
		}

		boolean valid() {
			if (!world.isLoaded(pos))
				return false;
			if (world.getBlockEntity(pos) != null)
				return true;
			//port: the wireless pads (the original's TileEntityWirelessCharger) are blocks without a block entity here; they stay end points
			//while they still offer energy on a side the network touches
			for (Direction d : sides)
				if (this.get(d) != null)
					return true;
			return false;
		}

		int collectEnergy(int max) {
			int total = 0;
			for (Direction d : sides) {
				IEnergyStorage s = this.get(d);
				if (s != null && s.canExtract())
					total += s.extractEnergy(max-total, false);
			}
			return total;
		}

		int addEnergy(int max) {
			int total = 0;
			for (Direction d : sides) {
				IEnergyStorage s = this.get(d);
				if (s != null && s.canReceive())
					total += s.receiveEnergy(max-total, false);
			}
			return total;
		}

		int getTotalCollectible() {
			long total = 0;
			for (Direction d : sides) {
				IEnergyStorage s = this.get(d);
				if (s != null && s.canExtract())
					total += s.extractEnergy(Integer.MAX_VALUE, true);
			}
			return (int)Math.min(Integer.MAX_VALUE, total);
		}

		int getTotalInsertible() {
			long total = 0;
			for (Direction d : sides) {
				IEnergyStorage s = this.get(d);
				if (s != null && s.canReceive())
					total += s.receiveEnergy(Integer.MAX_VALUE, true);
			}
			return (int)Math.min(Integer.MAX_VALUE, total);
		}
	}
}
