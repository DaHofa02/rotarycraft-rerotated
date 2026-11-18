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

import reika.rotarycraft.content.blockentity.PurifierBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** Gunpowder (0), five steel inputs (1-5), output (6, take only), sand (7), then the player inventory. */
public class PurifierMenu extends AbstractContainerMenu {

	public final PurifierBlockEntity tile;

	public PurifierMenu(int id, Inventory inv, PurifierBlockEntity te) {
		super(RotaryMenus.PURIFIER.get(), id);
		tile = te;
		this.addSlot(new SlotItemHandler(te.getInventory(), 0, 26, 17));
		for (int i = 1; i < 6; i++)
			this.addSlot(new SlotItemHandler(te.getInventory(), i, 44+(i-1)*18, 35));
		this.addSlot(new SlotItemHandler(te.getInventory(), 6, 152, 35) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		});
		this.addSlot(new SlotItemHandler(te.getInventory(), 7, 26, 53));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k+i*9+9, 8+k*18, 84+i*18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8+j*18, 142));
	}

	public static PurifierMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new PurifierMenu(id, inv, (PurifierBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
		else if (!this.moveItemStackTo(stack, 0, 6, false) && !this.moveItemStackTo(stack, 7, 8, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
