/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft API). Adapted to Minecraft 1.21.1 types for a private port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.api.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.rotarycraft.api.IOMachine;
import reika.rotarycraft.api.interfaces.ComplexIO;

/** Original: Reika.RotaryCraft.API.Power.PowerTransferHelper (the part ported so far). */
public final class PowerTransferHelper {

	private PowerTransferHelper() {}

	/** Is the machine on that side of tile still writing power into it? (API ShaftPowerReceivers call this to notice a lost input.) */
	public static boolean checkPowerFrom(BlockEntity tile, Direction dir) {
		BlockEntity toCheck = tile.getLevel().getBlockEntity(tile.getBlockPos().relative(dir));
		BlockPos p = tile.getBlockPos();
		if (toCheck instanceof ComplexIO cio)
			return cio.getTorqueToSide(dir.getOpposite())*(long)cio.getSpeedToSide(dir.getOpposite()) != 0;
		if (toCheck instanceof IOMachine io)
			return p.equals(io.getWritePos()) || p.equals(io.getWritePos2());
		if (toCheck instanceof PowerGenerator pg)
			return p.equals(pg.getEmittingPos());
		return false;
	}

	/** Original checkPowerFromAllSides: any side (horizontal only unless vertical) still powering the tile. */
	public static boolean checkPowerFromAllSides(BlockEntity tile, boolean vertical) {
		for (Direction dir : Direction.values()) {
			if (!vertical && dir.getAxis() == Direction.Axis.Y)
				continue;
			if (checkPowerFrom(tile, dir))
				return true;
		}
		return false;
	}
}
