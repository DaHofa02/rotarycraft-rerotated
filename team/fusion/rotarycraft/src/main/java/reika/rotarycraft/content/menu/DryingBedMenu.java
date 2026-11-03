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

import reika.rotarycraft.content.blockentity.DryingBedBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** Original: ContainerDryingBed - the output slot (125,35) and the player inventory. */
public class DryingBedMenu extends AbstractContainerMenu {

	public final DryingBedBlockEntity tile;

	public DryingBedMenu(int id, Inventory inv, DryingBedBlockEntity te) {
		super(RotaryMenus.DRYING_BED.get(), id);
		tile = te;
		this.addSlot(new BlastFurnaceMenu.OutputSlot(te.getInventory(), 0, 125, 35));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 84 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 142));
	}

	public static DryingBedMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new DryingBedMenu(id, inv, (DryingBedBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	@Override
	public boolean stillValid(Player player) {
		return tile != null && !tile.isRemoved() && tile.isPlayerAccessible(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (index != 0 || !slot.hasItem())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (!this.moveItemStackTo(stack, 1, this.slots.size(), true))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
