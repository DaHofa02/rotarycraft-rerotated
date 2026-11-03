/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.entity;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;

import reika.reactorcraft.radiation.RadiationEffects;
import reika.reactorcraft.radiation.RadiationIntensity;
import reika.reactorcraft.registry.ReactorEntities;

/**
 * Original: Reika.ReactorCraft.Entities.EntityRadiation - an invisible contamination cloud (only radiation goggles show it). Every tick it
 * gives radiation sickness to living things within its range and transforms one random block within range; it shrinks by one with a
 * 1/360000 chance per tick (and 1/36000 in rain where lightning can strike); an explosion scatters it.
 */
public class RadiationEntity extends Entity {

	private static final EntityDataAccessor<Integer> RANGE = SynchedEntityData.defineId(RadiationEntity.class, EntityDataSerializers.INT);

	private RadiationIntensity intensity = RadiationIntensity.LOWLEVEL;

	public RadiationEntity(EntityType<? extends RadiationEntity> type, Level world) {
		super(type, world);
		noPhysics = true;
	}

	public RadiationEntity(Level world, int range, RadiationIntensity ri) {
		this(ReactorEntities.RADIATION.get(), world);
		this.setRange(range);
		intensity = ri;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder b) {
		b.define(RANGE, 0);
	}

	public int getRange() {
		return entityData.get(RANGE);
	}

	private void setRange(int r) {
		entityData.set(RANGE, r);
	}

	public RadiationIntensity getIntensity() {
		return intensity;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide)
			return;
		this.applyRadiation();
		if (this.getRange() <= 0)
			this.discard();
		if (random.nextInt(360000) == 0)
			this.clean();
		if (level().isRaining() && random.nextInt(36000) == 0) {
			Biome b = level().getBiome(this.blockPosition()).value();
			if (b.getPrecipitationAt(this.blockPosition()) == Biome.Precipitation.RAIN)
				this.clean();
		}
	}

	private void applyRadiation() {
		int r = this.getRange();
		List<LivingEntity> inbox = level().getEntitiesOfClass(LivingEntity.class, new AABB(this.position(), this.position()).inflate(r));
		for (LivingEntity e : inbox)
			if (e.position().distanceTo(this.position()) <= r)
				RadiationEffects.applyEffects(e, intensity);
		BlockPos c = this.blockPosition();
		BlockPos p = c.offset(random.nextInt(2*r+1)-r, random.nextInt(2*r+1)-r, random.nextInt(2*r+1)-r);
		RadiationEffects.transformBlock(level(), p, intensity);
	}

	public void clean() {
		if (this.getRange() > 0)
			this.setRange(this.getRange()-1);
		else
			this.discard();
	}

	@Override
	public boolean hurt(DamageSource src, float amount) {
		if (src.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION) && !level().isClientSide) {
			RadiationEffects.contaminateArea(level(), this.blockPosition(), this.getRange(), 0.65F, 0.5, true, intensity);
			this.discard();
			return true;
		}
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag NBT) {
		this.setRange(NBT.getInt("effrange"));
		intensity = RadiationIntensity.radiationList[Math.min(NBT.getInt("intensity"), RadiationIntensity.radiationList.length-1)];
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag NBT) {
		NBT.putInt("effrange", this.getRange());
		NBT.putInt("intensity", intensity.ordinal());
	}
}
