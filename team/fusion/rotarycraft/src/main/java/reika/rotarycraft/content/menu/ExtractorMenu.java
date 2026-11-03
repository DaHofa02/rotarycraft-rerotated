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

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.content.blockentity.ExtractorBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** Original: ContainerExtractor - four input slots (top), four outputs below them, the bonus slot, and the drill slot when drills wear. */
public class ExtractorMenu extends AbstractContainerMenu {

	public final ExtractorBlockEntity tile;
	private final int machineSlots;

	public ExtractorMenu(int id, Inventory inv, ExtractorBlockEntity te) {
		super(RotaryMenus.EXTRACTOR.get(), id);
		tile = te;
		ItemStackHandler h = te.getInventory();
		for (int i = 0; i < 4; i++) {
			this.addSlot(new SlotItemHandler(h, i, 26+36*i, 13));
			this.addSlot(new BlastFurnaceMenu.OutputSlot(h, 4+i, 26+36*i, 55));
		}
		this.addSlot(new BlastFurnaceMenu.OutputSlot(h, 8, 152, 55));
		if (RotaryConfig.EXTRACTORMAINTAIN.get())
			this.addSlot(new SlotItemHandler(h, 9, 26, 34));
		machineSlots = this.slots.size();
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 84 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 142));
	}

	public static ExtractorMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new ExtractorMenu(id, inv, (ExtractorBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
		if (index < machineSlots) {
			if (!this.moveItemStackTo(stack, machineSlots, this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else {
			boolean moved = false;
			for (int i = 0; i < machineSlots && !moved; i++) {
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
