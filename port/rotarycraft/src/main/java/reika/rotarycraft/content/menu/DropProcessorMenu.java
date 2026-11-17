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

import reika.rotarycraft.content.blockentity.DropProcessorBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** Input and output slot and the player inventory. */
public class DropProcessorMenu extends AbstractContainerMenu {

	public final DropProcessorBlockEntity tile;

	public DropProcessorMenu(int id, Inventory inv, DropProcessorBlockEntity te) {
		super(RotaryMenus.DROP_PROCESSOR.get(), id);
		tile = te;
		this.addSlot(new SlotItemHandler(te.getInventory(), 0, 44, 35));
		this.addSlot(new SlotItemHandler(te.getInventory(), 1, 116, 35) {
			@Override
			public boolean mayPlace(ItemStack is) {
				return false;
			}
		});
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k+i*9+9, 8+k*18, 84+i*18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8+j*18, 142));
	}

	public static DropProcessorMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new DropProcessorMenu(id, inv, (DropProcessorBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
		if (index < 2) {
			if (!this.moveItemStackTo(stack, 2, this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!this.moveItemStackTo(stack, 0, 1, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
