/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import reika.reactorcraft.radiation.RadiationShield;
import reika.rotarycraft.lib.math.RandomHelper;

/**
 * Original: Reika.ReactorCraft.Auxiliary.HydrogenExplosion - before a normal explosion, every solid, plain block within the radius
 * (shielding blocks only with their deflection chance) is thrown into the air as a falling block, then the explosion.
 */
public final class HydrogenExplosion {

	private HydrogenExplosion() {}

	public static void explode(ServerLevel world, BlockPos c, float power) {
		int r = (int)power+2;
		List<FallingBlockEntity> li = new ArrayList<>();
		for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -r, -r), c.offset(r, r, r))) {
			BlockState s = world.getBlockState(p);
			RadiationShield rs = RadiationShield.getFrom(s);
			if (rs != null && !RandomHelper.doWithChance(rs.radiationDeflectChance, world.random))
				continue;
			if (canEntitize(world, p.immutable(), s, c, power)) {
				FallingBlockEntity e = FallingBlockEntity.fall(world, p.immutable(), s);
				e.disableDrop();
				li.add(e);
			}
		}
		world.explode(null, c.getX()+0.5, c.getY()+0.5, c.getZ()+0.5, power, Level.ExplosionInteraction.BLOCK);
		for (FallingBlockEntity e : li) {
			double dx = e.getX()-(c.getX()+0.5), dy = e.getY()-(c.getY()+0.5), dz = e.getZ()-(c.getZ()+0.5);
			double dd = 0.3, vy = 1.5;
			e.setDeltaMovement(dx*dd*world.random.nextDouble(), dy*dd*world.random.nextDouble()+vy, dz*dd*world.random.nextDouble());
			e.hurtMarked = true;
		}
	}

	private static boolean canEntitize(ServerLevel world, BlockPos p, BlockState s, BlockPos c, float power) {
		if (s.isAir() || s.is(Blocks.BEDROCK) || s.hasBlockEntity() || s.canBeReplaced())
			return false;
		if (s.getRenderShape() != RenderShape.MODEL || !s.isCollisionShapeFullBlock(world, p))
			return false;
		return Math.sqrt(p.distToCenterSqr(c.getX()+0.5, c.getY()+0.5, c.getZ()+0.5)) <= power+0.5;
	}
}
