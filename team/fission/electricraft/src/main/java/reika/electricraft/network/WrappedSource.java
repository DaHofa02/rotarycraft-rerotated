/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.network;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.rotarycraft.api.power.ShaftMerger;
import reika.rotarycraft.content.power.PowerSourceList;
import reika.rotarycraft.content.power.PowerSourceTracker;

/**
 * Original: Reika.ElectriCraft.Auxiliary.WrappedSource - presents a {@link WrappableWireSource} to the network as an emitter: voltage
 * omega*8, current torque/8 (TORQUE_PER_AMP), live while the source is functional. It never joins or leaves networks by itself; the
 * wires next to it add it again whenever they (re)join one.
 */
public final class WrappedSource implements WireEmitter, PowerSourceTracker {

	private final WrappableWireSource source;

	public WrappedSource(WrappableWireSource src) {
		if (!(src instanceof BlockEntity))
			throw new IllegalArgumentException("You cannot wrap non-tile sources!");
		source = src;
	}

	@Override
	public void findAndJoinNetwork(Level world, BlockPos pos) {}

	@Override
	public WireNetwork getNetwork() {
		return null;
	}

	@Override
	public void setNetwork(WireNetwork n) {}

	@Override
	public void removeFromNetwork() {}

	@Override
	public void resetNetwork() {}

	@Override
	public void onNetworkChanged() {}

	@Override
	public boolean isConnectable() {
		return this.canEmitPower();
	}

	@Override
	public boolean canNetworkOnSide(Direction dir) {
		return this.canEmitPowerToSide(dir);
	}

	@Override
	public Level getWorld() {
		return ((BlockEntity)source).getLevel();
	}

	@Override
	public BlockPos getPos() {
		return ((BlockEntity)source).getBlockPos();
	}

	@Override
	public BlockPos getIoOffset() {
		return source.getIoOffset();
	}

	@Override
	public int getGenVoltage() {
		return source.getOmega()*WireNetwork.TORQUE_PER_AMP;
	}

	@Override
	public int getGenCurrent() {
		return source.getTorque()/WireNetwork.TORQUE_PER_AMP;
	}

	@Override
	public boolean canEmitPowerToSide(Direction dir) {
		return source.canConnectToSide(dir);
	}

	@Override
	public boolean canEmitPower() {
		return source.isFunctional();
	}

	public boolean needsUpdate() {
		return source.hasPowerStatusChangedSinceLastTick();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof WrappedSource w && w.source.equals(source);
	}

	@Override
	public int hashCode() {
		return source.hashCode();
	}

	@Override
	public PowerSourceList getPowerSources(PowerSourceTracker io, ShaftMerger caller) {
		return source.getPowerSources(io, caller);
	}

	@Override
	public void getAllOutputs(Collection<BlockEntity> c, Direction dir) {
		source.getAllOutputs(c, dir);
	}
}
