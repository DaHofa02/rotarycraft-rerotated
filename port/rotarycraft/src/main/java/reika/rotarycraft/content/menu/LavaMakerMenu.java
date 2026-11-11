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

import reika.rotarycraft.content.blockentity.LavaMakerBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** Original: ContainerRockMelter - the 3x3 rock slots from (26,17) and the player inventory. */
public class LavaMakerMenu extends AbstractContainerMenu {

	public final LavaMakerBlockEntity tile;

	public LavaMakerMenu(int id, Inventory inv, LavaMakerBlockEntity te) {
		super(RotaryMenus.LAVA_MAKER.get(), id);
		tile = te;
		for (int i = 0; i < 9; i++)
			this.addSlot(new SlotItemHandler(te.getInventory(), i, 26 + 18 * (i % 3), 17 + 18 * (i / 3)));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 84 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 142));
	}

	public static LavaMakerMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new LavaMakerMenu(id, inv, (LavaMakerBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
		if (index < 9) {
			if (!this.moveItemStackTo(stack, 9, this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!this.slots.get(0).mayPlace(stack) || !this.moveItemStackTo(stack, 0, 9, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
