/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.entity;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

import reika.rotarycraft.content.blockentity.SonicBorerBlockEntity;
import reika.rotarycraft.registry.RotaryEntities;

/**
 * Original: Reika.RotaryCraft.Entities.EntitySonicShot - the sonic borer's pulse: flies 2 blocks a tick straight out of the borer and,
 * once as far as the first non-empty 7x7 plane, breaks that plane (dropping the blocks; not unbreakable blocks, liquids are taken too, not
 * shield blocks), deals 1 damage (suffocation) to everything living within 3 blocks and vanishes. It lives at most 1200 ticks.
 * Changes: the original's player-permission check and its mob-spawner item are not ported; it vanishes when its borer is gone.
 */
public class SonicShotEntity extends Entity implements IEntityWithComplexSpawn {

	private BlockPos borer = BlockPos.ZERO;
	private int sx, sy, sz;

	public SonicShotEntity(EntityType<? extends SonicShotEntity> type, Level world) {
		super(type, world);
		noPhysics = true;
	}

	public SonicShotEntity(Level world, SonicBorerBlockEntity te) {
		this(RotaryEntities.SONIC_SHOT.get(), world);
		borer = te.getBlockPos();
		sx = te.xstep;
		sy = te.ystep;
		sz = te.zstep;
		this.setPos(borer.getX()+0.5+sx, borer.getY()+0.5+sy, borer.getZ()+0.5+sz);
		this.setDeltaMovement(sx*2.0, sy*2.0, sz*2.0);
	}

	public int[] getSteps() {
		return new int[] {sx, sy, sz};
	}

	@Override
	public void tick() {
		this.baseTick();
		if (tickCount > 1200) {
			this.discard();
			return;
		}
		if (!level().isClientSide) {
			if (!(level().getBlockEntity(borer) instanceof SonicBorerBlockEntity te)) {
				this.discard();
				return;
			}
			int x = Mth.floor(this.getX()), y = Mth.floor(this.getY()), z = Mth.floor(this.getZ());
			int d = sx*(x-borer.getX())+sy*(y-borer.getY())+sz*(z-borer.getZ());
			if (d >= te.getDistanceToSurface()) {
				BlockPos tg = te.getTargetPos();
				this.breakBlocks(tg);
				this.hurtMobs(tg);
				this.discard();
				return;
			}
		}
		this.setPos(this.getX()+this.getDeltaMovement().x, this.getY()+this.getDeltaMovement().y, this.getZ()+this.getDeltaMovement().z);
	}

	private void hurtMobs(BlockPos p) {
		for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(p).inflate(3)))
			e.hurt(level().damageSources().inWall(), 1);
	}

	private void breakBlocks(BlockPos c) {
		int k = 3;
		for (int a = -k; a <= k; a++) {
			for (int b = -k; b <= k; b++) {
				if (sx != 0)
					this.dropBlockAt(c.offset(0, b, a));
				else if (sz != 0)
					this.dropBlockAt(c.offset(a, b, 0));
				else
					this.dropBlockAt(c.offset(a, 0, b));
			}
		}
		level().playSound(null, c, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4, 1);
		if (level() instanceof ServerLevel sl)
			sl.sendParticles(ParticleTypes.EXPLOSION, c.getX(), c.getY(), c.getZ(), 1, 0, 0, 0, 0);
	}

	/** Original dropBlockAt (never at the world's bottom layer, the original's y = 0). */
	private void dropBlockAt(BlockPos p) {
		if (p.getY() <= level().getMinBuildHeight())
			return;
		BlockState b = level().getBlockState(p);
		if (b.isAir())
			return;
		if (SonicBorerBlockEntity.canDrop(level(), p) || b.getBlock() instanceof LiquidBlock) {
			List<ItemStack> li = Block.getDrops(b, (ServerLevel)level(), p, level().getBlockEntity(p));
			for (ItemStack is : li)
				Containers.dropItemStack(level(), p.getX()+0.5, p.getY()+0.5, p.getZ()+0.5, is);
			level().removeBlock(p, false);
		}
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {}

	@Override
	protected void readAdditionalSaveData(CompoundTag NBT) {}

	@Override
	protected void addAdditionalSaveData(CompoundTag NBT) {}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf data) {
		data.writeBlockPos(borer);
		data.writeByte(sx);
		data.writeByte(sy);
		data.writeByte(sz);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf data) {
		borer = data.readBlockPos();
		sx = data.readByte();
		sy = data.readByte();
		sz = data.readByte();
		this.setDeltaMovement(sx*2.0, sy*2.0, sz*2.0);
	}
}
