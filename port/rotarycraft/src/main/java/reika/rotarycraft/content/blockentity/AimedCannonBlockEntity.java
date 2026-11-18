/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Base of the TNT cannon and the block cannon, written for the port: power from below, 9 ammo slots, a target set in the GUI. Every
 * {@link #getOperationTime()} ticks it fires one round at the target with the lowest launch speed (at 45 degrees) whose arc, under Minecraft's
 * gravity (0.04 blocks/tick^2) and drag (2 % a tick), reaches the target's distance at the target's height - if that speed is within what its
 * power allows.
 */
public abstract class AimedCannonBlockEntity extends PowerReceiverBlockEntity {

	public static final double GRAVITY = 0.04, DRAG = 0.98;

	protected final ItemStackHandler inv = new ItemStackHandler(9) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return AimedCannonBlockEntity.this.isAmmo(is);
		}

		@Override
		protected void onContentsChanged(int slot) {
			AimedCannonBlockEntity.this.setChanged();
		}
	};
	@Nullable
	private BlockPos target;
	public int shots;

	protected AimedCannonBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, long minPower) {
		super(type, pos, state, 1, 1, minPower);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public IItemHandler getItemHandler(@Nullable Direction side) {
		return inv;
	}

	@Nullable
	public BlockPos getTarget() {
		return target;
	}

	public void setTarget(@Nullable BlockPos t) {
		target = t;
		this.syncAllData();
	}

	public abstract boolean isAmmo(ItemStack is);

	/** Highest launch speed (blocks/tick) the current power allows. */
	public abstract double getMaxSpeed();

	public int getOperationTime() {
		return 20;
	}

	/** Fires the ammo in that slot from start with that velocity; true if it fired. */
	protected abstract boolean fire(Level world, int slot, Vec3 start, Vec3 velocity);

	/** Horizontal distance a projectile launched at 45 degrees with that speed has covered when it comes down to height dy. */
	public static double reach(double speed, double dy) {
		double vh = speed*Math.sqrt(0.5), vy = vh, x = 0, y = 0;
		for (int t = 0; t < 600; t++) {
			vy -= GRAVITY;
			x += vh;
			y += vy;
			vh *= DRAG;
			vy *= DRAG;
			if (vy < 0 && y <= dy)
				return x;
		}
		return x;
	}

	/** The lowest 45-degree launch speed reaching (dist, dy), or -1 if none up to 20 blocks a tick. */
	public static double speedFor(double dist, double dy) {
		double lo = 0, hi = 20;
		if (reach(hi, dy) < dist)
			return -1;
		for (int i = 0; i < 40; i++) {
			double mid = (lo+hi)/2;
			if (reach(mid, dy) >= dist)
				hi = mid;
			else
				lo = mid;
		}
		return hi;
	}

	/** The launch velocity towards the target, or null if out of reach. */
	@Nullable
	public Vec3 aim() {
		if (target == null)
			return null;
		Vec3 start = Vec3.atCenterOf(worldPosition).add(0, 1, 0);
		Vec3 to = Vec3.atBottomCenterOf(target).subtract(start);
		double dist = Math.sqrt(to.x*to.x+to.z*to.z);
		if (dist < 1)
			return null;
		double s = speedFor(dist, to.y);
		if (s < 0 || s > this.getMaxSpeed())
			return null;
		double vh = s*Math.sqrt(0.5);
		return new Vec3(to.x/dist*vh, vh, to.z/dist*vh);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		read = Direction.DOWN;
		if (world.isClientSide)
			return;
		this.readPower();
		if (power < MINPOWER || this.getTicksExisted()%this.getOperationTime() != 0)
			return;
		Vec3 v = this.aim();
		if (v == null)
			return;
		for (int i = 0; i < inv.getSlots(); i++)
			if (!inv.getStackInSlot(i).isEmpty() && this.isAmmo(inv.getStackInSlot(i))) {
				if (this.fire(world, i, Vec3.atCenterOf(pos).add(0, 1, 0), v)) {
					inv.extractItem(i, 1, false);
					shots++;
				}
				return;
			}
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
		for (int i = 0; i < inv.getSlots(); i++)
			if (!inv.getStackInSlot(i).isEmpty())
				return 0;
		return 15;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("inv", inv.serializeNBT(registries));
		if (target != null)
			NBT.put("target", NbtUtils.writeBlockPos(target));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		inv.deserializeNBT(registries, NBT.getCompound("inv"));
		target = NbtUtils.readBlockPos(NBT, "target").orElse(null);
	}
}
