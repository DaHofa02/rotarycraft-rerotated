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

import reika.rotarycraft.content.blockentity.BlowerBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/**
 * Original: ContainerBlower - 18 ghost filter slots (8+18i, 21+18k) and the player inventory 26 pixels lower than usual. GUI buttons 0-3
 * toggle whitelist, metadata, NBT and ore dictionary; 10-27 set a filter slot to the item on the cursor (empty cursor: clear).
 */
public class BlowerMenu extends AbstractContainerMenu {

	public final BlowerBlockEntity tile;

	public BlowerMenu(int id, Inventory inv, BlowerBlockEntity te) {
		super(RotaryMenus.BLOWER.get(), id);
		tile = te;
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 110 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 168));
	}

	public static BlowerMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new BlowerMenu(id, inv, (BlowerBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id >= 0 && id < 4)
			tile.toggle(id);
		else if (id >= 10 && id < 28)
			tile.setFilter(id-10, this.getCarried());
		else
			return false;
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
