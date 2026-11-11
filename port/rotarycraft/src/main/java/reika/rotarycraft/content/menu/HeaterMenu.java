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

import reika.rotarycraft.content.blockentity.HeaterBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/**
 * Original: ContainerHeater - two rows of nine fuel slots from (8,18) and the player inventory. The set temperature typed into the
 * GUI (original packet HEATER) arrives as a menu button click with the temperature as its id.
 */
public class HeaterMenu extends AbstractContainerMenu {

	public final HeaterBlockEntity tile;

	public HeaterMenu(int id, Inventory inv, HeaterBlockEntity te) {
		super(RotaryMenus.HEATER.get(), id);
		tile = te;
		for (int i = 0; i < 2; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new SlotItemHandler(te.getInventory(), k + i * 9, 8 + k * 18, 18 + i * 18));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 85 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 143));
	}

	public static HeaterMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new HeaterMenu(id, inv, (HeaterBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id < 0 || id > 9999 || tile == null) //the text field takes four digits
			return false;
		tile.setSetTemperature(id);
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
		else if (!this.slots.get(0).mayPlace(stack) || !this.moveItemStackTo(stack, 0, 18, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
