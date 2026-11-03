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

import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import reika.rotarycraft.content.blockentity.FractionatorBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** Original: ContainerFractionator - six ingredient slots in two columns, the ghast tear (103,54); the bucket slot 7 has no GUI slot. */
public class FractionatorMenu extends AbstractContainerMenu {

	public final FractionatorBlockEntity tile;
	private static final int MACHINE_SLOTS = 7;

	public FractionatorMenu(int id, Inventory inv, FractionatorBlockEntity te) {
		super(RotaryMenus.FRACTIONATOR.get(), id);
		tile = te;
		ItemStackHandler h = te.getInventory();
		for (int i = 0; i < 6; i++)
			this.addSlot(new SlotItemHandler(h, i, i < 3 ? 17 : 53, 16+19*(i%3)));
		this.addSlot(new SlotItemHandler(h, 6, 103, 54));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 84 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 142));
	}

	public static FractionatorMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new FractionatorMenu(id, inv, (FractionatorBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
		else {
			boolean moved = false;
			for (int i = 0; i < MACHINE_SLOTS && !moved; i++) {
				Slot s = this.slots.get(i);
				if (s.mayPlace(stack))
					moved = this.moveItemStackTo(stack, i, i+1, false);
			}
			if (!moved)
				return ItemStack.EMPTY;
		}
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
