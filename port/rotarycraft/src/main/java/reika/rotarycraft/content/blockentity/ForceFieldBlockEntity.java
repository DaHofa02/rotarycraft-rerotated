/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The force field, written for the port from the original's behaviour: a dome (at least 524288 W from below, range 2 + (P - 524288)/32768)
 * that keeps hostile mobs out (pushed outwards) and stops projectiles not shot by players (they are removed inside it).
 * Changes: the original's per-projectile reactions (reflected arrows, exploding ghast fireballs, split blaze fireballs, ...) are reduced to
 * removing them.
 */
public class ForceFieldBlockEntity extends DomeBlockEntity {

	public static final long MINPOWER_FIELD = 524288;
	public static final long FALLOFF = 32768;
	public int stopped;
	public int repelled;

	public ForceFieldBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.FORCE_FIELD.get(), pos, state, MINPOWER_FIELD, FALLOFF);
	}

	@Override
	protected void act(Level world, BlockPos pos) {
		Vec3 c = this.center();
		double r = this.getRange();
		for (Entity e : world.getEntities((Entity)null, this.box(), e -> e.position().distanceTo(c) <= r)) {
			if (e instanceof Projectile p && !(p.getOwner() instanceof Player)) {
				p.discard();
				stopped++;
			}
			else if (e instanceof Mob m && m instanceof Enemy) {
				Vec3 out = m.position().subtract(c);
				Vec3 dir = out.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : out.normalize();
				m.setDeltaMovement(dir.x*0.6, Math.max(0.2, dir.y*0.6), dir.z*0.6);
				m.hurtMarked = true;
				repelled++;
			}
		}
	}
}
