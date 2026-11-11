/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

import reika.rotarycraft.content.blockentity.ComposterBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/**
 * Original: ContainerComposter - matter (55,26), yeast (55,44), the compost output (116,35) and the player inventory. The cook time
 * (as in the original) and the temperature are sent to the open GUI.
 */
public class ComposterMenu extends AbstractContainerMenu {

	public final ComposterBlockEntity tile;

	public ComposterMenu(int id, Inventory inv, ComposterBlockEntity te) {
		super(RotaryMenus.COMPOSTER.get(), id);
		tile = te;
		this.addSlot(new SlotItemHandler(te.getInventory(), 0, 55, 26));
		this.addSlot(new SlotItemHandler(te.getInventory(), 1, 55, 44));
		this.addSlot(new SlotItemHandler(te.getInventory(), 2, 116, 35));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 84 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 142));
		this.addDataSlot(new DataSlot() {
			@Override
			public int get() {
				return tile.composterCookTime;
			}

			@Override
			public void set(int v) {
				tile.composterCookTime = v;
			}
		});
		this.addDataSlot(new DataSlot() {
			@Override
			public int get() {
				return tile.getTemperature();
			}

			@Override
			public void set(int v) {
				tile.setTemperature(v);
			}
		});
	}

	public static ComposterMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new ComposterMenu(id, inv, (ComposterBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
		if (index < 3) {
			if (!this.moveItemStackTo(stack, 3, this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!this.moveItemStackTo(stack, 0, 2, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
