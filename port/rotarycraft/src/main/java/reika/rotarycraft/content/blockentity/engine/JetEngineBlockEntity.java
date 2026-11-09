/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity.engine;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.TemperatureMachine;
import reika.rotarycraft.content.blockentity.UpgradeableMachine;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.content.item.EngineUpgradeItem;
import reika.rotarycraft.content.item.ScrewdriverItem;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.lib.world.TemperatureEnvironment;
import reika.rotarycraft.registry.DifficultyEffects;
import reika.rotarycraft.registry.EngineType;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryComponents;
import reika.rotarycraft.registry.RotaryDamage;
import reika.rotarycraft.registry.RotaryItems;
import reika.rotarycraft.registry.RotarySounds;

/**
 * Original: Reika.RotaryCraft.TileEntities.Engine.TileEntityJetEngine (gas turbine). Burns jet fuel, sucks in entities in front
 * (items and XP come out the back, living things cause foreign object damage "FOD"), is choked by blocks in its intake, heats the
 * exhaust area, can fail (backfires, then explodes above 1000 C) and takes the afterburner upgrade (double torque, more fuel, hotter).
 * <p>
 * Not ported: achievements (advancements come later), the JetEngineEnterFailure/Explosion API events, the bedrock-boots
 * suction reduction and turret-shot immunity (those items/entities are not ported yet) and the other mods' special cases
 * (Void Monster, Thaumcraft wisps, Reika's EtherealEntity). Wrenches of other mods are recognised by the convention tag c:tools/wrench.
 * <p>
 * Technical: the original ran most of this on both sides. Here world changes (damage, ingestion, explosions, block effects)
 * run on the server; the client-only visuals of failure, backfire and explosion are sent with block events.
 */
public class JetEngineBlockEntity extends EngineBlockEntity implements UpgradeableMachine {

	private static final int EVENT_FAILING = 1;
	private static final int EVENT_BACKFIRE = 2;
	private static final int EVENT_EXPLODE = 3;

	private boolean isJetFailing = false;

	/** Used in jet engines */
	public int FOD = 0;

	private int dumpx;
	private int dumpz;
	private int dumpvx;
	private int dumpvz;

	/** Original: StepTimer(479) */
	private int jetstarttimer = 0;
	private static final int JETSTART_CAP = 479;
	private int afterburnTick = 2000;

	private int chickenCount = 0;

	private boolean isChoking = false;

	private boolean canAfterBurn;
	private boolean burnerActive;

	public static final int BASE_CONSUMPTION = 10;
	public static final int AFTERBURNER_CONSUMPTION = 25;

	public JetEngineBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.JET_ENGINE.get(), pos, state, EngineType.JET);
	}

	@Override
	public int getFuelLevel() {
		return fuel.getFluidAmount();
	}

	@Override
	protected void consumeFuel(float scale) {
		fuel.drain((int)(scale*this.getConsumedFuel()), IFluidHandler.FluidAction.EXECUTE);
	}

	@Override
	protected int getConsumedFuel() {
		return this.isAfterburning() ? AFTERBURNER_CONSUMPTION : BASE_CONSUMPTION;
	}

	@Override
	protected void internalizeFuel() {

	}

	@Override
	protected boolean canBeThrottled() {
		return !this.isAfterburning();
	}

	@Override
	protected boolean getRequirements(Level world, BlockPos pos, int meta) {
		if (FOD >= 8) {
			jetstarttimer = 0;
			return false;
		}
		if (fuel.getFluidAmount() <= 0) {
			jetstarttimer = 0;
			return false;
		}
		return true;
	}

	private void checkJetFailure(Level world, BlockPos pos, int meta) {
		if (isJetFailing)
			this.jetEngineDetonation(world, pos, meta);
		else if (FOD > 0 && rand.nextInt(DifficultyEffects.JETFAILURE.getInt()*(9-FOD)) == 0) {
			this.triggerJetFailing(world, pos);
		}
	}

	/** Fraction of the intake left open by the block in front (original getChokedFraction, block bounds from its shape). */
	public float getChokedFraction(Level world, BlockPos pos, int meta) {
		BlockPos p = switch(meta) {
			case 0 -> pos.offset(1, 0, 0);
			case 1 -> pos.offset(-1, 0, 0);
			case 2 -> pos.offset(0, 0, 1);
			case 3 -> pos.offset(0, 0, -1);
			default -> pos;
		};
		BlockState b = world.getBlockState(p);
		if (b.isAir())
			return 1;
		if (b.getCollisionShape(world, p).isEmpty())
			return 1;
		if (b.is(BlockTags.FENCES))
			return 0.75F;
		if (b.is(Blocks.IRON_BARS))
			return 1F;
		if (b.is(BlockTags.WALLS))
			return 0.25F;
		if (b.is(Blocks.GLASS_PANE))
			return 0.5F;
		VoxelShape sh = b.getShape(world, p);
		if (sh.isEmpty())
			return 1;
		AABB bb = sh.bounds();
		if (bb.maxX > 0.875 && bb.maxY > 0.875 && bb.maxZ > 0.875)
			if (bb.minX < 0.125 && bb.minY < 0.125 && bb.minZ < 0.125)
				return 0;
		double dx = bb.maxX-bb.minX;
		double dy = bb.maxY-bb.minY;
		double dz = bb.maxZ-bb.minZ;
		if (bb.maxX <= 0.125 || bb.minX >= 0.875)
			dx = 0;
		if (bb.maxY <= 0.125 || bb.minY >= 0.875)
			dy = 0;
		if (bb.maxZ <= 0.125 || bb.minZ >= 0.875)
			dz = 0;
		if (bb.maxY >= 0.75)
			dy += 0.125;
		return (float)(1-(dx*dy*dz));
	}

	private void heatJet(Level world, BlockPos pos, int meta) {
		if (this.isOn() && this.getTicksExisted()%10 == 0) {
			int max = this.getMaxExhaustTemperature()*omega/type.getSpeed();
			if (max > temperature)
				temperature = Math.min(temperature+Math.max(1, (max-temperature)/16), max);
			else if (!this.isAfterburning()) {
				temperature = Math.max(temperature-Math.max(1, (temperature-max)/32), max);
			}
		}
		int T = temperature;
		int r = this.isAfterburning() ? 6 : 4;
		for (int i = 1; i < r; i++) {
			BlockPos d = pos.relative(write, i);
			BlockEntity te = world.getBlockEntity(d);
			if (te instanceof TemperatureMachine tm) {
				if (tm.allowExternalHeating()) {
					int dT = T-tm.getTemperature();
					tm.addTemperature(dT);
				}
			}
			if (this.isAfterburning()) {
				TemperatureEnvironment.temperatureEnvironment(world, d, Math.min(1400, T));
			}
		}
		int x = pos.getX(), y = pos.getY(), z = pos.getZ();
		int x1 = write.getStepX() != 0 ? write.getStepX() > 0 ? x : x-4 : x;
		int x2 = write.getStepX() != 0 ? write.getStepX() > 0 ? x+5 : x+1 : x+1;
		int z1 = write.getStepZ() != 0 ? write.getStepZ() > 0 ? z : z-4 : z;
		int z2 = write.getStepZ() != 0 ? write.getStepZ() > 0 ? z+5 : z+1 : z+1;
		AABB box = new AABB(x1, y, z1, x2, y+1, z2);
		List<LivingEntity> li = world.getEntitiesOfClass(LivingEntity.class, box);
		for (LivingEntity e : li) {
			e.hurt(RotaryDamage.source(world, RotaryDamage.HEAT), this.isAfterburning() ? 4 : 1);
		}
	}

	public int getMaxExhaustTemperature() {
		return this.isAfterburning() ? 1750 : 1200;
	}

	@Override
	public int getMaxTemperature() {
		return this.isAfterburning() ? 2000 : 1500;
	}

	/**
	 * Like BC obsidian pipe - suck in entities in a "funnel" in front of the engine, and deal damage (50 hearts).
	 * Items (including players' inventories and mob drops) will be spat out the back.
	 * Large mobs (Player, creeper, spider, ghast, etc) will cause foreign object damage, necessitating repair.
	 */
	private void ingest(Level world, BlockPos pos, int meta) {
		if (FOD >= 8)
			return;
		Direction dir = write.getOpposite();
		double px = pos.getX()+0.5+dir.getStepX()*0.49;
		double pz = pos.getZ()+0.5+dir.getStepZ()*0.49;
		for (int step = 0; step < 8; step++) {
			AABB zone = this.getSuctionZone(pos, meta, step);
			List<Entity> inzone = world.getEntitiesOfClass(Entity.class, zone);
			for (Entity caught : inzone) {
				if (caught.isRemoved())
					continue;
				if (this.canSuckTowards(world, pos, caught, px, pz)) {
					float mult = this.getSuctionMultiplier(caught);
					if (mult > 0) {
						caught.setDeltaMovement(caught.getDeltaMovement().add((pos.getX()+0.5D-caught.getX())/20*mult,
								(pos.getY()+0.5D-caught.getY())/20*mult, (pos.getZ()+0.5D-caught.getZ())/20*mult));
						if (!world.isClientSide)
							caught.hurtMarked = true;
					}
					if (!world.isClientSide && ReikaMathLibrary.py3d(caught.getX()-px, caught.getY()-(pos.getY()+0.5), caught.getZ()-pz) < 1.2) {
						this.ingest(world, pos, caught, mult <= 0);
					}
				}
			}
		}
	}

	/** Original: DragonAPI RayTracer.isClearLineOfSight - blocked by any block that is not soft (air, fluids, replaceable, vines). */
	private boolean canSuckTowards(Level world, BlockPos pos, Entity e, double px, double pz) {
		int n = 2;
		for (int i = 0; i <= n; i++) {
			if (isClearLineOfSight(world, new Vec3(px, pos.getY()+0.5, pz), new Vec3(e.getX(), e.getY()+e.getBbHeight()*i/n, e.getZ())))
				return true;
		}
		return false;
	}

	private static boolean isClearLineOfSight(Level world, Vec3 from, Vec3 to) {
		BlockPos start = BlockPos.containing(from);
		BlockPos end = BlockPos.containing(to);
		Vec3 cur = from;
		Vec3 dir = to.subtract(from).normalize().scale(0.01);
		for (int guard = 0; guard < 64; guard++) {
			BlockHitResult hit = world.clip(new ClipContext(cur, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CollisionContext.empty()));
			if (hit.getType() == HitResult.Type.MISS)
				return true;
			BlockPos hp = hit.getBlockPos();
			if (!hp.equals(start) && !hp.equals(end)) {
				BlockState b = world.getBlockState(hp);
				if (!b.canBeReplaced() && !b.is(Blocks.VINE))
					return false;
			}
			cur = hit.getLocation().add(dir);
			if (cur.distanceToSqr(from) >= to.distanceToSqr(from))
				return true;
		}
		return true;
	}

	private float getSuctionMultiplier(Entity e) {
		if (e instanceof Player epl) {
			if (epl.isCreative() || epl.isSpectator())
				return 0;
			//Original: bedrock boots / bedrock jump boots reduce the pull to 0.1 (not ported yet)
		}
		//Original: turret shots are immune (not ported yet)
		if (e instanceof EnderDragon)
			return 0;
		return 1;
	}

	/** Original: ReikaWorldHelper.nonSolidBlocks - no collision box. */
	private static boolean nonSolidBlocks(Level world, BlockPos p) {
		return world.getBlockState(p).getCollisionShape(world, p).isEmpty();
	}

	private void moveDumpPosition(Level world, int y) {
		int trycount = 0;
		while (trycount < 1 && !nonSolidBlocks(world, new BlockPos(dumpx, y, dumpz))) {
			if (dumpvx == 1)
				dumpx++;
			if (dumpvx == -1)
				dumpx--;
			if (dumpvz == 1)
				dumpz++;
			if (dumpvz == -1)
				dumpz--;
			trycount++;
		}
	}

	/** Kill the adjacent entities, except items, which are teleported */
	private void ingest(Level world, BlockPos pos, Entity e, boolean immune) {
		int y = pos.getY();
		if (e instanceof ItemEntity ei) {
			if (e.isAlive()) {
				ItemStack is = ei.getItem().copy();
				e.discard();
				this.moveDumpPosition(world, y);
				ItemStack repl = this.modifyIngestedItem(is);
				if (repl != null) {
					ItemEntity item = new ItemEntity(world, dumpx+0.5D, y+0.375D, dumpz+0.5D, repl);
					item.setDeltaMovement(dumpvx*1.5D, 0.15, dumpvz*1.5D);
					world.addFreshEntity(item);
				}
				if (this.itemDestroysEngine(is)) {
					FOD = 2;
					this.triggerJetFailing(world, pos);
				}
			}
		}
		else if (e instanceof ExperienceOrb orb) {
			if (e.isAlive()) {
				int xp = orb.getValue();
				e.discard();
				this.moveDumpPosition(world, y);
				ExperienceOrb item = new ExperienceOrb(world, dumpx+0.5D, y+0.375D, dumpz+0.5D, xp);
				item.setDeltaMovement(dumpvx*1.5D, 0.15, dumpvz*1.5D);
				world.addFreshEntity(item);
			}
		}
		else if (e instanceof LivingEntity le && !(e instanceof Player && immune)) {
			e.igniteForSeconds(2);
			if (le.getHealth() > 0 && this.canDamageEngine(e))
				this.damageEngine();
			if (FOD > 8)
				FOD = 8;
			if (e instanceof Chicken && e.isAlive() && le.getHealth() > 0) {
				chickenCount++; //Original: 50 chickens trigger an achievement (not ported yet)
			}
			if (e.isAlive() && !(le.getHealth() < 0))
				RotarySounds.play(world, pos, RotarySounds.INGESTION.get(), 1, 1.4F);
			e.hurt(RotaryDamage.source(world, RotaryDamage.JETINGEST), 10000);
		}
	}

	private ItemStack modifyIngestedItem(ItemStack is) {
		if (is.getItem() instanceof ArmorItem && is.isDamageableItem()) {
			int max = is.getMaxDamage();
			int after = is.getDamageValue()+rand.nextInt(200);
			if (after >= max)
				return null;
			else
				is.setDamageValue(after);
		}
		else if (is.getItem() instanceof DiggerItem && !(is.getItem() instanceof HoeItem) && is.isDamageableItem()) { //Original: ItemTool (pickaxe, axe, shovel)
			int max = is.getMaxDamage();
			int after = is.getDamageValue()+rand.nextInt(40);
			if (after >= max)
				return null;
			else
				is.setDamageValue(after);
		}
		if (is.is(Items.STONE)) {
			is = is.transmuteCopy(Items.COBBLESTONE, is.getCount());
			FOD += 4;
		}
		else if (is.is(Items.COBBLESTONE)) {
			is = is.transmuteCopy(Items.GRAVEL, is.getCount());
			FOD += 2;
		}
		else if (is.is(Items.GRAVEL)) {
			is = is.transmuteCopy(Items.SAND, is.getCount());
			FOD += 1;
		}
		return is;
	}

	private void triggerJetFailing(Level world, BlockPos pos) {
		RotaryCraft.LOGGER.warn(this+" just entered failure mode!");
		isJetFailing = true;

		world.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 2, 0.75F);
		RotarySounds.play(world, pos, RotarySounds.INGESTION.get(), 0.5F, 1);

		AABB box = new AABB(pos).expandTowards(write.getStepX()*8, pos.getY(), write.getStepZ()*8).inflate(3, 3, 3);
		List<LivingEntity> li = world.getEntitiesOfClass(LivingEntity.class, box);
		Set<Integer> idSet = new HashSet<>();
		for (LivingEntity e : li) {
			e.hurt(world.damageSources().generic(), 8);
			idSet.add(e.getId());
		}
		box = new AABB(pos).inflate(4, 4, 4);
		li = world.getEntitiesOfClass(LivingEntity.class, box);
		for (LivingEntity e : li) {
			if (!idSet.contains(e.getId()))
				e.hurt(world.damageSources().generic(), 4);
		}
		world.blockEvent(pos, this.getBlockState().getBlock(), EVENT_FAILING, 0);
		this.syncAllData();
	}

	/** Original client half of triggerJetFailing: scrap flying out of the exhaust and break particles of the surroundings. */
	private void failingEffects(Level world, BlockPos pos) {
		ItemStack scrap = new ItemStack(RotaryItems.part(CraftingPart.SCRAP));
		for (int i = 0; i < 24; i++) {
			double v = 0.8+rand.nextDouble()*1.2;
			double vx = write.getStepX()*v+(rand.nextDouble()*2-1)*0.25;
			double vz = write.getStepZ()*v+(rand.nextDouble()*2-1)*0.25;
			double vy = (rand.nextDouble()*2-1)*0.25;
			world.addParticle(new ItemParticleOption(ParticleTypes.ITEM, scrap), pos.getX()+0.5+write.getStepX(), pos.getY()+0.5, pos.getZ()+0.5+write.getStepZ(), vx, vy, vz);
		}
		int r = 8;
		for (int i = -r; i <= r; i++) {
			for (int j = -r; j <= r; j++) {
				for (int k = -r; k <= r; k++) {
					BlockState b = world.getBlockState(pos.offset(i, j, k));
					if (b.isAir())
						continue;
					//Original ReikaRenderHelper.spawnDropParticles(world, x, y, z, ...): 12-24 particles at the engine itself
					int n = 12+rand.nextInt(12);
					for (int m = 0; m < n; m++) {
						world.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, b), pos.getX()+rand.nextDouble(), pos.getY()+rand.nextDouble(), pos.getZ()+rand.nextDouble(),
								(rand.nextDouble()*2-1)*0.25, 0.125+rand.nextDouble()*0.875, (rand.nextDouble()*2-1)*0.25);
					}
				}
			}
		}
	}

	private boolean itemDestroysEngine(ItemStack is) {
		return is.getItem() instanceof ScrewdriverItem || is.is(Tags.Items.TOOLS_WRENCH);
	}

	private void damageEngine() {
		FOD++;
		if (DifficultyEffects.JETINGESTFAIL.testChance()) {
			isJetFailing = true;
			temperature = Math.max(temperature, 800);
		}
	}

	private boolean canDamageEngine(Entity caught) {
		if (caught.noPhysics)
			return false;
		if (caught instanceof Chicken)
			return false;
		if (caught instanceof Bat)
			return false;
		if (caught instanceof Silverfish)
			return false;
		if (caught instanceof ItemEntity)
			return false;
		if (caught instanceof ExperienceOrb)
			return false;
		String name = caught.getName().getString().toLowerCase(Locale.ENGLISH);
		if (name.contains("bird"))
			return false;
		if (name.contains("firefly"))
			return false;
		if (name.contains("butterfly"))
			return false;
		return caught instanceof LivingEntity;
	}

	private AABB getSuctionZone(BlockPos pos, int meta, int step) {
		int x = pos.getX(), y = pos.getY(), z = pos.getZ();
		int minx = 0;
		int miny = 0;
		int minz = 0;
		int maxx = 0;
		int maxy = 0;
		int maxz = 0;

		switch (meta) {
			case 0 -> {
				minx = x+1+step;
				maxx = x+1+step+1;
				miny = y-step;
				maxy = y+step+1;
				minz = z-step;
				maxz = z+step+1;
				dumpx = x-1;
				dumpz = z;
				dumpvx = -1;
				dumpvz = 0;
			}
			case 1 -> {
				minx = x-1-step;
				maxx = x-1-step+1;
				miny = y-step;
				maxy = y+step+1;
				minz = z-step;
				maxz = z+step+1;
				dumpx = x+1;
				dumpz = z;
				dumpvx = 1;
				dumpvz = 0;
			}
			case 2 -> {
				minz = z+1+step;
				maxz = z+1+step+1;
				miny = y-step;
				maxy = y+step+1;
				minx = x-step;
				maxx = x+step+1;
				dumpx = x;
				dumpz = z-1;
				dumpvx = 0;
				dumpvz = -1;
			}
			case 3 -> {
				minz = z-1-step;
				maxz = z-1-step+1;
				miny = y-step;
				maxy = y+step+1;
				minx = x-step;
				maxx = x+step+1;
				dumpx = x;
				dumpz = z+1;
				dumpvx = 0;
				dumpvz = 1;
			}
		}

		return new AABB(minx, miny, minz, maxx, maxy, maxz).inflate(0.25, 0.25, 0.25);
	}

	/** Original: repair with a turbine item (BlockEngine). */
	public void repairJet() {
		FOD = 0;
		isJetFailing = false;
		temperature = Math.max(AmbientTemperature.getAmbientTemperatureAt(level, worldPosition), temperature/2);
		this.syncAllData();
	}

	/** Original: repair with a compressor item (BlockEngine). */
	public void repairJetPartial() {
		if (FOD > 0)
			FOD--;
		this.syncAllData();
	}

	private void jetEngineDetonation(Level world, BlockPos pos, int meta) {
		AABB zone = this.getFlameZone(pos, meta);
		List<LivingEntity> in = world.getEntitiesOfClass(LivingEntity.class, zone);
		for (LivingEntity e : in) {
			e.igniteForSeconds(2);
		}
		BlockPos back = this.getBackPos();
		int dx = pos.getX()-back.getX();
		int dz = pos.getZ()-back.getZ();
		for (int i = 0; i < 16; i++) {
			TemperatureEnvironment.temperatureEnvironment(world, pos.offset(dx*i, 0, dz*i), 800);
		}
		world.playSound(null, pos, SoundEvents.BLAZE_HURT, SoundSource.BLOCKS, 1F, 1F);
		if (fuel.getFluidAmount() < FUELCAP/12 && rand.nextInt(10) == 0) {
			this.backFire(world, pos);
		}
		if (fuel.getFluidAmount() < FUELCAP/4 && rand.nextInt(20) == 0) {
			this.backFire(world, pos);
		}
		else if (rand.nextInt(40) == 0) {
			this.backFire(world, pos);
		}
		if (rand.nextInt(2) == 0)
			temperature++;

		if (temperature >= 800) {
			RotaryCraft.LOGGER.warn("WARNING: "+this+" is near explosion!");
		}

		if (temperature > 1000) {
			this.fail(world, pos);
		}
	}

	/** Original client half of jetEngineDetonation: flames and smoke blown out of the exhaust. */
	private void detonationParticles(Level world, BlockPos pos) {
		BlockPos back = this.getBackPos();
		double vx = (pos.getX()-back.getX())/2D;
		double vz = (pos.getZ()-back.getZ())/2D;
		for (int i = 0; i < 16; i++) {
			world.addParticle(i%2 == 0 ? ParticleTypes.FLAME : ParticleTypes.SMOKE, pos.getX()+0.25+0.5*rand.nextDouble(), pos.getY()+0.25+0.5*rand.nextDouble(), pos.getZ()+0.25+0.5*rand.nextDouble(),
					vx-0.1+0.2*rand.nextDouble(), -0.1+0.2*rand.nextDouble(), vz-0.1+0.2*rand.nextDouble());
		}
	}

	private void fail(Level world, BlockPos pos) {
		if (RotaryConfig.BLOCKDAMAGE.get()) {
			int r = 6;
			for (int i = -r; i <= r; i++) {
				for (int j = -r; j <= r; j++) {
					for (int k = -r; k <= r; k++) {
						TemperatureEnvironment.temperatureEnvironment(world, pos.offset(i, j, k), 1000);
					}
				}
			}
		}
		world.blockEvent(pos, this.getBlockState().getBlock(), EVENT_EXPLODE, 0);
		world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 12F, true, Level.ExplosionInteraction.BLOCK);
		for (int m = 0; m < 6; m++)
			world.explode(null, pos.getX()-4+rand.nextInt(11), pos.getY()-4+rand.nextInt(11), pos.getZ()-4+rand.nextInt(11), 4F+rand.nextFloat()*2, true, Level.ExplosionInteraction.BLOCK);
	}

	private void explosionParticles(Level world, BlockPos pos) {
		int r = 6;
		for (int i = -r; i <= r; i++) {
			for (int j = -r; j <= r; j++) {
				for (int k = -r; k <= r; k++) {
					world.addParticle(ParticleTypes.LAVA, pos.getX()+i, pos.getY()+j, pos.getZ()+k, 0, 0, 0);
					world.addParticle(ParticleTypes.LAVA, pos.getX()+i, pos.getY()+j, pos.getZ()+k, rand.nextDouble()-0.5, rand.nextDouble()-0.5, rand.nextDouble()-0.5);
				}
			}
		}
	}

	/** Original backFire (server: a small explosion without block damage; the clients get the flames through ENGINEBACKFIRE). */
	public void backFire(Level world, BlockPos pos) {
		world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 2*rand.nextFloat(), false, Level.ExplosionInteraction.NONE);
		world.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1, 0.5F);
		world.blockEvent(pos, this.getBlockState().getBlock(), EVENT_BACKFIRE, 0);
	}

	private void backFireParticles(Level world, BlockPos pos) {
		BlockPos back = this.getBackPos();
		double vx = (pos.getX()-back.getX())/2D;
		double vz = (pos.getZ()-back.getZ())/2D;
		for (int i = 0; i < 32; i++) {
			world.addParticle(i%2 == 0 ? ParticleTypes.FLAME : ParticleTypes.SMOKE, pos.getX()+0.25+0.5*rand.nextDouble(), pos.getY()+0.25+0.5*rand.nextDouble(), pos.getZ()+0.25+0.5*rand.nextDouble(),
					-vx-0.1+0.2*rand.nextDouble(), -0.1+0.2*rand.nextDouble(), -vz-0.1+0.2*rand.nextDouble());
		}
	}

	@Override
	public boolean triggerEvent(int id, int param) {
		if (level != null && level.isClientSide && write != null) {
			switch(id) {
				case EVENT_FAILING -> this.failingEffects(level, worldPosition);
				case EVENT_BACKFIRE -> this.backFireParticles(level, worldPosition);
				case EVENT_EXPLODE -> this.explosionParticles(level, worldPosition);
				default -> {
					return super.triggerEvent(id, param);
				}
			}
			return true;
		}
		return super.triggerEvent(id, param);
	}

	private AABB getFlameZone(BlockPos pos, int meta) {
		int x = pos.getX(), y = pos.getY(), z = pos.getZ();
		return switch(meta) {
			case 0 -> new AABB(x-6, y, z, x+1, y+1, z+1); //-x
			case 1 -> new AABB(x, y, z, x+7, y+1, z+1); //+x
			case 2 -> new AABB(x, y, z-6, x+1, y+1, z+1); //-z
			case 3 -> new AABB(x, y, z, x+1, y+1, z+7); //+z
			default -> new AABB(pos);
		};
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);

		NBT.putBoolean("choke", isChoking);
		NBT.putBoolean("jetfail", isJetFailing);
		NBT.putBoolean("burn", canAfterBurn);
		NBT.putBoolean("burning", burnerActive);
		NBT.putInt("FOD", FOD);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);

		isChoking = NBT.getBoolean("choke");
		isJetFailing = NBT.getBoolean("jetfail");
		canAfterBurn = NBT.getBoolean("burn");
		burnerActive = NBT.getBoolean("burning");
		FOD = NBT.getInt("FOD");
	}

	@Override
	protected void writeAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeAdditional(NBT, registries);
		NBT.putInt("chickens", chickenCount);
	}

	@Override
	protected void readAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readAdditional(NBT, registries);
		chickenCount = NBT.getInt("chickens");
	}

	@Override
	protected void playSounds(Level world, BlockPos pos, float pitchMultiplier, float volume) {
		soundtick++;
		afterburnTick++;
		if (FOD > 0 && rand.nextInt(2*(9-FOD)) == 0) {
			world.playSound(null, pos, SoundEvents.BLAZE_HURT, SoundSource.BLOCKS, 1F+rand.nextFloat(), 1F);
		}
		if (this.isMuffled(world, pos)) {
			volume *= 0.3125F;
		}

		if (this.isAfterburning() && afterburnTick >= 50) {
			afterburnTick = 0;
			float vol = 0.9F;
			if (omega < type.getSpeed()) {
				vol *= (float)Math.pow(0.75, type.getSpeed()/omega);
			}
			RotarySounds.play(world, pos, RotarySounds.AFTERBURN.get(), vol, 1);
			RotarySounds.play(world, pos, RotarySounds.AFTERBURN.get(), vol, 1);
		}

		if (soundtick < this.getSoundLength(1F/pitchMultiplier) && soundtick < 2000)
			return;
		soundtick = 0;

		float pitch = 1F/(0.125F*FOD+1);
		if (jetstarttimer >= JETSTART_CAP)
			RotarySounds.play(world, pos, RotarySounds.JET.get(), volume, pitch*pitchMultiplier);
		else
			soundtick = 2000;
	}

	public boolean isAfterburning() {
		return canAfterBurn && burnerActive;
	}

	@Override
	public boolean hasTemperature() {
		return true;
	}

	@Override
	protected int getMaxSpeed(Level world, BlockPos pos, int meta) {
		return (int)(EngineType.JET.getSpeed()*this.getChokedFraction(world, pos, meta));
	}

	@Override
	protected boolean canStart() {
		return true; //JumpStart code goes here
	}

	@Override
	protected int getGenTorque(Level world, BlockPos pos, int meta) {
		int amt = EngineType.JET.getTorque();
		if (this.isAfterburning())
			amt *= 2;
		return amt/ReikaMathLibrary.intpow2(2, FOD);
	}

	@Override
	protected void affectSurroundings(Level world, BlockPos pos, int meta) {
		if (world.isClientSide) {
			if (isJetFailing)
				this.detonationParticles(world, pos);
			if (FOD > 0 && rand.nextInt(2*(9-FOD)) == 0)
				world.addParticle(ParticleTypes.CRIT, pos.getX()+rand.nextFloat(), pos.getY()+rand.nextFloat(), pos.getZ()+rand.nextFloat(), -0.5+rand.nextFloat(), rand.nextFloat(), -0.5+rand.nextFloat());
		}
		else {
			this.checkJetFailure(world, pos, meta);
		}
		this.ingest(world, pos, meta);
		this.fluidIngest(world, pos);
		if (!world.isClientSide)
			this.heatJet(world, pos, meta);
		if (lastpower == 0) {
			RotarySounds.play(world, pos, RotarySounds.JETSTART.get(), 1, 1);
		}
		if (world.isClientSide)
			this.spawnSmokeParticles(world, pos, meta);
		jetstarttimer++;
		this.doAfterburning(world, pos);
	}

	private void fluidIngest(Level world, BlockPos pos) {
		BlockPos d = pos.relative(write.getOpposite());
		FluidState fs = world.getFluidState(d);
		if (!fs.isEmpty()) {
			if (world.isClientSide) {
				this.fluidIngestParticles(world, pos, fs);
			}
			else {
				int temp = fs.getFluidType().getTemperature(fs, world, d);
				if (net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fs.getType()).getPath().toLowerCase(Locale.ENGLISH).contains("fuel")) {
					if (!isJetFailing && rand.nextInt(200) == 0) {
						temperature = 900;
						isJetFailing = true;
						this.jetEngineDetonation(world, pos, this.getBlockMetadata());
					}
				}
				else if (temp >= 500) {
					if (temp >= 2000 || rand.nextInt(1+(2000-temp)/500) == 0) {
						if (FOD < 8 && world.getGameTime()%20 == 0 && rand.nextInt(1+2*FOD) == 0) {
							this.damageEngine();
						}
					}
				}
			}
		}
	}

	/** Original: EntityLiquidParticleFX with the fluid's texture; here the fluid block's break particle. */
	private void fluidIngestParticles(Level world, BlockPos pos, FluidState f) {
		BlockPos back = this.getBackPos();
		int n = 1+rand.nextInt(8);
		for (int i = 0; i < n; i++) {
			double vx = (pos.getX()-back.getX())/4D+(rand.nextDouble()*2-1)*0.0625;
			double vy = (rand.nextDouble()*2-1)*0.0625;
			double vz = (pos.getZ()-back.getZ())/4D+(rand.nextDouble()*2-1)*0.0625;
			world.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, f.createLegacyBlock()), pos.getX()+0.5+write.getStepX()*0.25, pos.getY()+0.5, pos.getZ()+0.5+write.getStepZ()*0.25, vx, vy, vz);
		}
	}

	private void doAfterburning(Level world, BlockPos pos) {
		if (this.isAfterburning()) {
			if (world.isClientSide)
				this.afterBurnParticles(world, pos);
			else if (this.getTicksExisted()%200 == 0) {
				temperature += 1;
				if (temperature > this.getMaxTemperature()) {
					temperature = this.getMaxTemperature();
					this.fail(world, pos);
				}
				else if (temperature >= 600) {
					world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1, 1);
					if (world instanceof net.minecraft.server.level.ServerLevel sl)
						sl.sendParticles(ParticleTypes.SMOKE, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 8, 0.5, 0.5, 0.5, 0);
				}
			}
		}
	}

	/**
	 * Original: red dust coloured by distance from the nozzle (code values: blue-white, white, yellow, dark red-yellow, red).
	 * Note: 1.7.10 reddust turned a red value of 0 into 1 and clamped channels above 1, so the old flame looked whiter; the
	 * colours here are the ones the code specifies.
	 */
	private void afterBurnParticles(Level world, BlockPos pos) {
		BlockPos back = this.getBackPos();
		double dx = (pos.getX()-back.getX())/2D;
		double dz = (pos.getZ()-back.getZ())/2D;
		double vx = (pos.getX()-back.getX())*6D;
		double vz = (pos.getZ()-back.getZ())*6D;
		int x = pos.getX(), y = pos.getY(), z = pos.getZ();
		for (int i = 0; i < 16; i++) {
			int r = 255;
			int g = 0;
			int b = 0;
			double px = dx+x+0.25+0.5*rand.nextDouble()+vx*rand.nextDouble();
			double pz = dz+z+0.25+0.5*rand.nextDouble()+vz*rand.nextDouble();
			double dd = Math.abs(px-x)+Math.abs(pz-z);
			if (dd < 1.5+rand.nextDouble()) {
				r = 0;
				g = 127;
				b = 255;
			}
			else if (dd < 2.5+rand.nextDouble()) {
				r = 255;
				g = 255;
				b = 255;
			}
			else if (dd < 3+rand.nextDouble()*2) {
				g = 255;
			}
			else if (dd < 5+rand.nextDouble()*3 && rand.nextBoolean()) {
				g = 10;
			}
			world.addParticle(new DustParticleOptions(new Vector3f(r/255F, g/255F, b/255F), 1F), px, y+0.75*rand.nextDouble(), pz, 0, 0, 0);
		}
	}

	private void spawnSmokeParticles(Level world, BlockPos pos, int meta) {
		BlockPos back = this.getBackPos();
		int x = pos.getX(), y = pos.getY(), z = pos.getZ();
		double dx = (x-back.getX())/2D;
		double dz = (z-back.getZ())/2D;
		double vx = -(x-back.getX())/2D;
		double vz = -(z-back.getZ())/2D;
		world.addParticle(ParticleTypes.SMOKE, dx+x+0.25+0.5*rand.nextDouble(), y+0.5*rand.nextDouble(), dz+z+0.25+0.5*rand.nextDouble(), -vx-0.1+0.2*rand.nextDouble(), -0.1+0.2*rand.nextDouble(), -vz-0.1+0.2*rand.nextDouble());

		int n = 1+rand.nextInt(8);
		double w = n/2D;
		dx = write.getStepX() == 0 ? x+0.5+(rand.nextDouble()*2-1)*w : x+0.5-n*write.getStepX();
		double dy = y+0.5+(rand.nextDouble()*2-1)*w;
		dz = write.getStepZ() == 0 ? z+0.5+(rand.nextDouble()*2-1)*w : z+0.5-n*write.getStepZ();

		double v = -0.0625;

		vx = v*(dx-x-0.5);
		double vy = v*(dy-y-0.5);
		vz = v*(dz-z-0.5);

		BlockPos bp = BlockPos.containing(dx, dy, dz);
		BlockState b = world.getBlockState(bp);
		if (b.isAir()) {
			if (rand.nextInt(3) == 0)
				world.addParticle(ParticleTypes.CLOUD, dx, dy, dz, vx, vy, vz);
		}
		else {
			//Original: EntityBlockTexFX (block texture, no gravity)
			world.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, b), dx, dy+1, dz, vx, vy-0.03125, vz);
		}
	}

	@Override
	protected void resetPower() {
		super.resetPower();
		jetstarttimer = 0;
	}

	@Override
	protected int getSoundLength(float factor) {
		return super.getSoundLength(factor)+(int)(Math.min(FOD, 7)*11*factor);
	}

	@Override
	protected void applyImplicitComponents(DataComponentInput input) {
		super.applyImplicitComponents(input);
		FOD = input.getOrDefault(RotaryComponents.JET_DAMAGE.get(), 0);
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (FOD > 0)
			components.set(RotaryComponents.JET_DAMAGE.get(), FOD);
	}

	@Override
	public boolean isBroken() {
		return FOD >= 8;
	}

	@Override
	public void upgrade(ItemStack is) {
		canAfterBurn = true;
	}

	@Override
	public boolean canUpgradeWith(ItemStack item) {
		return !canAfterBurn && EngineUpgradeItem.is(item, EngineUpgradeItem.Upgrades.AFTERBURNER);
	}

	public boolean canAfterBurn() {
		return canAfterBurn;
	}

	public boolean burnerActive() {
		return burnerActive;
	}

	public void setBurnerActive(boolean burn) {
		burnerActive = burn;
		if (level != null && !level.isClientSide)
			this.syncAllData();
	}

	public boolean isJetFailing() {
		return isJetFailing;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		super.onBlockBroken(world, pos);
		if (canAfterBurn) {
			this.dropItem(world, pos, new ItemStack(RotaryItems.UPGRADE_AFTERBURNER.get()));
		}
	}

	@Override
	public boolean allowExternalHeating() {
		return false;
	}

	/** Original allowHeatExtraction (HeatConduction): TileEntityJetEngine, true. */
	@Override
	public boolean allowHeatExtraction() {
		return true;
	}
}
