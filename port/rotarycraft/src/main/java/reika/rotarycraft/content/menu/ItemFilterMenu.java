/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

import reika.rotarycraft.content.blockentity.ItemFilterBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** Template slot, buffer slot, 16 blacklist slots and the player inventory. Buttons 0-3 cycle item/components/mod/tags, 10+i tag i. */
public class ItemFilterMenu extends AbstractContainerMenu {

	public static final int INV_Y = 102;

	public final ItemFilterBlockEntity tile;

	public ItemFilterMenu(int id, Inventory inv, ItemFilterBlockEntity te) {
		super(RotaryMenus.ITEM_FILTER.get(), id);
		tile = te;
		this.addSlot(new SlotItemHandler(te.getInventory(), 0, 8, 18));
		this.addSlot(new SlotItemHandler(te.getInventory(), 1, 44, 18));
		for (int i = 0; i < 16; i++)
			this.addSlot(new SlotItemHandler(te.getInventory(), 2+i, 8+(i%8)*18, 54+(i/8)*18));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k+i*9+9, 8+k*18, INV_Y+i*18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8+j*18, INV_Y+58));
	}

	public static ItemFilterMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new ItemFilterMenu(id, inv, (ItemFilterBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (tile == null || !this.stillValid(player))
			return false;
		if (id >= 0 && id < 4)
			tile.cycle(ItemFilterBlockEntity.Criterion.values()[id]);
		else if (id >= 10 && id-10 < tile.getTags().size())
			tile.cycleTag(id-10);
		else
			return false;
		return true;
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
		if (index < 18) {
			if (!this.moveItemStackTo(stack, 18, this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!this.moveItemStackTo(stack, 1, 2, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
