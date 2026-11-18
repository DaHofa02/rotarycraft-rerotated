/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/**
 * The anti-aircraft gun, written for the port from the original's behaviour: needs 65536 W at 1024 Nm or more from below, reaches 128 blocks
 * and only aims at flying things (ghasts, phantoms, bats, blazes, vexes, the wither and the dragon, and players gliding or flying). Every 6
 * ticks a flak burst of scrap metal: 6 damage to everything hostile within 3 blocks of the target.
 */
public class AntiAirGunBlockEntity extends TurretBlockEntity {

	public AntiAirGunBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.ANTI_AIR_GUN.get(), pos, state, 1024, 1, 65536);
	}

	@Override
	public int getRange() {
		return 128;
	}

	@Override
	public int getOperationTime() {
		return 6;
	}

	@Override
	public boolean isAmmo(ItemStack is) {
		return is.is(RotaryItems.part(CraftingPart.SCRAP));
	}

	public static boolean isFlying(LivingEntity e) {
		if (e instanceof Player p)
			return p.isFallFlying() || p.getAbilities().flying;
		return e instanceof FlyingMob || e instanceof Bat || e instanceof Blaze || e instanceof Vex || e instanceof WitherBoss || e instanceof EnderDragon;
	}

	@Override
	public boolean isValidTarget(LivingEntity e) {
		return isFlying(e) && (e instanceof Bat || super.isValidTarget(e));
	}

	@Override
	protected boolean canFire() {
		return this.findAmmo() >= 0;
	}

	@Override
	protected void fire(Level world, Vec3 muzzle, LivingEntity target) {
		inv.extractItem(this.findAmmo(), 1, false);
		for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, new AABB(target.blockPosition()).inflate(3), this::isValidTarget))
			e.hurt(world.damageSources().explosion(null, null), 6F);
	}
}
