/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/** The 16 filter slots (two rows of eight, one per dye colour in dye order) and the player inventory. */
public class BundledBusMenu extends AbstractContainerMenu {

	public final BundledBusBlockEntity tile;

	public BundledBusMenu(int id, Inventory inv, BundledBusBlockEntity te) {
		super(RotaryME.BUNDLED_BUS_MENU.get(), id);
		tile = te;
		for (int i = 0; i < BundledBusBlockEntity.CHANNELS; i++)
			this.addSlot(new SlotItemHandler(te.getFilters(), i, 17+(i%8)*18, 20+(i/8)*22));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k+i*9+9, 8+k*18, 84+i*18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8+j*18, 142));
	}

	public static BundledBusMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new BundledBusMenu(id, inv, (BundledBusBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
		int n = BundledBusBlockEntity.CHANNELS;
		if (index < n) {
			if (!this.moveItemStackTo(stack, n, this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!this.moveItemStackTo(stack, 0, n, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
