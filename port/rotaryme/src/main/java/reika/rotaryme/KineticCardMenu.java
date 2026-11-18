/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

import appeng.api.upgrades.IUpgradeInventory;

import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/** Shared by both kinetic GUIs: 4 acceleration card slots (menu slots 0-3, a column at the right) and the player inventory below. */
public abstract class KineticCardMenu<T extends RotaryCraftBlockEntity> extends AbstractContainerMenu {

	public static final int CARD_X = 172, CARD_Y = 8, INV_Y = 120;

	public final T tile;

	protected KineticCardMenu(MenuType<?> type, int id, Inventory inv, T te, IUpgradeInventory cards) {
		super(type, id);
		tile = te;
		for (int i = 0; i < cards.size(); i++)
			this.addSlot(new SlotItemHandler(cards.toItemHandler(), i, CARD_X, CARD_Y+i*18));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k+i*9+9, 8+k*18, INV_Y+i*18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8+j*18, INV_Y+58));
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
		if (index < 4) {
			if (!this.moveItemStackTo(stack, 4, this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!this.moveItemStackTo(stack, 0, 4, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
