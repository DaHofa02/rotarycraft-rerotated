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

import reika.rotarycraft.content.blockentity.CentrifugeBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** Original: ContainerCentrifuge - input (26,38), nine outputs in a 3x3 grid from (85,20). */
public class CentrifugeMenu extends AbstractContainerMenu {

	public final CentrifugeBlockEntity tile;
	private static final int MACHINE_SLOTS = 10;

	public CentrifugeMenu(int id, Inventory inv, CentrifugeBlockEntity te) {
		super(RotaryMenus.CENTRIFUGE.get(), id);
		tile = te;
		ItemStackHandler h = te.getInventory();
		this.addSlot(new SlotItemHandler(h, 0, 26, 38));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 3; k++)
				this.addSlot(new BlastFurnaceMenu.OutputSlot(h, 1+i*3+k, 85+k*18, 20+i*18));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 84 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 142));
	}

	public static CentrifugeMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new CentrifugeMenu(id, inv, (CentrifugeBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
