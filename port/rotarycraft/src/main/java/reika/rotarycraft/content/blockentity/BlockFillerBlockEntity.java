/*******************************************************************************
 * Block filler: sets the blocks from its slot into the hollow below it. Ported from Reika.RotaryCraft.TileEntities.World.TileEntityBlockFiller
 * (RotaryCraft 1.7.10 by Reika Kalseki); private NeoForge 1.21.1 port, not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Containers;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.registry.RotaryBlockEntities;

public class BlockFillerBlockEntity extends AreaFillerBlockEntity {

	private final ItemStackHandler inv = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return getBlock(is) != null;
		}

		@Override
		protected void onContentsChanged(int slot) {
			BlockFillerBlockEntity.this.setChanged();
		}
	};

	public BlockFillerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.FILLER.get(), pos, state, 1024);
	}

	@Override
	public String getName() {
		return "Block Filler";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	/** The block an item places (original: Block.getBlockFromItem with the item's metadata), or null. */
	@Nullable
	private static BlockState getBlock(ItemStack is) {
		if (is.isEmpty() || !(is.getItem() instanceof BlockItem bi))
			return null;
		BlockState b = bi.getBlock().defaultBlockState();
		return b.isAir() ? null : b;
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	protected boolean hasRemainingBlocks() {
		return !inv.getStackInSlot(0).isEmpty();
	}

	@Override
	protected void onBlockPlaced() {
		inv.extractItem(0, 1, false);
	}

	@Nullable
	@Override
	protected BlockState getNextPlacedBlock() {
		return getBlock(inv.getStackInSlot(0));
	}

	/**
	 * Original: 512 W for non-solid materials, 4096 W for Material.iron / anvil, 2048 W for Material.rock, 1024 W for everything else.
	 * 1.21 has no materials: metal is recognised by its sound, rock by the pickaxe tag.
	 */
	@Override
	public long getRequiredPower() {
		BlockState b = this.getNextPlacedBlock();
		if (b == null)
			return MINPOWER;
		if (!b.blocksMotion())
			return 512;
		SoundType s = b.getSoundType();
		if (s == SoundType.METAL || s == SoundType.ANVIL || s == SoundType.NETHERITE_BLOCK || s == SoundType.COPPER)
			return 4096;
		return b.is(BlockTags.MINEABLE_WITH_PICKAXE) ? 2048 : 1024;
	}

	@Override
	protected boolean allowFluidOverwrite() {
		return true;
	}

	/** Blocks in from any side, nothing out (original canExtractItem: false). */
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
				return ItemStack.EMPTY;
			}

			@Override
			public int getSlotLimit(int slot) {
				return inv.getSlotLimit(slot);
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
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
