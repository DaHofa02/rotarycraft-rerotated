/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The railgun, written for the port from the original's behaviour: needs 4194304 W from below, reaches 164 blocks, one shot a second. Each
 * shot uses one slug: an iron ingot (10 damage), gold ingot (20), diamond (40) or netherite ingot (80); the target is also thrown back.
 * Changes: own ammunition (the original had its own slug items) and no block damage along the shot.
 */
public class RailgunBlockEntity extends TurretBlockEntity {

	public RailgunBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.RAILGUN.get(), pos, state, 1, 1, 4194304);
	}

	public static float damageOf(ItemStack is) {
		return is.is(Items.IRON_INGOT) ? 10 : is.is(Items.GOLD_INGOT) ? 20 : is.is(Items.DIAMOND) ? 40 : is.is(Items.NETHERITE_INGOT) ? 80 : 0;
	}

	@Override
	public int getRange() {
		return 164;
	}

	@Override
	public int getOperationTime() {
		return 20;
	}

	@Override
	public boolean isAmmo(ItemStack is) {
		return damageOf(is) > 0;
	}

	@Override
	protected boolean canFire() {
		return this.findAmmo() >= 0;
	}

	@Override
	protected void fire(Level world, Vec3 muzzle, LivingEntity target) {
		int slot = this.findAmmo();
		float dmg = damageOf(inv.getStackInSlot(slot));
		inv.extractItem(slot, 1, false);
		target.hurt(world.damageSources().magic(), dmg);
		Vec3 push = target.position().subtract(muzzle).normalize().scale(dmg/20);
		target.push(push.x, 0.2, push.z);
		target.hurtMarked = true;
	}
}
