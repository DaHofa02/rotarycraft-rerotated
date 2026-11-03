/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.power;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.rotarycraft.api.power.PowerAcceptor;
import reika.rotarycraft.content.blockentity.ClutchBlockEntity;
import reika.rotarycraft.content.blockentity.FlywheelBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.IOMachineBlockEntity;
import reika.rotarycraft.content.blockentity.ShaftBlockEntity;
import reika.rotarycraft.content.blockentity.SplitterBlockEntity;
import reika.rotarycraft.content.blockentity.TransmissionMachineBlockEntity;

/**
 * Original: Reika.RotaryCraft.Auxiliary.TorqueUsage. Walks the drive train behind a flywheel and sums the torque its consumers need.
 * The branches for machines that are not ported yet (advanced gears, belts, power bus, power receivers) are added with those machines.
 */
public final class TorqueUsage {

	private static double requiredTorque;
	private static final Set<BlockPos> pathCache = new HashSet<>();

	private TorqueUsage() {}

	public static int getTorque(FlywheelBlockEntity te) {
		requiredTorque = 0;
		recursiveFind(te.getWriteTileEntity(), te, 1);
		if (requiredTorque < 0)
			requiredTorque = 0;
		pathCache.clear();
		return (int)Math.ceil(requiredTorque);
	}

	private static void recursiveFind(BlockEntity tile, FlywheelBlockEntity reader, double activeRatio) {
		if (tile == null)
			return;
		if (!pathCache.add(tile.getBlockPos().immutable()))
			return;

		if (tile instanceof TransmissionMachineBlockEntity io) {
			if (tile instanceof SplitterBlockEntity spl) {
				if (!spl.isSplitting()) { //merge mode
					BlockEntity write = spl.getWriteTileEntity();
					if (isPoweredFrom(write, spl)) {
						recursiveFind(write, reader, activeRatio);
					}
				}
				else {
					BlockEntity di = spl.getWriteTileEntity();
					BlockEntity di2 = spl.getWriteTileEntity2();
					if (isPoweredFrom(di, spl)) {
						recursiveFind(di, reader, activeRatio);
					}
					if (isPoweredFrom(di2, spl)) {
						recursiveFind(di2, reader, activeRatio);
					}
				}
			}
			else if (tile instanceof FlywheelBlockEntity fly) {
				requiredTorque += activeRatio*fly.getOppTorque(reader);
			}
			else if (tile instanceof ClutchBlockEntity clu) {
				if (clu.isOutputEnabled()) {
					if (isPoweredFrom(clu.getWriteTileEntity(), clu)) {
						recursiveFind(clu.getWriteTileEntity(), reader, activeRatio);
					}
				}
			}
			else if (tile instanceof GearboxBlockEntity gbx) {
				if (isPoweredFrom(gbx.getWriteTileEntity(), gbx)) {
					if (gbx.reduction) {
						recursiveFind(gbx.getWriteTileEntity(), reader, activeRatio/gbx.getRatio());
					}
					else {
						recursiveFind(gbx.getWriteTileEntity(), reader, activeRatio*gbx.getRatio());
					}
				}
			}
			else if (tile instanceof ShaftBlockEntity sha && sha.isCross()) {
				BlockEntity write = sha.getWriteTileEntity();
				BlockEntity write2 = sha.getWriteTileEntity2();
				if (isPoweredFrom(write, sha)) {
					recursiveFind(write, reader, activeRatio);
				}
				if (isPoweredFrom(write2, sha)) {
					recursiveFind(write2, reader, activeRatio);
				}
			}
			else {
				if (isPoweredFrom(io.getWriteTileEntity(), io)) {
					recursiveFind(io.getWriteTileEntity(), reader, activeRatio);
				}
			}
		}
		else if (tile instanceof PowerAcceptor acc) {
			requiredTorque += Math.max(acc.getMinTorque(reader.torque), 1);
		}
	}

	private static boolean isPoweredFrom(BlockEntity tile, IOMachineBlockEntity caller) {
		BlockPos loc = caller.getBlockPos();
		if (tile instanceof IOMachineBlockEntity io) {
			return isAt(io, io.getReadDirection(), loc) || isAt(io, io.getReadDirection2(), loc) || isAt(io, io.getReadDirection3(), loc) || isAt(io, io.getReadDirection4(), loc);
		}
		else if (tile instanceof PowerAcceptor acc) {
			if (acc.isReceiving()) {
				for (Direction dir : Direction.values()) {
					if (acc.canReadFrom(dir) && tile.getBlockPos().relative(dir).equals(loc))
						return true;
				}
			}
		}
		return false;
	}

	private static boolean isAt(IOMachineBlockEntity io, Direction dir, BlockPos loc) {
		return dir != null && io.getBlockPos().relative(dir).equals(loc);
	}
}
