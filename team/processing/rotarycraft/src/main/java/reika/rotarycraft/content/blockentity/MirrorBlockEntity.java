/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import reika.rotarycraft.content.solar.SolarPlant;
import reika.rotarycraft.content.solar.SolarPlantBlock;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Auxiliary.TileEntityMirror - a heliostat of a solar plant. It works while it is not broken,
 * no RotaryCraft machine sits on it, the sky is open above it and it is the highest mirror of its column. The client turns it between
 * the sun and the top of its tower (the original's aiming formulas, 0.5 degrees per tick). Something solid landing on it (falling
 * faster than 0.1 blocks per tick with mass - 20 * speed > 80, the original's mass table) breaks it: the glass shatters and the
 * entity takes 1 damage. A broken mirror is repaired with a mirror item and drops its parts when mined.
 */
public class MirrorBlockEntity extends RotaryCraftBlockEntity implements SolarPlantBlock {

	public float theta;
	private float targetTheta;
	private float targetPhi;

	public boolean broken;
	private boolean rotatingLarge;

	private float aimFactor = 1;

	@Nullable
	private SolarPlant plant;

	public MirrorBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.MIRROR.get(), pos, state);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	@Override
	public void searchForPlant(Level world, BlockPos pos) {
		if (plant != null)
			return;
		plant = SolarPlant.build(world, pos);
	}

	@Override
	@Nullable
	public SolarPlant getPlant() {
		return plant;
	}

	@Override
	public void setPlant(@Nullable SolarPlant p) {
		plant = p;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	/** Client side only (the original sent it to the server, which never used it). */
	public float getAimingAccuracy() {
		return aimFactor;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (broken)
			return;
		this.searchForPlant(world, pos);

		if (world.isClientSide && (this.getTicksExisted() < 400 || rotatingLarge || Math.abs(world.getGameTime()%8) == Math.abs(System.identityHashCode(this)%8)))
			this.adjustAim(world, pos);

		if (!world.isClientSide) {
			AABB above = new AABB(pos.getX()+0.25, pos.getY()+1, pos.getZ()+0.25, pos.getX()+0.75, pos.getY()+1.5, pos.getZ()+0.75);
			for (Entity e : world.getEntitiesOfClass(Entity.class, above)) {
				double m = getEntityMass(e);
				double vy = e.getDeltaMovement().y;
				if (vy < -0.1 && m-vy*20 > 80) {
					e.hurt(world.damageSources().cactus(), 1);
					this.breakMirror(world, pos);
					break;
				}
			}
		}
	}

	/** Original ReikaEntityHelper.getEntityMass (kg). */
	public static double getEntityMass(Entity ent) {
		if (ent instanceof ItemEntity || ent instanceof ExperienceOrb)
			return 0.25;
		if (ent instanceof Creeper)
			return 100;
		if (ent instanceof Skeleton)
			return 30;
		if (ent instanceof ZombifiedPiglin)
			return 90;
		if (ent instanceof Zombie || ent instanceof Player || ent instanceof Villager || ent instanceof Witch)
			return 70;
		if (ent instanceof CaveSpider)
			return 30;
		if (ent instanceof Spider)
			return 60;
		if (ent instanceof Pig)
			return 100;
		if (ent instanceof Cow)
			return 350;
		if (ent instanceof Ghast)
			return 20;
		if (ent instanceof Blaze)
			return 300;
		if (ent instanceof MagmaCube cube)
			return 400*cube.getSize()*cube.getSize();
		if (ent instanceof Slime cube)
			return 200*cube.getSize()*cube.getSize();
		if (ent instanceof EnderMan)
			return 40;
		if (ent instanceof Silverfish)
			return 1;
		if (ent instanceof Chicken)
			return 2;
		if (ent instanceof EnderDragon)
			return 10000;
		if (ent instanceof WitherBoss)
			return 3000;
		if (ent instanceof Wolf)
			return 50;
		if (ent instanceof Ocelot)
			return 15;
		if (ent instanceof IronGolem)
			return 32000;
		if (ent instanceof AbstractGolem)
			return 100;
		if (ent instanceof Sheep)
			return 150;
		if (ent instanceof Squid)
			return 120;
		if (ent instanceof Bat)
			return 0.5;
		if (ent instanceof AbstractMinecart)
			return 400;
		if (ent instanceof Boat)
			return 70;
		if (ent instanceof PrimedTnt)
			return 2700;
		if (ent instanceof FallingBlockEntity)
			return 1000; //original: the block's density in kg/m^3 - every block's is far above the 80 kg needed to break a mirror
		return 100;
	}

	public boolean isFunctional() {
		if (broken)
			return false;
		BlockPos up = worldPosition.above();
		if (level.getBlockEntity(up) instanceof RotaryCraftBlockEntity te
				&& "rotarycraft".equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(te.getType()).getNamespace()))
			return false;
		return plant != null && plant.canSeeTheSky(this);
	}

	/** Original adjustAim (client): halfway between the sun and the tower top, eased in at dawn and dusk. */
	private void adjustAim(Level world, BlockPos pos) {
		if (plant == null)
			return;
		BlockPos target = plant.getAimingPositionForMirror(this);
		if (target == null)
			return;

		float finalphi;
		float finaltheta;

		long tot = world.getDayTime();
		int time = (int)(tot%12000);
		time = forceDuskDawnAiming(tot, time);

		float sunphi = time >= 6000 ? -90 : 90;
		float suntheta = 0.5F*(float)(90*Math.sin(Math.toRadians((world.getDayTime()%12000)*90D/6000D))); //original ReikaWorldHelper.getSunAngle

		float movespeed = 0.5F;

		double dx = pos.getX()-target.getX(), dy = pos.getY()-target.getY(), dz = pos.getZ()-target.getZ();
		double len = Math.sqrt(dx*dx+dy*dy+dz*dz);
		float targetphi = (float)(180+Math.toDegrees(Math.atan2(dx, dz)));
		float targettheta = (float)Math.toDegrees(Math.acos(dy/len));

		targettheta = Math.abs(targettheta)-90;
		targettheta *= 0.5F;

		sunphi = clampPhi(sunphi, time);
		boolean bool = time >= 6000 || targetphi > 270;
		if (bool)
			targetphi = clampPhi(targetphi, time);

		if (time >= 6000)
			finalphi = sunphi-(sunphi-targetphi)/2F;
		else
			finalphi = sunphi+(targetphi-sunphi)/2F;

		float sunangle = time >= 6000 ? (float)(1-Math.cos(Math.toRadians((time-6000)*90D/6000D))) : (float)Math.cos(Math.toRadians(time*90D/6000D));

		finalphi = finalphi*sunangle+(1-sunangle)*targetphi;
		finalphi = clampPhi(finalphi, time);

		finaltheta = targettheta+(suntheta-targettheta)/2F;

		if (!(targetphi >= 0 && targetphi <= 90) && time >= 6000) {
			finalphi = -sunphi-(sunphi-targetphi)/2F;
			finalphi = finalphi*sunangle+(1-sunangle)*targetphi;
		}

		finalphi = this.adjustPhiForClosestPath(finalphi);
		if (Math.abs(sunphi-targetphi) == 180) {
			finalphi = targetphi;
			finaltheta = Math.max(60-suntheta, finaltheta);
		}

		if (finalphi-phi > 180)
			finalphi -= 360;

		targetTheta = finaltheta;
		targetPhi = finalphi;

		if (phi < targetPhi)
			phi += movespeed;
		if (phi > targetPhi)
			phi -= movespeed;

		if (theta < targetTheta)
			theta += movespeed;
		if (theta > targetTheta)
			theta -= movespeed;

		float aim = (float)Math.max(0, 1-Math.sqrt((theta-targetTheta)*(theta-targetTheta)+(phi-targetPhi)*(phi-targetPhi))/20D);
		if (Math.abs(aimFactor-aim) > 0.05)
			aimFactor = aim;

		if (rotatingLarge)
			rotatingLarge = Math.abs(targetPhi-phi) > 2;
		else
			rotatingLarge = Math.abs(targetPhi-phi) > 10;
	}

	private static int forceDuskDawnAiming(long tot, int time) {
		int day = (int)(tot%24000);
		if (day >= 12000 && day <= 13000)
			return 11999;
		if (day >= 23000 && day <= 24000)
			return 0;
		return time;
	}

	private static float clampPhi(float phi, int time) {
		boolean afternoon = time >= 6000;
		if (afternoon) {
			if (phi >= 360)
				phi -= 360;
			if (phi < -360)
				phi += 360;
		}
		else {
			if (phi > 180)
				phi -= 360;
			if (phi <= -180)
				phi += 360;
		}
		return phi;
	}

	private float adjustPhiForClosestPath(float finalphi) {
		if (Math.signum(finalphi) != Math.signum(phi)) { //original ReikaMathLibrary.isSameSign
			if (finalphi < -180)
				finalphi += 360;
			if (finalphi > 180)
				finalphi -= 360;
			if (finalphi < 0 && finalphi < -90)
				finalphi += 360;
		}
		return finalphi;
	}

	public void breakMirror(Level world, BlockPos pos) {
		broken = true;
		world.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1, 1);
		if (world instanceof ServerLevel sl)
			sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GLASS.defaultBlockState()), pos.getX()+0.5, pos.getY()+0.9, pos.getZ()+0.5,
					24, 0.3, 0.1, 0.3, 0.1);
		this.syncAllData();
	}

	public void repair(Level world, BlockPos pos) {
		broken = false;
		this.syncAllData();
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		if (plant != null)
			plant.invalidate(world);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("broke", broken);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		broken = NBT.getBoolean("broke");
	}
}
