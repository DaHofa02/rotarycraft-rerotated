/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

import reika.reactorcraft.content.blockentity.ElectrolyzerBlockEntity;
import reika.reactorcraft.registry.ReactorMenus;

/** Original: ContainerElectrolyzer - the item slot, player inventory 9 pixels lower than usual. */
public class ElectrolyzerMenu extends AbstractContainerMenu {

	public final ElectrolyzerBlockEntity tile;

	public ElectrolyzerMenu(int id, Inventory inv, ElectrolyzerBlockEntity te) {
		super(ReactorMenus.ELECTROLYZER.get(), id);
		tile = te;
		this.addSlot(new SlotItemHandler(te.getInventory(), 0, 44, 41));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 93 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 151));
	}

	public static ElectrolyzerMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new ElectrolyzerMenu(id, inv, (ElectrolyzerBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
		if (index < 1) {
			if (!this.moveItemStackTo(stack, 1, this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!this.slots.get(0).mayPlace(stack) || !this.moveItemStackTo(stack, 0, 1, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
