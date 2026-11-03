/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.entity;

import java.util.List;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import reika.reactorcraft.radiation.RadiationEffects;
import reika.reactorcraft.radiation.RadiationIntensity;
import reika.reactorcraft.registry.ReactorEntities;

/**
 * Original: Reika.ReactorCraft.Entities.EntityNuclearWaste - dropped nuclear waste never despawns and cannot be destroyed; it gives high
 * level radiation sickness within 6 blocks, and after 3 minutes, then every 10 minutes, contaminates the area (radius 24) unless 32
 * clouds are already there. Below the world it bounces back up.
 */
public class NuclearWasteEntity extends ItemEntity {

	public static final int RANGE = 6;
	public static final int RADIATION_INTERVAL = 10*60*20;
	public static final int RADIATION_DELAY = 3*60*20;

	private int timer;

	public NuclearWasteEntity(EntityType<? extends NuclearWasteEntity> type, Level world) {
		super(type, world);
	}

	public NuclearWasteEntity(Level world, double x, double y, double z, ItemStack is) {
		this(ReactorEntities.NUCLEAR_WASTE.get(), world);
		this.setPos(x, y, z);
		this.setItem(is);
	}

	@Override
	public void tick() {
		super.tick();
		this.setUnlimitedLifetime();
		if (!level().isClientSide) {
			this.applyRadiation();
			if (this.getY() < level().getMinBuildHeight()) {
				this.setDeltaMovement(this.getDeltaMovement().x, Math.abs(this.getDeltaMovement().y), this.getDeltaMovement().z);
				this.setPos(this.getX(), Math.max(this.getY(), level().getMinBuildHeight()), this.getZ());
				if (timer%256 == 0 && level().getEntitiesOfClass(RadiationEntity.class, this.getBoundingBox().inflate(RANGE)).size() < 100)
					RadiationEffects.contaminateArea(level(), this.blockPosition(), RANGE*4, 2, 0, false, RadiationIntensity.HIGHLEVEL);
			}
		}
		timer++;
	}

	private void applyRadiation() {
		List<LivingEntity> inbox = level().getEntitiesOfClass(LivingEntity.class, new AABB(this.position(), this.position()).inflate(RANGE));
		for (LivingEntity e : inbox)
			if (e.position().distanceTo(this.position()) <= RANGE)
				RadiationEffects.applyEffects(e, RadiationIntensity.HIGHLEVEL);
		if (timer >= RADIATION_DELAY && (timer-RADIATION_DELAY)%RADIATION_INTERVAL == 0) {
			List<RadiationEntity> near = level().getEntitiesOfClass(RadiationEntity.class, new AABB(this.position(), this.position()).inflate(RANGE));
			if (near.size() < 32)
				RadiationEffects.contaminateArea(level(), this.blockPosition(), RANGE*4, 2, 0, false, RadiationIntensity.HIGHLEVEL);
		}
	}

	@Override
	public boolean hurt(DamageSource src, float dmg) {
		return false;
	}

	@Override
	public boolean fireImmune() {
		return true;
	}
}
