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

import reika.rotarycraft.content.blockentity.ScreenBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** Original: ContainerScreen - three dye slots (62/80/98, 35) and the player inventory. */
public class ScreenMenu extends AbstractContainerMenu {

	public final ScreenBlockEntity tile;
	private final int own;

	public ScreenMenu(int id, Inventory inv, ScreenBlockEntity te) {
		super(RotaryMenus.SCREEN.get(), id);
		tile = te;
		for (int i = 0; i < 3; i++)
			this.addSlot(new SlotItemHandler(te.getInventory(), i, 62+18*i, 35));
		own = slots.size();
		for (int r = 0; r < 3; r++)
			for (int c = 0; c < 9; c++)
				this.addSlot(new Slot(inv, c+r*9+9, 8+c*18, 84+r*18));
		for (int c = 0; c < 9; c++)
			this.addSlot(new Slot(inv, c, 8+c*18, 142));
	}

	public static ScreenMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new ScreenMenu(id, inv, (ScreenBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	@Override
	public boolean stillValid(Player player) {
		return tile != null && !tile.isRemoved() && tile.isPlayerAccessible(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem())
			return ItemStack.EMPTY;
		ItemStack is = slot.getItem(), copy = is.copy();
		if (index < own) {
			if (!this.moveItemStackTo(is, own, slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!this.moveItemStackTo(is, 0, own, false))
			return ItemStack.EMPTY;
		if (is.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
