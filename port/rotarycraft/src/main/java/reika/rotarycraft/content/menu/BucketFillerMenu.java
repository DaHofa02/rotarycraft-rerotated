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

import reika.rotarycraft.content.blockentity.BucketFillerBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** Original: ContainerBasicStorage for the bucket filler - two rows of 9 slots and the player inventory (36 pixels higher than for 4 rows). */
public class BucketFillerMenu extends AbstractContainerMenu {

	public final BucketFillerBlockEntity tile;

	public BucketFillerMenu(int id, Inventory inv, BucketFillerBlockEntity te) {
		super(RotaryMenus.BUCKET_FILLER.get(), id);
		tile = te;
		for (int i = 0; i < 2; i++)
			for (int j = 0; j < 9; j++)
				this.addSlot(new SlotItemHandler(te.getInventory(), j + i * 9, 8 + j * 18, 18 + i * 18));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 67 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 125));
	}

	public static BucketFillerMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new BucketFillerMenu(id, inv, (BucketFillerBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
		if (index < 18) {
			if (!this.moveItemStackTo(stack, 18, this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!this.moveItemStackTo(stack, 0, 18, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
