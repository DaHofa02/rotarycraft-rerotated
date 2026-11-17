/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Farming.TileEntitySpawnerController - sits on a mob spawner and takes 131072 W through it
 * (from whichever horizontal side of the spawner delivers the most). It then sets the spawner's delay itself, counting up to the delay set
 * in its GUI (default 800 ticks); when the spawner's delay runs out it spawns the spawner's batch regardless of nearby players, and the
 * mobs never despawn. It only counts while fewer than 99 of that mob are around it (16 blocks sideways, 24 up and down), and holds the
 * spawner off while it is disabled in the GUI or the spawner gets a redstone signal. Without a spawner below it forgets its delay. The shortest
 * delay shown is 800 - 40*log2(omega) ticks.
 * Changes: other mods' controllable spawners are left out; the no-despawn flag is vanilla persistence plus a "ControllerSpawned" tag.
 */
public class SpawnerControllerBlockEntity extends PowerReceiverBlockEntity {

	public static final int BASEDELAY = 800;
	public static final String CONTROLLED_SPAWN_TAG = "ControllerSpawned";

	public boolean disable;
	private int setDelay = BASEDELAY;
	private int timer;

	public SpawnerControllerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.SPAWNER_CONTROLLER.get(), pos, state, 1, 1, 131072);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public int getOperationTime() {
		if (omega <= 0)
			return BASEDELAY;
		return Math.max(0, BASEDELAY-40*(int)ReikaMathLibrary.logbase(omega, 2));
	}

	public void setDelay(int delay) {
		setDelay = delay;
		timer = Math.max(0, Math.min(setDelay-1, timer));
		this.setChanged();
	}

	public int getSetDelay() {
		return setDelay;
	}

	/** What the GUI shows: the set delay, but never less than the operation time. */
	public int getDelay() {
		return Math.max(setDelay, this.getOperationTime());
	}

	/** GUI: -1 disables, anything else enables with that delay. */
	public void handleGui(int value) {
		if (value == -1)
			disable = true;
		else {
			disable = false;
			this.setDelay(value);
		}
		this.syncAllData();
	}

	@Nullable
	private SpawnerBlockEntity spawner(Level world) {
		return world.getBlockEntity(worldPosition.below()) instanceof SpawnerBlockEntity s ? s : null;
	}

	public boolean isValidLocation(Level world) {
		return this.spawner(world) != null;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.setPointingOffset(0, -1, 0);
		if (world.isClientSide)
			return;
		SpawnerBlockEntity sp = this.spawner(world);
		if (sp == null) {
			disable = false;
			setDelay = 0;
			timer = 0;
			omega = torque = 0;
			power = 0;
			return;
		}
		this.readPowerOffset4Sided();
		if (power >= MINPOWER && setDelay > 0)
			this.applyToSpawner((ServerLevel)world, pos.below(), sp.getSpawner());
	}

	private void applyToSpawner(ServerLevel world, BlockPos at, BaseSpawner lgc) {
		if (disable || world.hasNeighborSignal(at)) {
			lgc.spawnDelay = 5;
			this.shutdownParticles(world, at);
		}
		else if (this.canSpawn(world, lgc)) {
			timer++;
			lgc.spawnDelay = setDelay-timer;
			if (timer >= setDelay)
				timer = 0;
		}
		if (lgc.spawnDelay <= 0)
			this.spawnCycle(world, at, lgc);
	}

	private void shutdownParticles(ServerLevel world, BlockPos at) {
		RandomSource r = world.random;
		for (int i = 0; i < 4; i++) {
			double y = at.getY()+r.nextFloat();
			world.sendParticles(DustParticleOptions.REDSTONE, at.getX()-0.25+1.5*r.nextFloat(), y, at.getZ()-0.25+1.5*r.nextFloat(), 1, 0, 0, 0, 0);
			world.sendParticles(ParticleTypes.CRIT, at.getX()+r.nextFloat(), y, at.getZ()+r.nextFloat(), 0, -0.3+0.6*r.nextFloat(), 0.4*r.nextFloat(), -0.3+0.6*r.nextFloat(), 1);
		}
	}

	private static Optional<EntityType<?>> type(BaseSpawner lgc) {
		return lgc.nextSpawnData == null ? Optional.empty() : EntityType.by(lgc.nextSpawnData.entityToSpawn());
	}

	/** Original: fewer than 99 of the spawner's mob within 16 blocks sideways and 24 up and down of the controller. */
	private boolean canSpawn(ServerLevel world, BaseSpawner lgc) {
		Optional<EntityType<?>> t = type(lgc);
		if (t.isEmpty())
			return false;
		return world.getEntities(t.get(), new AABB(worldPosition).inflate(16, 24, 16), e -> true).size() < 99;
	}

	/** Original runVanillaSpawnCycle: the spawner's batch, ignoring nearby players and the spawner's own crowd limit. */
	private void spawnCycle(ServerLevel world, BlockPos at, BaseSpawner lgc) {
		if (lgc.nextSpawnData == null)
			return;
		RandomSource r = world.random;
		for (int i = 0; i < lgc.spawnCount; i++) {
			CompoundTag tag = lgc.nextSpawnData.entityToSpawn().copy();
			double ex = at.getX()+(r.nextDouble()-r.nextDouble())*lgc.spawnRange;
			double ey = at.getY()+r.nextInt(3)-1;
			double ez = at.getZ()+(r.nextDouble()-r.nextDouble())*lgc.spawnRange;
			Entity e = EntityType.loadEntityRecursive(tag, world, x -> {
				x.moveTo(ex, ey, ez, r.nextFloat()*360, 0);
				return x;
			});
			if (e == null)
				continue;
			Mob mob = e instanceof Mob m ? m : null;
			if (mob != null && (!mob.checkSpawnRules(world, MobSpawnType.SPAWNER) || !mob.checkSpawnObstruction(world)))
				continue;
			if (mob != null) {
				mob.setPersistenceRequired();
				if (tag.size() == 1 && tag.contains("id"))
					mob.finalizeSpawn(world, world.getCurrentDifficultyAt(e.blockPosition()), MobSpawnType.SPAWNER, null);
			}
			e.getPersistentData().putBoolean(CONTROLLED_SPAWN_TAG, true);
			if (!world.tryAddFreshEntityWithPassengers(e))
				continue;
			world.levelEvent(2004, at, 0);
			if (mob != null)
				mob.spawnAnim();
		}
	}

	public static boolean isFlaggedNoDespawn(Entity e) {
		return e.getPersistentData().getBoolean(CONTROLLED_SPAWN_TAG);
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("setdelay", setDelay);
		NBT.putBoolean("disable", disable);
		NBT.putInt("timer", timer);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		setDelay = NBT.getInt("setdelay");
		disable = NBT.getBoolean("disable");
		timer = NBT.getInt("timer");
	}
}
