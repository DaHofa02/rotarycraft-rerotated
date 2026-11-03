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

import reika.rotarycraft.content.blockentity.RefrigeratorBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** Original: ContainerFridge - the ice slot (99,58), the dry ice output (132,72) and the player inventory 22 lower. */
public class RefrigeratorMenu extends AbstractContainerMenu {

	public final RefrigeratorBlockEntity tile;

	public RefrigeratorMenu(int id, Inventory inv, RefrigeratorBlockEntity te) {
		super(RotaryMenus.REFRIGERATOR.get(), id);
		tile = te;
		this.addSlot(new SlotItemHandler(te.getInventory(), 0, 99, 58));
		this.addSlot(new BlastFurnaceMenu.OutputSlot(te.getInventory(), 1, 132, 72));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 106 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 164));
	}

	public static RefrigeratorMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new RefrigeratorMenu(id, inv, (RefrigeratorBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
		else if (!this.slots.get(0).mayPlace(stack) || !this.moveItemStackTo(stack, 0, 1, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
