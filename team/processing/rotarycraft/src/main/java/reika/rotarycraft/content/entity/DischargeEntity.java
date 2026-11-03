/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

import reika.rotarycraft.registry.RotaryEntities;

/**
 * Original: Reika.RotaryCraft.Entities.EntityDischarge - the visible bolt of a Van de Graaff discharge, from the generator to its target;
 * it lives for two ticks. Its colour follows the current (charge * 20 / 1000 A).
 */
public class DischargeEntity extends Entity implements IEntityWithComplexSpawn {

	public int charge;
	public double targetX;
	public double targetY;
	public double targetZ;

	public DischargeEntity(EntityType<? extends DischargeEntity> type, Level world) {
		super(type, world);
	}

	public DischargeEntity(Level world, double x, double y, double z, int charge, double tx, double ty, double tz) {
		this(RotaryEntities.DISCHARGE.get(), world);
		this.setPos(x, y, z);
		this.charge = charge;
		targetX = tx;
		targetY = ty;
		targetZ = tz;
	}

	public int getCurrent() {
		return charge*20/1000; //1A = 1C/s, 1t = 1/20 s
	}

	/** Original getColor (RGB). */
	public int getColor() {
		int a = this.getCurrent();
		if (a > 120)
			return 0x7F00FF;
		if (a > 90)
			return 0x00C0FF;
		if (a > 70)
			return 0xFFFFFF;
		if (a > 50)
			return 0xFFFF00;
		return 0x000000;
	}

	@Override
	public void tick() {
		super.tick();
		if (tickCount > 1)
			this.discard();
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
		data.writeDouble(targetX);
		data.writeDouble(targetY);
		data.writeDouble(targetZ);
		data.writeInt(charge);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf data) {
		targetX = data.readDouble();
		targetY = data.readDouble();
		targetZ = data.readDouble();
		charge = data.readInt();
	}

	@Override
	public boolean displayFireAnimation() {
		return false;
	}
}
