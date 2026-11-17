/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The drop processor, written for the port from the original's behaviour: with 1024 W (at least 32 Nm) on its input side it turns a block
 * item into what the block drops when mined (ore blocks give their ores, and so on). One block takes max(1, 300 - 20*log2(speed+1)) ticks,
 * and above 65536 rad/s it does several per tick. The first drop goes to the output slot, the rest wait in an overflow and follow one by
 * one; it only starts the next block when the output and the overflow are empty. Items go into the input from any side, out of the output only.
 * Changes: the drops are rolled as if mined with an unenchanted diamond pickaxe (1.7.10 ignored the tool); the machine enchantments
 * (fortune, efficiency) and the other mods' handlers (crates, scrap boxes, Mystcraft notebooks, GregTech ores) are left out.
 */
public class DropProcessorBlockEntity extends PowerReceiverBlockEntity {

	private static final int BASE = 300, SCALE = 20;

	private final ItemStackHandler inv = new ItemStackHandler(2) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return slot == 1 || isProcessable(is);
		}

		@Override
		protected void onContentsChanged(int slot) {
			DropProcessorBlockEntity.this.setChanged();
		}
	};
	private final List<ItemStack> overflow = new ArrayList<>();
	public int dropProcessTime;
	public int processed;

	public DropProcessorBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.DROP_PROCESSOR.get(), pos, state, 32, 1, 1024);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public int getOverflowCount() {
		return overflow.size();
	}

	public static boolean isProcessable(ItemStack is) {
		return is.getItem() instanceof BlockItem;
	}

	/** Input from any side, output only from the output slot. */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return new net.neoforged.neoforge.items.wrapper.RangedWrapper(inv, 0, 2) {
			@Override
			public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
				return slot == 0 ? super.insertItem(slot, stack, simulate) : stack;
			}

			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				return slot == 1 ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
			}
		};
	}

	public int getOperationTime() {
		return (int)Math.max(1, BASE-SCALE*ReikaMathLibrary.logbase(Math.max(0, omega)+1, 2));
	}

	public int getNumberConsecutiveOperations() {
		int threshold = 1 << (1+BASE/SCALE);
		return 1+(omega >= threshold ? (int)(ReikaMathLibrary.logbase(omega, 2)-BASE/SCALE) : 0);
	}

	public int getProgressScaled(int px) {
		return dropProcessTime*px/this.getOperationTime();
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		this.getIOSidesDefault(this.getBlockMetadata());
		this.readPower();
		if (!overflow.isEmpty() && inv.getStackInSlot(1).isEmpty()) {
			inv.setStackInSlot(1, overflow.remove(0));
			return;
		}
		if (power < MINPOWER || torque < MINTORQUE) {
			dropProcessTime = 0;
			return;
		}
		int n = this.getNumberConsecutiveOperations();
		for (int i = 0; i < n; i++)
			this.operate((ServerLevel)world, pos, n > 1);
	}

	private void operate(ServerLevel world, BlockPos pos, boolean multiple) {
		ItemStack in = inv.getStackInSlot(0);
		if (in.isEmpty() || !isProcessable(in) || !inv.getStackInSlot(1).isEmpty() || !overflow.isEmpty()) {
			dropProcessTime = 0;
			return;
		}
		dropProcessTime++;
		if (!multiple && dropProcessTime < this.getOperationTime())
			return;
		dropProcessTime = 0;
		List<ItemStack> drops = getDrops(world, pos, in);
		inv.extractItem(0, 1, false);
		processed++;
		drops.removeIf(ItemStack::isEmpty);
		if (!drops.isEmpty()) {
			inv.setStackInSlot(1, drops.remove(0));
			overflow.addAll(drops);
		}
		this.setChanged();
	}

	/** The block's loot as if mined with an unenchanted diamond pickaxe. */
	public static List<ItemStack> getDrops(ServerLevel world, BlockPos pos, ItemStack block) {
		BlockState st = ((BlockItem)block.getItem()).getBlock().defaultBlockState();
		return new ArrayList<>(Block.getDrops(st, world, pos, null, null, new ItemStack(Items.DIAMOND_PICKAXE)));
	}

	@Override
	public int getRedstoneOverride() {
		return inv.getStackInSlot(0).isEmpty() ? 15 : 0;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < 2; i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
		for (ItemStack is : overflow)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), is.copy());
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("inv", inv.serializeNBT(registries));
		NBT.putInt("time", dropProcessTime);
		ListTag li = new ListTag();
		for (ItemStack is : overflow)
			li.add(is.save(registries));
		NBT.put("overflow", li);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		inv.deserializeNBT(registries, NBT.getCompound("inv"));
		dropProcessTime = NBT.getInt("time");
		overflow.clear();
		for (Tag t : NBT.getList("overflow", Tag.TAG_COMPOUND))
			ItemStack.parse(registries, t).ifPresent(overflow::add);
	}
}
