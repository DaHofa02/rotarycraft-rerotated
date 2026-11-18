/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The laser gun, written for the port from the original's behaviour: needs 8388608 W from below and no ammunition, reaches 256 blocks, and
 * every 4 ticks burns the target: 4 damage and 10 seconds on fire.
 * Changes: the beam does not burn or melt blocks on its way.
 */
public class LaserGunBlockEntity extends TurretBlockEntity {

	public LaserGunBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.LASER_GUN.get(), pos, state, 1, 1, 8388608);
	}

	@Override
	protected int getAmmoSlots() {
		return 0;
	}

	@Override
	public int getRange() {
		return 256;
	}

	@Override
	public int getOperationTime() {
		return 4;
	}

	@Override
	public boolean isAmmo(ItemStack is) {
		return false;
	}

	@Override
	protected boolean canFire() {
		return true;
	}

	@Override
	protected void fire(Level world, Vec3 muzzle, LivingEntity target) {
		target.igniteForSeconds(10);
		target.hurt(world.damageSources().inFire(), 4F);
	}
}
