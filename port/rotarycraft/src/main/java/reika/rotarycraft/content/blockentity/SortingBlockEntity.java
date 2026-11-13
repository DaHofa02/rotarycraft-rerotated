/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import reika.rotarycraft.content.block.SortingBlock;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.TileEntitySorting - takes items from the inventory above it, or items lying on top of it,
 * and sends each to the side its GUI maps it to: the 27 mapping slots are three rows of nine, one row for each horizontal side but the
 * input (in the order north, south, west, east, without the input side); unmapped items go down. Needs 1024 W from its input side
 * (metadata 0 east, 1 west, 2 north, 3 south); 1, 2, 4, 16, 32 or 64 items per tick at 1, 2, 4, 8, 12 or 16 times that. An inventory on
 * the target side takes the item, otherwise it is thrown out that way.
 */
public class SortingBlockEntity extends PowerReceiverBlockEntity {

	public static final int LENGTH = 9;

	private final ItemStack[] mappings = new ItemStack[27];

	public SortingBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.SORTING.get(), pos, state, 1, 1, 1024);
		java.util.Arrays.fill(mappings, ItemStack.EMPTY);
	}

	@Override
	public String getName() {
		return "Item Sorter";
	}

	public static Direction facingFromMeta(int meta) {
		return switch (meta) {
			case 1 -> Direction.WEST;
			case 2 -> Direction.NORTH;
			case 3 -> Direction.SOUTH;
			default -> Direction.EAST;
		};
	}

	public Direction getFacing() {
		return facingFromMeta(this.getBlockMetadata());
	}

	@Override
	public void setBlockMetadata(int meta) {
		super.setBlockMetadata(meta);
		if (level != null && !level.isClientSide) {
			BlockState bs = this.getBlockState();
			if (bs.hasProperty(SortingBlock.FACING))
				level.setBlock(worldPosition, bs.setValue(SortingBlock.FACING, facingFromMeta(meta)), 3);
		}
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		read = this.getFacing();
		if (world.isClientSide)
			return;
		this.readPower();
		if (power < MINPOWER)
			return;
		int n = this.getNumberCyclesPerTick();
		IItemHandler above = world.getCapability(Capabilities.ItemHandler.BLOCK, pos.above(), Direction.DOWN);
		if (above != null) {
			for (int i = 0; i < above.getSlots(); i++)
				for (int k = 0; k < n; k++) {
					ItemStack one = above.extractItem(i, 1, false);
					if (one.isEmpty())
						break;
					this.dumpItem(world, pos, one, this.getSideForItem(one));
				}
		}
		else {
			AABB box = new AABB(pos.getX(), pos.getY()+1, pos.getZ(), pos.getX()+1, pos.getY()+1.25, pos.getZ()+1);
			for (ItemEntity ei : world.getEntitiesOfClass(ItemEntity.class, box)) {
				ItemStack is = ei.getItem();
				for (int k = 0; k < n && !is.isEmpty() && ei.isAlive(); k++) {
					ItemStack one = is.split(1);
					if (is.isEmpty())
						ei.discard();
					else
						ei.setItem(is);
					this.dumpItem(world, pos, one, this.getSideForItem(one));
				}
			}
		}
	}

	public int getNumberCyclesPerTick() {
		long frac = power/MINPOWER;
		if (frac >= 16)
			return 64;
		if (frac >= 12)
			return 32;
		if (frac >= 8)
			return 16;
		if (frac >= 4)
			return 4;
		return frac >= 2 ? 2 : 1;
	}

	private void dumpItem(Level world, BlockPos pos, ItemStack drop, Direction dir) {
		IItemHandler target = world.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(dir), dir.getOpposite());
		if (target != null)
			drop = ItemHandlerHelper.insertItemStacked(target, drop, false);
		if (drop.isEmpty())
			return;
		ItemEntity e = new ItemEntity(world, pos.getX()+0.5+dir.getStepX()*0.75, pos.getY()+0.5+dir.getStepY()*0.75, pos.getZ()+0.5+dir.getStepZ()*0.75, drop);
		e.setDeltaMovement(dir.getStepX()*0.1, dir.getStepY()*0.1, dir.getStepZ()*0.1);
		world.addFreshEntity(e);
	}

	/** Original getSideForItem: the first mapping of the same item (1.21 items have no damage subtypes). */
	public Direction getSideForItem(ItemStack is) {
		for (int k = 0; k < mappings.length; k++)
			if (!mappings[k].isEmpty() && ItemStack.isSameItem(mappings[k], is))
				return this.getDirection(k);
		return Direction.DOWN;
	}

	/** The side row index/9 sends to: north, south, west, east without the input side. */
	public Direction getDirection(int index) {
		List<Direction> li = new ArrayList<>(List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST));
		li.remove(this.getFacing());
		return li.get(index/LENGTH);
	}

	public ItemStack getMapping(int index) {
		return mappings[index];
	}

	/** Original setMapping: an empty stack clears the slot; an item already mapped elsewhere is refused. */
	public boolean setMapping(int index, ItemStack item) {
		if (item.isEmpty()) {
			mappings[index] = ItemStack.EMPTY;
		}
		else {
			for (ItemStack is : mappings)
				if (!is.isEmpty() && ItemStack.isSameItemSameComponents(is, item))
					return false;
			mappings[index] = item.copyWithCount(1);
		}
		this.syncAllData();
		this.setChanged();
		return true;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		ListTag li = new ListTag();
		for (int i = 0; i < mappings.length; i++) {
			if (!mappings[i].isEmpty()) {
				CompoundTag tag = (CompoundTag)mappings[i].save(registries);
				tag.putByte("Slot", (byte)i);
				li.add(tag);
			}
		}
		NBT.put("Items", li);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		java.util.Arrays.fill(mappings, ItemStack.EMPTY);
		ListTag li = NBT.getList("Items", 10);
		for (int i = 0; i < li.size(); i++) {
			CompoundTag tag = li.getCompound(i);
			int s = tag.getByte("Slot");
			if (s >= 0 && s < mappings.length)
				mappings[s] = ItemStack.parseOptional(registries, tag);
		}
	}
}
