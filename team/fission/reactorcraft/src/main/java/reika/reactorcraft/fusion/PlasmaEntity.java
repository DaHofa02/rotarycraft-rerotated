/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.fusion;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import reika.reactorcraft.registry.ReactorEntities;
import reika.rotarycraft.lib.world.TemperatureEnvironment;

/**
 * Original: Reika.ReactorCraft.Entities.EntityPlasma (a DragonAPI ParticleEntity) - a ball of fusion plasma flying at 0.75 blocks per
 * tick, flat, at the height it was made. Toroid magnets steer it from one to the next; when no magnet has caught it for 6 ticks it
 * has "escaped" (12: badly). It sets flammable blocks it enters alight and kills whatever it touches (4 damage with fire
 * resistance). Now and then (1 in 12 ticks, 1 in 48 when escaped, never when badly escaped) it checks for 15-20 plasma balls within
 * a block of it: then it fuses (a fusion flash with 3 neutrons) and is gone. It dies when it stops, 100 blocks from where it was
 * made, or after 300 ticks badly escaped.
 */
public class PlasmaEntity extends Entity {

	private int targetX;
	private int targetZ;

	public int magnetOrdinal = -1;

	private int escapeTicks = 0;

	private BlockPos spawn;
	private BlockPos oldBlock;
	private String placerOfInjector = "";

	public PlasmaEntity(EntityType<? extends PlasmaEntity> type, Level world) {
		super(type, world);
		noPhysics = true;
	}

	public PlasmaEntity(Level world, BlockPos pos, String placer) {
		this(ReactorEntities.PLASMA.get(), world);
		spawn = pos;
		oldBlock = pos;
		placerOfInjector = placer == null ? "" : placer;
		this.moveTo(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 0, 0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder b) {}

	public double getSpeed() {
		return 0.75;
	}

	public void setTarget(int x, int z) {
		targetX = x;
		targetZ = z;
		double dx = targetX+0.5-this.getX();
		double dz = targetZ+0.5-this.getZ();
		double dd = Math.sqrt(dx*dx+dz*dz);
		double v = this.getSpeed();
		this.setDeltaMovement(dx*v/dd, 0, dz*v/dd);
		hasImpulse = true;
	}

	private void checkFusion() {
		List<PlasmaEntity> li = level().getEntitiesOfClass(PlasmaEntity.class, new AABB(this.position(), this.position()).inflate(1));
		if (li.size() >= this.getFusionThreshold() && !li.get(0).hasEscaped() && !li.get(li.size()-1).hasEscaped()) {
			level().addFreshEntity(new FusionEntity(level(), this.getX(), this.getY(), this.getZ(), placerOfInjector));
			this.discard();
		}
	}

	public int getFusionThreshold() {
		return 15+random.nextInt(6);
	}

	@Override
	public void tick() {
		super.tick();
		Vec3 m = this.getDeltaMovement();
		this.setPos(this.getX()+m.x, this.getY()+m.y, this.getZ()+m.z);
		if (level().isClientSide)
			return;
		if (m.lengthSqr() == 0 && tickCount > 20) {
			this.discard();
			return;
		}
		if (this.getY() > level().getMaxBuildHeight() || this.getY() < level().getMinBuildHeight()) {
			this.discard();
			return;
		}
		if (spawn != null && Math.sqrt(spawn.getCenter().distanceToSqr(this.position())) >= 100) {
			this.discard();
			return;
		}
		BlockPos p = this.blockPosition();
		if (oldBlock == null)
			oldBlock = p;
		if (!p.equals(oldBlock)) {
			oldBlock = p;
			if (spawn == null || !p.equals(spawn))
				this.onEnterBlock(level(), p);
		}
		for (Entity e : level().getEntities(this, new AABB(this.position(), this.position()).inflate(this.getHitboxSize())))
			this.collide(e);
		this.onTick();
	}

	private void onEnterBlock(Level world, BlockPos p) {
		if (TemperatureEnvironment.flammable(world, p))
			TemperatureEnvironment.ignite(world, p);
	}

	private void collide(Entity e) {
		if (e instanceof PlasmaEntity || e instanceof FusionEntity || e instanceof reika.reactorcraft.content.entity.NeutronEntity)
			return;
		float dmg = e instanceof LivingEntity le && le.hasEffect(MobEffects.FIRE_RESISTANCE) ? 4 : Integer.MAX_VALUE;
		e.hurt(e.damageSources().source(FusionDamage.FUSION), dmg);
	}

	private void onTick() {
		if (!this.hasEscapedSeverely() && random.nextInt(this.hasEscaped() ? 48 : 12) == 0)
			this.checkFusion();
		if (this.isRemoved())
			return;
		Vec3 m = this.getDeltaMovement();
		this.setDeltaMovement(m.x, 0, m.z);
		if (spawn != null)
			this.setPos(this.getX(), spawn.getY()+0.5, this.getZ());
		if (tickCount > 300 && this.hasEscapedSeverely())
			this.discard();
		escapeTicks++;
	}

	public double getHitboxSize() {
		return 0.5;
	}

	public void resetEscapeTimer() {
		escapeTicks = 0;
	}

	public boolean hasEscaped() {
		return escapeTicks >= 6; //was 4
	}

	public boolean hasEscapedSeverely() {
		return escapeTicks >= 12; //was 8
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
	public boolean isInvulnerable() {
		return true;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double d) {
		return true;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag NBT) {
		if (NBT.contains("spawn"))
			spawn = BlockPos.of(NBT.getLong("spawn"));
		magnetOrdinal = NBT.contains("magnet") ? NBT.getInt("magnet") : -1;
		placerOfInjector = NBT.getString("placer");
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag NBT) {
		if (spawn != null)
			NBT.putLong("spawn", spawn.asLong());
		NBT.putInt("magnet", magnetOrdinal);
		NBT.putString("placer", placerOfInjector);
	}
}
