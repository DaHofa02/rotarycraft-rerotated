/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

import reika.rotarycraft.content.blockentity.FillingStationBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/**
 * Original: ContainerFillingStation - the item being filled (106,71) and the output (134,71) take nothing by hand, the fuel slot (54,21),
 * the input slot (106,21), the player inventory 21 lower (the original's armour slots are left out).
 */
public class FillingStationMenu extends AbstractContainerMenu {

	public final FillingStationBlockEntity tile;

	public FillingStationMenu(int id, Inventory inv, FillingStationBlockEntity te) {
		super(RotaryMenus.FILLING_STATION.get(), id);
		tile = te;
		this.addSlot(new BlastFurnaceMenu.OutputSlot(te.getInventory(), FillingStationBlockEntity.FILLING_SLOT, 106, 71));
		this.addSlot(new SlotItemHandler(te.getInventory(), FillingStationBlockEntity.FUEL_SLOT, 54, 21));
		this.addSlot(new BlastFurnaceMenu.OutputSlot(te.getInventory(), FillingStationBlockEntity.OUTPUT_SLOT, 134, 71));
		this.addSlot(new SlotItemHandler(te.getInventory(), FillingStationBlockEntity.INPUT_SLOT, 106, 21));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 105 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 163));
	}

	public static FillingStationMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new FillingStationMenu(id, inv, (FillingStationBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
		else if (this.slots.get(3).mayPlace(stack)) {
			if (!this.moveItemStackTo(stack, 3, 4, false))
				return ItemStack.EMPTY;
		}
		else if (!this.slots.get(1).mayPlace(stack) || !this.moveItemStackTo(stack, 1, 2, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
