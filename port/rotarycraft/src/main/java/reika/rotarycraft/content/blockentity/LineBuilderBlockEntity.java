/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotarySounds;

/**
 * Original: Reika.RotaryCraft.TileEntities.World.TileEntityLineBuilder - a ram fed with blocks from its 9 slots. Needs 131072 W and
 * 1024 Nm from behind; every max(3, 40 - 2*log2(omega+1)) ticks it pushes the line of blocks in front of it (up to 64) one block on and
 * puts the next block from its inventory into the gap. It stops at bedrock and other unbreakable blocks, at blocks with block entities and when the spot after the
 * line is not free (air or a replaceable block). Not ported: protection checks for the placing player, other mods' immovable blocks, the
 * range config.
 */
public class LineBuilderBlockEntity extends PowerReceiverBlockEntity implements net.minecraft.world.Clearable {

	public static final int MAXRANGE = 64;

	private Direction dir = Direction.WEST;
	private int timer;

	private final ItemStackHandler inv = new ItemStackHandler(9) {
		@Override
		protected void onContentsChanged(int slot) {
			LineBuilderBlockEntity.this.setChanged();
		}
	};

	public LineBuilderBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.LINE_BUILDER.get(), pos, state, 1024, 1, 131072);
	}

	@Override
	public String getName() {
		return "Line Builder";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public Direction getPushDirection() {
		return dir;
	}

	/** Original DurationRegistry.RAM.getOperationTime, at least 3. */
	public int getOperationTime() {
		return Math.max(3, (int)Math.max(1, 40-2*ReikaMathLibrary.logbase(Math.max(0, omega)+1, 2)));
	}

	private void updateSides(int meta) {
		switch (meta) {
			case 0 -> { read = Direction.EAST; dir = Direction.WEST; }
			case 1 -> { read = Direction.WEST; dir = Direction.EAST; }
			case 2 -> { read = Direction.SOUTH; dir = Direction.NORTH; }
			case 3 -> { read = Direction.NORTH; dir = Direction.SOUTH; }
			case 4 -> { read = Direction.DOWN; dir = Direction.UP; }
			case 5 -> { read = Direction.UP; dir = Direction.DOWN; }
		}
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.updateSides(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		this.readPower();
		if (power >= MINPOWER && torque >= MINTORQUE) {
			timer++;
			if (timer >= this.getOperationTime()) {
				timer = 0;
				this.shiftBlocks(world, pos);
			}
		}
	}

	private boolean shiftBlocks(Level world, BlockPos pos) {
		RotarySounds.play(world, pos, RotarySounds.LINEBUILDER.get(), 1, 1);
		if (!this.canShift(world))
			return false;
		int slot = this.getNextBlockSlot();
		if (slot < 0)
			return false;
		BlockState add = ((BlockItem)inv.getStackInSlot(slot).getItem()).getBlock().defaultBlockState();
		int r = this.getLineLength();
		for (int i = r; i > 0; i--) {
			BlockPos from = pos.relative(dir, i);
			world.setBlock(pos.relative(dir, i+1), world.getBlockState(from), 3);
			world.setBlock(from, Blocks.AIR.defaultBlockState(), 3);
		}
		inv.extractItem(slot, 1, false);
		world.setBlock(pos.relative(dir), add, 3);
		RotarySounds.play(world, pos.relative(dir), RotarySounds.LINEBUILDER.get(), 1, 1);
		return true;
	}

	/** Original ReikaInventoryHelper.getNextBlockInInventory: the first slot holding a block. */
	private int getNextBlockSlot() {
		for (int i = 0; i < inv.getSlots(); i++)
			if (inv.getStackInSlot(i).getItem() instanceof BlockItem)
				return i;
		return -1;
	}

	private static boolean isSoft(Level world, BlockPos p) {
		BlockState s = world.getBlockState(p);
		return s.isAir() || s.canBeReplaced();
	}

	public boolean canShift(Level world) {
		int r = this.getLineLength()+1;
		return r > 0 && r <= MAXRANGE && isSoft(world, worldPosition.relative(dir, r));
	}

	/** Original getLineLength: the number of solid blocks in front of it, or Integer.MIN_VALUE when it may not push them. */
	public int getLineLength() {
		int i = 1;
		BlockPos p = worldPosition.relative(dir, i);
		if (this.isImmovable(p))
			return Integer.MIN_VALUE;
		while (!isSoft(level, p) && i <= MAXRANGE) {
			p = worldPosition.relative(dir, ++i);
			if (this.isImmovable(p))
				return Integer.MIN_VALUE;
		}
		return i-1;
	}

	/**
	 * Bedrock and blocks with block entities as in the original; also any other unbreakable block (barriers, portal frames), which the
	 * original kept still through its check that the placing player may break the block.
	 */
	private boolean isImmovable(BlockPos p) {
		BlockState s = level.getBlockState(p);
		return s.is(Blocks.BEDROCK) || s.getDestroySpeed(level, p) < 0 || level.getBlockEntity(p) != null;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	/** Items in from any side; nothing comes out (original canExtractItem: false). */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return new IItemHandler() {
			@Override
			public int getSlots() {
				return inv.getSlots();
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
				return 64;
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return true;
			}
		};
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < inv.getSlots(); i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}

	@Override
	public void clearContent() {
		for (int i = 0; i < inv.getSlots(); i++)
			inv.setStackInSlot(i, ItemStack.EMPTY);
	}

	@Override
	protected void writeSyncTag(net.minecraft.nbt.CompoundTag NBT, net.minecraft.core.HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("timer", timer);
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(net.minecraft.nbt.CompoundTag NBT, net.minecraft.core.HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		timer = NBT.getInt("timer");
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
