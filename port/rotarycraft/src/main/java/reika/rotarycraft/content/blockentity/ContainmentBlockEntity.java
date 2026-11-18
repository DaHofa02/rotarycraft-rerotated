/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The containment field, written for the port from the original's behaviour: a dome (at least 131072 W from below, range
 * 2 + (P - 131072)/8192) that keeps hostile mobs inside: one that gets within half a block of the edge is pulled back to the middle. The
 * wither needs 524288 W and the ender dragon 2097152 W to be held (then from 2 blocks before the edge).
 */
public class ContainmentBlockEntity extends DomeBlockEntity {

	public static final long MINPOWER_FIELD = 131072;
	public static final long FALLOFF = 8192;
	public static final long WITHERPOWER = 524288;
	public static final long DRAGONPOWER = 2097152;
	public int pulled;

	public ContainmentBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.CONTAINMENT.get(), pos, state, MINPOWER_FIELD, FALLOFF);
	}

	public boolean canHold(LivingEntity e) {
		if (e instanceof EnderDragon)
			return power >= DRAGONPOWER;
		if (e instanceof WitherBoss)
			return power >= WITHERPOWER;
		return e instanceof Enemy;
	}

	@Override
	protected void act(Level world, BlockPos pos) {
		Vec3 c = this.center();
		double r = this.getRange();
		for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, this.box().inflate(2), this::canHold)) {
			Vec3 d = e.position().subtract(c);
			double dist = d.length();
			double edge = e instanceof EnderDragon || e instanceof WitherBoss ? r-2 : r-0.5;
			if (dist > edge && dist < r+2) {
				Vec3 back = d.scale(-0.5/Math.max(dist, 1e-3));
				e.setDeltaMovement(back);
				e.hurtMarked = true;
				pulled++;
			}
		}
	}
}
