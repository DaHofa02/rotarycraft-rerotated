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

import reika.rotarycraft.content.blockentity.LandmineBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** Four charge slots (gunpowder), four modifier slots, then the player inventory. */
public class LandmineMenu extends AbstractContainerMenu {

	public final LandmineBlockEntity tile;

	public LandmineMenu(int id, Inventory inv, LandmineBlockEntity te) {
		super(RotaryMenus.LANDMINE.get(), id);
		tile = te;
		for (int i = 0; i < 8; i++)
			this.addSlot(new SlotItemHandler(te.getInventory(), i, 53+(i%4)*18, i < 4 ? 20 : 48));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k+i*9+9, 8+k*18, 84+i*18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8+j*18, 142));
	}

	public static LandmineMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new LandmineMenu(id, inv, (LandmineBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
		if (index < 8) {
			if (!this.moveItemStackTo(stack, 8, this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!this.moveItemStackTo(stack, 0, 8, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
