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

import reika.rotarycraft.content.blockentity.DefoliatorBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** Original: ContainerDefoliator - the potion slot (80, 17), the bottle slot (80, 53) and the player inventory. */
public class DefoliatorMenu extends AbstractContainerMenu {

	public final DefoliatorBlockEntity tile;
	private final int own;

	public DefoliatorMenu(int id, Inventory inv, DefoliatorBlockEntity te) {
		super(RotaryMenus.DEFOLIATOR.get(), id);
		tile = te;
		this.addSlot(new SlotItemHandler(te.getInventory(), 0, 80, 17));
		this.addSlot(new SlotItemHandler(te.getInventory(), 1, 80, 53));
		own = slots.size();
		for (int r = 0; r < 3; r++)
			for (int c = 0; c < 9; c++)
				this.addSlot(new Slot(inv, c+r*9+9, 8+c*18, 84+r*18));
		for (int c = 0; c < 9; c++)
			this.addSlot(new Slot(inv, c, 8+c*18, 142));
	}

	public static DefoliatorMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new DefoliatorMenu(id, inv, (DefoliatorBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
