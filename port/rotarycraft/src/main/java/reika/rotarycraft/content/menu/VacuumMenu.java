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

import reika.rotarycraft.content.blockentity.VacuumBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** Original: ContainerBasicStorage for the vacuum (GuiVacuum) - six rows of 9 slots and the player inventory; button 0 lets the stored XP out. */
public class VacuumMenu extends AbstractContainerMenu {

	public final VacuumBlockEntity tile;

	public VacuumMenu(int id, Inventory inv, VacuumBlockEntity te) {
		super(RotaryMenus.VACUUM.get(), id);
		tile = te;
		for (int i = 0; i < 6; i++)
			for (int j = 0; j < 9; j++)
				this.addSlot(new SlotItemHandler(te.getInventory(), j + i * 9, 8 + j * 18, 18 + i * 18));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 139 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 197));
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id != 0)
			return false;
		tile.spawnXP();
		return true;
	}

	public static VacuumMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new VacuumMenu(id, inv, (VacuumBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
		if (index < 54) {
			if (!this.moveItemStackTo(stack, 54, this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!this.moveItemStackTo(stack, 0, 54, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
