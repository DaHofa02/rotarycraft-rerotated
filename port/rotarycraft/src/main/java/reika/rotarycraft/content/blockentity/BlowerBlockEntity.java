/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import reika.rotarycraft.content.block.BlowerBlock;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.TileEntityBlower (the item pump) - with at least 1024 W and 256 rad/s from any side it moves
 * up to power/1024 items a tick from the inventory behind it to the inventory in front of it (metadata 0 east, 1 west, 2 south, 3 north,
 * 4 down, 5 up); a line of blowers passes the items on to the inventory at its end, and with nothing there (air) they are spilled out.
 * Its GUI holds 18 filter items (blacklist, or whitelist) and switches for metadata (damage), NBT (components) and ore dictionary
 * (shared c: tags). Not ported: Applied Energistics patterns, deep storage units, world rifts.
 */
public class BlowerBlockEntity extends PowerReceiverBlockEntity {

	public final ItemStack[] matchingItems = new ItemStack[18];
	public boolean isWhitelist = false;
	public boolean useOreDict = true;
	public boolean checkMeta = false;
	public boolean checkNBT = false;

	public BlowerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.BLOWER.get(), pos, state, 1, 256, 1024);
		java.util.Arrays.fill(matchingItems, ItemStack.EMPTY);
	}

	@Override
	public String getName() {
		return "Item Pump";
	}

	public static Direction facingFromMeta(int meta) {
		return switch (meta) {
			case 1 -> Direction.WEST;
			case 2 -> Direction.SOUTH;
			case 3 -> Direction.NORTH;
			case 4 -> Direction.DOWN;
			case 5 -> Direction.UP;
			default -> Direction.EAST;
		};
	}

	public Direction getFacingDir() {
		return facingFromMeta(this.getBlockMetadata());
	}

	@Override
	public void setBlockMetadata(int meta) {
		super.setBlockMetadata(meta);
		if (level != null && !level.isClientSide && this.getBlockState().hasProperty(BlowerBlock.FACING))
			level.setBlock(worldPosition, this.getBlockState().setValue(BlowerBlock.FACING, facingFromMeta(meta)), 3);
	}

	public int getNumberTransferrableItems() {
		return (int)(power/1024);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		this.readPowerSummative();
		if (power < MINPOWER || omega < MINSPEED)
			return;
		Direction dir = this.getFacingDir();
		IItemHandler source = world.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(dir.getOpposite()), dir);
		if (source == null || world.getBlockEntity(pos.relative(dir.getOpposite())) instanceof BlowerBlockEntity)
			return;
		BlockPos tg = pos.relative(dir);
		Set<BlockPos> seen = new HashSet<>();
		while (world.getBlockEntity(tg) instanceof BlowerBlockEntity b) {
			if (!seen.add(tg) || tg.equals(pos))
				return;
			dir = b.getFacingDir();
			tg = tg.relative(dir);
		}
		if (tg.equals(pos.relative(this.getFacingDir().getOpposite())))
			return;
		IItemHandler target = world.getCapability(Capabilities.ItemHandler.BLOCK, tg, dir.getOpposite());
		BlockEntity tte = world.getBlockEntity(tg);
		int max = this.getNumberTransferrableItems();
		for (int slot = 0; slot < source.getSlots() && max > 0; slot++) {
			ItemStack is = source.extractItem(slot, max, true);
			if (is.isEmpty() || !this.isItemTransferrable(is))
				continue;
			if (target != null) {
				ItemStack left = ItemHandlerHelper.insertItemStacked(target, is, true);
				int n = is.getCount()-left.getCount();
				if (n > 0) {
					ItemHandlerHelper.insertItemStacked(target, source.extractItem(slot, n, false), false);
					max -= n;
				}
			}
			else if (tte == null && world.getBlockState(tg).isAir()) {
				ItemStack out = source.extractItem(slot, is.getCount(), false);
				for (int i = 0; i < out.getCount(); i++) {
					ItemEntity ei = new ItemEntity(world, tg.getX()+0.5, tg.getY()+0.5, tg.getZ()+0.5, out.copyWithCount(1));
					double v = (2+rand.nextDouble()*4)/20D;
					ei.setDeltaMovement(dir.getStepX()*v, dir.getStepY()*v, dir.getStepZ()*v);
					world.addFreshEntity(ei);
				}
				max -= out.getCount();
			}
		}
	}

	public boolean isItemTransferrable(ItemStack is) {
		boolean match = false;
		for (ItemStack f : matchingItems)
			if (!f.isEmpty() && this.doStacksMatch(is, f))
				match = true;
		return isWhitelist == match;
	}

	/** Original doStacksMatch (which in the end matches any stack of the same item unless metadata or NBT checks fail). */
	private boolean doStacksMatch(ItemStack is, ItemStack is1) {
		if (checkMeta && is.getDamageValue() != is1.getDamageValue())
			return false;
		if (checkNBT && !ItemStack.isSameItemSameComponents(is, is1))
			return false;
		if (is.is(is1.getItem()))
			return true;
		return useOreDict && is.getTags().anyMatch(t -> t.location().getNamespace().equals("c") && is1.is(t));
	}

	/** GUI buttons 0-3: whitelist, metadata, NBT, ore dictionary (original BLOWER* packets). */
	public void toggle(int id) {
		switch (id) {
			case 0 -> isWhitelist = !isWhitelist;
			case 1 -> checkMeta = !checkMeta;
			case 2 -> checkNBT = !checkNBT;
			case 3 -> useOreDict = !useOreDict;
		}
		this.syncAllData();
		this.setChanged();
	}

	public void setFilter(int slot, ItemStack is) {
		matchingItems[slot] = is.isEmpty() ? ItemStack.EMPTY : is.copyWithCount(1);
		this.syncAllData();
		this.setChanged();
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
		NBT.putBoolean("ore", useOreDict);
		NBT.putBoolean("metac", checkMeta);
		NBT.putBoolean("cnbt", checkNBT);
		NBT.putBoolean("white", isWhitelist);
		ListTag li = new ListTag();
		for (int i = 0; i < matchingItems.length; i++) {
			if (!matchingItems[i].isEmpty()) {
				CompoundTag tag = (CompoundTag)matchingItems[i].save(registries);
				tag.putByte("Slot", (byte)i);
				li.add(tag);
			}
		}
		NBT.put("Items", li);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		isWhitelist = NBT.getBoolean("white");
		checkMeta = NBT.getBoolean("metac");
		checkNBT = NBT.getBoolean("cnbt");
		useOreDict = !NBT.contains("ore") || NBT.getBoolean("ore");
		java.util.Arrays.fill(matchingItems, ItemStack.EMPTY);
		ListTag li = NBT.getList("Items", 10);
		for (int i = 0; i < li.size(); i++) {
			CompoundTag tag = li.getCompound(i);
			int s = tag.getByte("Slot");
			if (s >= 0 && s < matchingItems.length)
				matchingItems[s] = ItemStack.parseOptional(registries, tag);
		}
	}
}
