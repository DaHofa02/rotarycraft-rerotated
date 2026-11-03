/*******************************************************************************
 * @author Reika Kalseki (original DragonAPI). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.lib.data;

import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.items.ItemStackHandler;

/** Original: DragonAPI ReikaInventoryHelper (the parts used so far). */
public final class InventoryHelper {

	private InventoryHelper() {}

	/** Original addOrSetStack(ItemStack, ItemStack[], slot): puts the whole stack in, or nothing; true if it fit. */
	public static boolean addOrSetStack(ItemStack is, ItemStackHandler inv, int slot) {
		if (is.isEmpty())
			return false;
		ItemStack at = inv.getStackInSlot(slot);
		if (at.isEmpty()) {
			inv.setStackInSlot(slot, is.copy());
			return true;
		}
		if (!ItemStack.isSameItemSameComponents(is, at) || at.getCount()+is.getCount() > is.getMaxStackSize())
			return false;
		inv.setStackInSlot(slot, at.copyWithCount(at.getCount()+is.getCount()));
		return true;
	}

	/** Original decrStack: removes one item. */
	public static void decrStack(ItemStackHandler inv, int slot, int amount) {
		ItemStack at = inv.getStackInSlot(slot);
		if (at.isEmpty())
			return;
		inv.setStackInSlot(slot, at.getCount() > amount ? at.copyWithCount(at.getCount()-amount) : ItemStack.EMPTY);
	}

	/** Original hasSpaceFor(is, inv, true, first, last): room for the whole stack in slots [first, last). */
	public static boolean hasSpaceFor(ItemStack is, ItemStackHandler inv, int first, int last) {
		int left = is.getCount();
		int max = is.getMaxStackSize();
		for (int i = first; i < last && left > 0; i++) {
			ItemStack in = inv.getStackInSlot(i);
			if (in.isEmpty())
				left -= max;
			else if (ItemStack.isSameItemSameComponents(is, in))
				left -= max-in.getCount();
		}
		return left <= 0;
	}

	/**
	 * Original addToIInv(is, inv, true, first, last): adds the whole stack to slots [first, last) or nothing, ignoring slot validity.
	 * As in the original, the slots are visited in order and the first empty slot takes the rest of the stack.
	 */
	public static boolean addToRange(ItemStack is, ItemStackHandler inv, int first, int last) {
		if (is.isEmpty() || !hasSpaceFor(is, inv, first, last))
			return false;
		int left = is.getCount();
		int max = is.getMaxStackSize();
		for (int i = first; i < last; i++) {
			ItemStack in = inv.getStackInSlot(i);
			if (in.isEmpty()) {
				int added = Math.min(left, max);
				inv.setStackInSlot(i, is.copyWithCount(added));
				return true;
			}
			if (ItemStack.isSameItemSameComponents(is, in) && in.getCount() < max) {
				int added = Math.min(left, max-in.getCount());
				inv.setStackInSlot(i, in.copyWithCount(in.getCount()+added));
				left -= added;
				if (left <= 0)
					return true;
			}
		}
		return left == 0;
	}
}
