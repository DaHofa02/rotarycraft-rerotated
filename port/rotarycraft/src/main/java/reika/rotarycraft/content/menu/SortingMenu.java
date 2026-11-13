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

import reika.rotarycraft.content.blockentity.SortingBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/**
 * Original: ContainerSorter - 27 ghost slots (8+18i, 18+22k) and the player inventory 14 pixels lower than usual. Clicking a ghost slot
 * (here GUI button 0-26) maps it to one of the item held on the cursor, or clears it with an empty cursor.
 */
public class SortingMenu extends AbstractContainerMenu {

	public final SortingBlockEntity tile;

	public SortingMenu(int id, Inventory inv, SortingBlockEntity te) {
		super(RotaryMenus.SORTING.get(), id);
		tile = te;
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 98 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 156));
	}

	public static SortingMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new SortingMenu(id, inv, (SortingBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id < 0 || id >= 27)
			return false;
		tile.setMapping(id, this.getCarried().copyWithCount(Math.min(1, this.getCarried().getCount())));
		return true;
	}

	@Override
	public boolean stillValid(Player player) {
		return tile != null && !tile.isRemoved() && tile.isPlayerAccessible(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}
}
