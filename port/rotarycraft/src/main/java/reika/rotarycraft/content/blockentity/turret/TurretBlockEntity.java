/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity.turret;

import java.util.Comparator;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.blockentity.PowerReceiverBlockEntity;

/**
 * Base of the port's turrets, written from the original's behaviour: power from below; every {@link #getOperationTime()} ticks, if it can
 * fire, it picks the nearest target it can see within its range - hostile mobs, and players other than its owner (unless switched off;
 * creative and spectating players never) - and fires at it. Turrets have 9 ammo slots unless they say otherwise.
 * Changes: shots hit at once (no flying projectile entities) and the barrel does not visibly turn.
 */
public abstract class TurretBlockEntity extends PowerReceiverBlockEntity {

	protected final ItemStackHandler inv = new ItemStackHandler(this.getAmmoSlots()) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return TurretBlockEntity.this.isAmmo(is);
		}

		@Override
		protected void onContentsChanged(int slot) {
			TurretBlockEntity.this.setChanged();
		}
	};
	public boolean targetPlayers = true;
	public int shots;
	@Nullable
	public LivingEntity lastTarget;

	protected TurretBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int minTorque, int minSpeed, long minPower) {
		super(type, pos, state, minTorque, minSpeed, minPower);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	protected int getAmmoSlots() {
		return 9;
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	@Nullable
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return inv;
	}

	public abstract int getRange();

	public abstract int getOperationTime();

	public abstract boolean isAmmo(ItemStack is);

	/** True if it may fire now (ammo, fuel). */
	protected abstract boolean canFire();

	/** Fires at the target from the muzzle. */
	protected abstract void fire(Level world, Vec3 muzzle, LivingEntity target);

	protected int findAmmo() {
		for (int i = 0; i < inv.getSlots(); i++)
			if (!inv.getStackInSlot(i).isEmpty() && this.isAmmo(inv.getStackInSlot(i)))
				return i;
		return -1;
	}

	public boolean isValidTarget(LivingEntity e) {
		if (!e.isAlive())
			return false;
		if (e instanceof Player p)
			return targetPlayers && !p.isCreative() && !p.isSpectator() && !p.getUUID().equals(this.getPlacerUUID());
		return e instanceof Enemy;
	}

	public Vec3 muzzle() {
		return Vec3.atCenterOf(worldPosition).add(0, 0.75, 0);
	}

	public boolean canSee(Level world, LivingEntity e) {
		Vec3 from = this.muzzle(), to = e.getEyePosition();
		return world.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)).getType() == HitResult.Type.MISS;
	}

	public Optional<LivingEntity> findTarget(Level world) {
		int r = this.getRange();
		Vec3 m = this.muzzle();
		return world.getEntitiesOfClass(LivingEntity.class, new AABB(worldPosition).inflate(r), e -> this.isValidTarget(e)
				&& e.position().distanceTo(m) <= r && this.canSee(world, e)).stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(m)));
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		read = Direction.DOWN;
		if (world.isClientSide)
			return;
		this.readPower();
		if (power < MINPOWER || torque < MINTORQUE || omega < MINSPEED || this.getTicksExisted()%this.getOperationTime() != 0 || !this.canFire())
			return;
		Optional<LivingEntity> t = this.findTarget(world);
		if (t.isEmpty())
			return;
		lastTarget = t.get();
		this.fire(world, this.muzzle(), t.get());
		shots++;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < inv.getSlots(); i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return this.canFire() ? 0 : 15;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("inv", inv.serializeNBT(registries));
		NBT.putBoolean("players", targetPlayers);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		inv.deserializeNBT(registries, NBT.getCompound("inv"));
		targetPlayers = !NBT.contains("players") || NBT.getBoolean("players");
	}
}
