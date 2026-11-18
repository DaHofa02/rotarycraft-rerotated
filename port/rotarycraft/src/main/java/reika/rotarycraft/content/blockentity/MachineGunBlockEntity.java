/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The machine gun, written for the port from the original's behaviour: with 65536 W at 1024 rad/s or more into its back it fires an arrow
 * from its 27 ammo slots straight ahead every max(4, 16 - log2(speed+1)) ticks, at log2(torque+1)/2 blocks a tick, while something living
 * (other than its owner) is in the 10 + 2*log2(torque+1) blocks in front of it. Its arrows cannot be picked up.
 * Changes: the arrow speed is halved (the original's full log2 value made arrows pass through targets in 1.21).
 */
public class MachineGunBlockEntity extends PowerReceiverBlockEntity {

	private final ItemStackHandler inv = new ItemStackHandler(27) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return is.is(Items.ARROW);
		}

		@Override
		protected void onContentsChanged(int slot) {
			MachineGunBlockEntity.this.setChanged();
		}
	};
	public int shots;

	public MachineGunBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.MACHINE_GUN.get(), pos, state, 1, 1024, 65536);
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

	public Direction getFacing() {
		return read == null ? Direction.WEST : read.getOpposite();
	}

	public int getRange() {
		return 10+2*(int)ReikaMathLibrary.logbase(torque+1, 2);
	}

	public int getOperationTime() {
		return Math.max(16-(int)ReikaMathLibrary.logbase(omega+1, 2), 4);
	}

	public double getArrowSpeed() {
		return ReikaMathLibrary.logbase(torque+1, 2)/2;
	}

	public AABB getArea() {
		Direction d = this.getFacing();
		return new AABB(worldPosition.relative(d)).minmax(new AABB(worldPosition.relative(d, this.getRange())));
	}

	private int findArrow() {
		for (int i = 0; i < inv.getSlots(); i++)
			if (inv.getStackInSlot(i).is(Items.ARROW))
				return i;
		return -1;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSidesDefault(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		this.readPower();
		if (power < MINPOWER || omega < MINSPEED || this.getTicksExisted()%this.getOperationTime() != 0)
			return;
		int slot = this.findArrow();
		if (slot < 0)
			return;
		boolean target = !world.getEntitiesOfClass(LivingEntity.class, this.getArea(),
				e -> e.isAlive() && !(e instanceof Player p && p.getUUID().equals(this.getPlacerUUID()))).isEmpty();
		if (!target)
			return;
		Direction d = this.getFacing();
		Arrow a = new Arrow(world, pos.getX()+0.5+d.getStepX()*0.8, pos.getY()+0.5, pos.getZ()+0.5+d.getStepZ()*0.8, new ItemStack(Items.ARROW), null);
		a.pickup = AbstractArrow.Pickup.DISALLOWED;
		double v = this.getArrowSpeed();
		a.shoot(d.getStepX(), 0, d.getStepZ(), (float)v, 0);
		world.addFreshEntity(a);
		inv.extractItem(slot, 1, false);
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
		return this.findArrow() < 0 ? 15 : 0;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
