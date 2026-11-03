/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import reika.rotarycraft.content.blockentity.BlastFurnaceBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/**
 * Original: ContainerBlastFurnace. Centre additive (26,35), 3x3 grid, outputs (148,17/35/53), lower (26,54) and upper (26,16)
 * additive; middle-click on an input slot locks it against automated insertion. The crafting pattern slot (123,53) is not ported yet.
 */
public class BlastFurnaceMenu extends AbstractContainerMenu {

	public final BlastFurnaceBlockEntity tile;
	/** Menu slot index -> machine slot (original order of addSlotToContainer). */
	private static final int[] MACHINE_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
	private final int machineSlots;

	public BlastFurnaceMenu(int id, Inventory inv, BlastFurnaceBlockEntity te) {
		super(RotaryMenus.BLAST_FURNACE.get(), id);
		tile = te;
		ItemStackHandler h = te.getInventory();
		this.addSlot(new SlotItemHandler(h, 0, 26, 35));
		int n = 1;
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				this.addSlot(new SlotItemHandler(h, n, 62+j*18, 17+i*18));
				n++;
			}
		}
		this.addSlot(new OutputSlot(h, 10, 148, 35));
		this.addSlot(new SlotItemHandler(h, 11, 26, 54));
		this.addSlot(new OutputSlot(h, 12, 148, 17));
		this.addSlot(new OutputSlot(h, 13, 148, 53));
		this.addSlot(new SlotItemHandler(h, 14, 26, 16));
		machineSlots = this.slots.size();

		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 84 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 142));
	}

	public static BlastFurnaceMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new BlastFurnaceMenu(id, inv, (BlastFurnaceBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	/** Original SlotFurnace: take only. */
	public static class OutputSlot extends SlotItemHandler {
		public OutputSlot(IItemHandler h, int index, int x, int y) {
			super(h, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}
	}

	/** Whether a menu slot is one of the plain input slots that can be locked (original: slots of class Slot). */
	public boolean isLockable(int menuSlot) {
		return menuSlot >= 0 && menuSlot < machineSlots && !(this.slots.get(menuSlot) instanceof OutputSlot);
	}

	public int machineSlotOf(int menuSlot) {
		return MACHINE_SLOTS[menuSlot];
	}

	@Override
	public void clicked(int slotId, int button, ClickType type, Player player) {
		if (type == ClickType.CLONE && this.isLockable(slotId)) {
			if (!player.level().isClientSide)
				tile.toggleLock(this.machineSlotOf(slotId));
			return; //prevent creative item dupe
		}
		super.clicked(slotId, button, type, player);
	}

	@Override
	public boolean stillValid(Player player) {
		return tile != null && !tile.isRemoved() && tile.isPlayerAccessible(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (!slot.hasItem())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index < machineSlots) {
			if (!this.moveItemStackTo(stack, machineSlots, this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!this.moveItemStackTo(stack, 1, 10, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
