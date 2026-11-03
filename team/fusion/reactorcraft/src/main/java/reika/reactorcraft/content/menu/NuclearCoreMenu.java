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

import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import reika.reactorcraft.reactor.NuclearCoreBlockEntity;
import reika.reactorcraft.registry.ReactorMenus;

/** Original: ContainerNuclearCore - the fuel column in the middle, waste on both sides (take only), player inventory 16 lower. */
public class NuclearCoreMenu extends AbstractContainerMenu {

	public final NuclearCoreBlockEntity tile;
	private static final int MACHINE_SLOTS = 12;

	public NuclearCoreMenu(int id, Inventory inv, NuclearCoreBlockEntity te) {
		super(ReactorMenus.NUCLEAR_CORE.get(), id);
		tile = te;
		ItemStackHandler h = te.getInventory();
		for (int i = 0; i < 4; i++)
			this.addSlot(new SlotItemHandler(h, i, 80, 23+18*i));
		for (int i = 0; i < 4; i++)
			this.addSlot(new TakeOnly(h, 4+i, 53, 23+18*i));
		for (int i = 0; i < 4; i++)
			this.addSlot(new TakeOnly(h, 8+i, 107, 23+18*i));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 100 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 158));
	}

	private static final class TakeOnly extends SlotItemHandler {
		TakeOnly(ItemStackHandler h, int i, int x, int y) {
			super(h, i, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}
	}

	public static NuclearCoreMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new NuclearCoreMenu(id, inv, (NuclearCoreBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
		if (index < MACHINE_SLOTS) {
			if (!this.moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!this.moveItemStackTo(stack, 0, 4, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
