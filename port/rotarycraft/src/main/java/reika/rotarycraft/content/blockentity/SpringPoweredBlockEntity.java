/*******************************************************************************
 * Base of the machines that run on a wound coil in their one slot. Ported from Reika.RotaryCraft.Base.TileEntity.TileEntitySpringPowered
 * (RotaryCraft 1.7.10 by Reika Kalseki); private NeoForge 1.21.1 port, not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.api.interfaces.TensionStorage;
import reika.rotarycraft.content.item.CoilItem;

public abstract class SpringPoweredBlockEntity extends RotaryCraftBlockEntity {

	/** Original: set by the debug item; the machine then needs no coil and uses none up. */
	public boolean isCreativeMode;

	private final ItemStackHandler inv = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return is.getItem() instanceof TensionStorage;
		}

		@Override
		public int getSlotLimit(int slot) {
			return 1;
		}

		@Override
		protected void onContentsChanged(int slot) {
			SpringPoweredBlockEntity.this.setChanged();
		}
	};

	protected SpringPoweredBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** Ticks one unit of coil charge lasts, for a spring of stiffness 1. */
	public abstract int getBaseDischargeTime();

	public ItemStackHandler getInventory() {
		return inv;
	}

	public int getCoilCharge() {
		return CoilItem.getCharge(inv.getStackInSlot(0));
	}

	protected final int getUnwindTime() {
		ItemStack is = inv.getStackInSlot(0);
		if (isCreativeMode || !(is.getItem() instanceof TensionStorage ts))
			return Integer.MAX_VALUE;
		return this.getBaseDischargeTime()*ts.getStiffness(is);
	}

	public int getExpectedCoilLife() {
		return this.getUnwindTime()*this.getCoilCharge();
	}

	/** Uses up one unit of charge (original getDecrementedCharged). */
	protected final void decrementCharge() {
		if (isCreativeMode)
			return;
		ItemStack is = inv.getStackInSlot(0);
		inv.setStackInSlot(0, CoilItem.withCharge(is, CoilItem.getCharge(is)-1));
	}

	protected final boolean hasCoil() {
		if (isCreativeMode)
			return true;
		ItemStack is = inv.getStackInSlot(0);
		return !is.isEmpty() && is.getItem() instanceof TensionStorage && CoilItem.getCharge(is) > 0;
	}

	/** Coils in from any side; only unwound ones come out again (original canExtractItem: damage 0). */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return new IItemHandler() {
			@Override
			public int getSlots() {
				return 1;
			}

			@Override
			public ItemStack getStackInSlot(int slot) {
				return inv.getStackInSlot(slot);
			}

			@Override
			public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
				return inv.insertItem(slot, stack, simulate);
			}

			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				return CoilItem.getCharge(inv.getStackInSlot(slot)) == 0 ? inv.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
			}

			@Override
			public int getSlotLimit(int slot) {
				return 1;
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return inv.isItemValid(slot, stack);
			}
		};
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(0).copy());
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("creative", isCreativeMode);
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		isCreativeMode = NBT.getBoolean("creative");
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
