/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.entity;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import reika.reactorcraft.ReactorConfig;
import reika.reactorcraft.content.block.FluoriteBlock;
import reika.reactorcraft.radiation.RadiationEffects;
import reika.reactorcraft.radiation.RadiationIntensity;
import reika.reactorcraft.radiation.RadiationShield;
import reika.reactorcraft.reactor.NeutronTile;
import reika.reactorcraft.registry.ReactorEntities;
import reika.rotarycraft.lib.math.RandomHelper;

/**
 * Original: Reika.ReactorCraft.Entities.EntityNeutron (a DragonAPI ParticleEntity) - flies straight at 0.75 blocks per tick through
 * everything; each new block it enters may absorb it: neutron block entities (fuel cores, coolant cells, ...), radiation shields by their
 * absorption chance, otherwise by the neutron speed's irradiation chance depending on the block (opaque and blast resistant blocks
 * catch it more often; then a chance to contaminate and to irradiate the block). Fluorite it passes lights up. It harms living things it
 * touches (1/8 chance: one second of radiation, then it is gone). After 6 seconds it is gone (original despawn chance); out of the world it dies.
 */
public class NeutronEntity extends Entity {

	public enum NeutronType {
		NULL, DECAY, FISSION, BREEDER, FUSION, WASTE, THORIUM;

		public boolean isFissionType() {
			return this == DECAY || this == FISSION || this == BREEDER || this == THORIUM;
		}

		public boolean canTriggerFission(RandomSource r) {
			return this.isFissionType() || (this == WASTE && r.nextInt(100) < 40);
		}

		public NeutronSpeed getCreationSpeed() {
			if (!ReactorConfig.FASTNEUTRONS.get())
				return NeutronSpeed.THERMAL;
			return this == BREEDER || this == FISSION || this == THORIUM ? NeutronSpeed.FAST : NeutronSpeed.THERMAL;
		}

		public boolean canTriggerFuelConversion() {
			return this == BREEDER;
		}

		public int getSodiumBoilerAbsorptionChance() {
			return this != BREEDER && this != DECAY ? 90 : 0;
		}

		public boolean canIrradiateMaterials() {
			return this == FISSION || this == FUSION || this == BREEDER || this == THORIUM;
		}

		public int getBoilerAbsorptionChance() {
			return this == BREEDER || this == THORIUM ? 80 : 0;
		}
	}

	public enum NeutronSpeed {
		THERMAL, FAST;

		public float getInteractionMultiplier() {
			return this == FAST ? 0.6F : 1;
		}

		public double getIrradiatedAbsorptionChance() {
			return this == FAST ? 40 : 100;
		}

		public float getWasteConversionMultiplier() {
			return this == FAST ? 2.2F : 1;
		}
	}

	private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(NeutronEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SPEED = SynchedEntityData.defineId(NeutronEntity.class, EntityDataSerializers.INT);
	public static final double SPEED_BLOCKS = 0.75;

	private BlockPos spawn;
	private BlockPos oldBlock;

	public NeutronEntity(EntityType<? extends NeutronEntity> type, Level world) {
		super(type, world);
		noPhysics = true;
	}

	public NeutronEntity(Level world, BlockPos pos, Direction dir, NeutronType type) {
		this(ReactorEntities.NEUTRON.get(), world);
		spawn = pos;
		oldBlock = pos;
		this.moveTo(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 0, 0);
		this.setDeltaMovement(dir.getStepX()*SPEED_BLOCKS, dir.getStepY()*SPEED_BLOCKS, dir.getStepZ()*SPEED_BLOCKS);
		entityData.set(TYPE, type.ordinal());
		entityData.set(SPEED, type.getCreationSpeed().ordinal());
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder b) {
		b.define(TYPE, 0);
		b.define(SPEED, 0);
	}

	public NeutronType getNeutronType() {
		return NeutronType.values()[Math.min(entityData.get(TYPE), NeutronType.values().length-1)];
	}

	public NeutronSpeed getNeutronSpeed() {
		return NeutronSpeed.values()[Math.min(entityData.get(SPEED), 1)];
	}

	/** Heavy water slows neutrons down. */
	public void moderate() {
		entityData.set(SPEED, NeutronSpeed.THERMAL.ordinal());
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
		if (this.getY() < level().getMinBuildHeight() || this.getY() > level().getMaxBuildHeight()) {
			this.discard();
			return;
		}
		if (tickCount > 120 && RandomHelper.doWithChance(tickCount-120, random)) { //original: 1 counts as 100 %, so at most 121 ticks
			this.discard();
			return;
		}
		BlockPos p = this.blockPosition();
		if (oldBlock == null)
			oldBlock = p;
		if (!p.equals(oldBlock)) {
			oldBlock = p;
			if (spawn == null || !p.equals(spawn)) {
				if (this.onEnterBlock(level(), p)) {
					this.discard();
					return;
				}
			}
		}
		List<LivingEntity> inbox = level().getEntitiesOfClass(LivingEntity.class, new AABB(this.position(), this.position()).inflate(0.1));
		for (LivingEntity e : inbox) {
			if (random.nextInt(8) == 0) {
				RadiationEffects.applyPulseEffects(e, RadiationIntensity.MODERATE);
				this.discard();
				return;
			}
		}
	}

	/** Original onEnterBlock: true = absorbed. */
	private boolean onEnterBlock(Level world, BlockPos p) {
		BlockState s = world.getBlockState(p);
		if (s.isAir())
			return random.nextInt(1000) == 0;
		BlockEntity te = world.getBlockEntity(p);
		if (te instanceof NeutronTile nt && nt.onNeutron(this, world, p))
			return true;
		if (s.getBlock() instanceof FluoriteBlock && !s.getValue(FluoriteBlock.ACTIVATED))
			world.setBlockAndUpdate(p, s.setValue(FluoriteBlock.ACTIVATED, true));
		RadiationShield rs = RadiationShield.getFrom(s);
		if (rs != null && RandomHelper.doWithChance(rs.neutronAbsorbChance, random))
			return true;
		if (RandomHelper.doWithChance(this.getNeutronSpeed().getIrradiatedAbsorptionChance(), random)) {
			float res = s.getBlock().getExplosionResistance();
			boolean flag;
			if (s.canOcclude())
				flag = (random.nextBoolean() && res >= 12) || safeRandom(24-res) == 0;
			else if (s.getLightBlock(world, p) >= 15)
				flag = safeRandom(s.getLightBlock(world, p)) > 0;
			else
				flag = random.nextInt(1000) == 0;
			if (flag) {
				this.spawnRadiationChance(world, p);
				if (random.nextInt(5) == 0)
					RadiationEffects.transformBlock(world, p, RadiationIntensity.MODERATE);
				return true;
			}
		}
		return false;
	}

	private int safeRandom(float bound) {
		int b = (int)bound;
		return b > 0 ? random.nextInt(b) : 0;
	}

	private void spawnRadiationChance(Level world, BlockPos p) {
		if (random.nextInt(50) == 0) {
			if (world.getEntitiesOfClass(RadiationEntity.class, new AABB(p).inflate(8)).size() < 3)
				RadiationEffects.contaminateArea(world, p, 1, 1, 0, false, RadiationIntensity.LOWLEVEL);
		}
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
	public boolean shouldRenderAtSqrDistance(double d) {
		return d <= 4096;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag NBT) {
		entityData.set(TYPE, NBT.getInt("ntype"));
		entityData.set(SPEED, NBT.getInt("nspeed"));
		if (NBT.contains("spawn"))
			spawn = BlockPos.of(NBT.getLong("spawn"));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag NBT) {
		NBT.putInt("ntype", entityData.get(TYPE));
		NBT.putInt("nspeed", entityData.get(SPEED));
		if (spawn != null)
			NBT.putLong("spawn", spawn.asLong());
	}
}
