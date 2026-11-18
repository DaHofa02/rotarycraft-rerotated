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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The projector, written for the port: with 512 W into its back it throws the picture of the filled map in its slot onto the first solid
 * face within 12 blocks in front of it, as a square of max(1, distance/2) blocks. Changes: the original showed slides and built-in pictures;
 * the port projects maps (no images come with it).
 */
public class ProjectorBlockEntity extends PowerReceiverBlockEntity {

	public static final int MAXRANGE = 12;

	private final ItemStackHandler inv = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return is.is(Items.FILLED_MAP);
		}

		@Override
		public int getSlotLimit(int slot) {
			return 1;
		}

		@Override
		protected void onContentsChanged(int slot) {
			ProjectorBlockEntity.this.setChanged();
			ProjectorBlockEntity.this.syncAllData();
		}
	};
	private int range;

	public ProjectorBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.PROJECTOR.get(), pos, state, 1, 1, 512);
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

	/** The way the projector looks (away from its input side). */
	public Direction getFacing() {
		this.getIOSidesDefault(this.getBlockMetadata());
		return read == null ? Direction.NORTH : read.getOpposite();
	}

	/** Distance to the screen in blocks, 0 for none. */
	public int getRange() {
		return range;
	}

	public int getImageSize() {
		return Math.max(1, range/2);
	}

	public boolean canShow() {
		return range > 0 && power >= MINPOWER && inv.getStackInSlot(0).is(Items.FILLED_MAP);
	}

	private int findScreen(Level world, BlockPos pos, Direction dir) {
		for (int i = 1; i <= MAXRANGE; i++) {
			BlockPos p = pos.relative(dir, i);
			BlockState s = world.getBlockState(p);
			if (!s.isAir())
				return s.isFaceSturdy(world, p, dir.getOpposite()) ? i : 0;
		}
		return 0;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		Direction dir = this.getFacing();
		if (world.isClientSide)
			return;
		this.readPower();
		int r = this.findScreen(world, pos, dir);
		if (r != range) {
			range = r;
			this.syncAllData();
		}
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(0).copy());
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("inv", inv.serializeNBT(registries));
		NBT.putInt("range", range);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		inv.deserializeNBT(registries, NBT.getCompound("inv"));
		range = NBT.getInt("range");
	}
}
