/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.fusion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.registry.ReactorEntities;
import reika.reactorcraft.registry.ReactorSounds;

/**
 * Original: Reika.ReactorCraft.Entities.EntityFusion - the flash of a fusion event: when made it sends out 3 fusion neutrons in random
 * horizontal directions and plays the fusion sound; it is gone after 5 ticks.
 */
public class FusionEntity extends Entity {

	public FusionEntity(EntityType<? extends FusionEntity> type, Level world) {
		super(type, world);
		noPhysics = true;
	}

	public FusionEntity(Level world, double x, double y, double z, String creator) {
		this(ReactorEntities.FUSION.get(), world);
		this.setPos(x, y, z);
		if (!world.isClientSide) {
			BlockPos p = BlockPos.containing(x, y, z);
			for (int i = 0; i < 3; i++)
				world.addFreshEntity(new NeutronEntity(world, p, this.getRandomDirection(), NeutronEntity.NeutronType.FUSION));
			ReactorSounds.play(world, p, ReactorSounds.FUSION.get(), 1, 1);
		}
	}

	public Direction getRandomDirection() {
		return Direction.values()[2+random.nextInt(4)];
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder b) {}

	@Override
	public void tick() {
		super.tick();
		if (tickCount > 5 && !level().isClientSide)
			this.discard();
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isInvulnerable() {
		return true;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag NBT) {}

	@Override
	protected void addAdditionalSaveData(CompoundTag NBT) {}
}
