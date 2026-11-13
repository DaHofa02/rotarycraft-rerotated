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

import reika.rotarycraft.content.blockentity.BaitBoxBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** Original: ContainerBasicStorage for the bait box - three rows of 9 slots and the player inventory (18 pixels higher than for 4 rows). */
public class BaitBoxMenu extends AbstractContainerMenu {

	public final BaitBoxBlockEntity tile;

	public BaitBoxMenu(int id, Inventory inv, BaitBoxBlockEntity te) {
		super(RotaryMenus.BAIT_BOX.get(), id);
		tile = te;
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 9; j++)
				this.addSlot(new SlotItemHandler(te.getInventory(), j + i * 9, 8 + j * 18, 18 + i * 18));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 85 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 143));
	}

	public static BaitBoxMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new BaitBoxMenu(id, inv, (BaitBoxBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
		if (index < 27) {
			if (!this.moveItemStackTo(stack, 27, this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!this.moveItemStackTo(stack, 0, 27, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
