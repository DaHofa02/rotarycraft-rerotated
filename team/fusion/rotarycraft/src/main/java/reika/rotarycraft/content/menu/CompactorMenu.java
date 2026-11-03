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

import reika.rotarycraft.content.blockentity.CompactorBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** Original: ContainerCompactor - the four input slots (26, 8-62), the output (80,35) and the player inventory. */
public class CompactorMenu extends AbstractContainerMenu {

	public final CompactorBlockEntity tile;

	public CompactorMenu(int id, Inventory inv, CompactorBlockEntity te) {
		super(RotaryMenus.COMPACTOR.get(), id);
		tile = te;
		for (int i = 0; i < 4; i++)
			this.addSlot(new SlotItemHandler(te.getInventory(), i, 26, 8+18*i));
		this.addSlot(new BlastFurnaceMenu.OutputSlot(te.getInventory(), 4, 80, 35));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 84 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 142));
	}

	public static CompactorMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new CompactorMenu(id, inv, (CompactorBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
		if (index < 5) {
			if (!this.moveItemStackTo(stack, 5, this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!this.slots.get(0).mayPlace(stack) || !this.moveItemStackTo(stack, 0, 4, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
