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

import reika.rotarycraft.content.blockentity.BigFurnaceBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/**
 * Original: ContainerBigFurnace - 18 inputs (8+18c, 18+21r) above their 18 take-only outputs (from y 72) and the player inventory 41
 * pixels lower than usual. The smelting progress and the temperature are sent to the open GUI.
 */
public class BigFurnaceMenu extends AbstractContainerMenu {

	public final BigFurnaceBlockEntity tile;

	public BigFurnaceMenu(int id, Inventory inv, BigFurnaceBlockEntity te) {
		super(RotaryMenus.BIG_FURNACE.get(), id);
		tile = te;
		for (int i = 0; i < BigFurnaceBlockEntity.INPUTS; i++)
			this.addSlot(new SlotItemHandler(te.getInventory(), i, 8+(i%9)*18, 18+(i/9)*21));
		for (int i = 0; i < BigFurnaceBlockEntity.INPUTS; i++)
			this.addSlot(new SlotItemHandler(te.getInventory(), i+BigFurnaceBlockEntity.INPUTS, 8+(i%9)*18, 72+(i/9)*21));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 125 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 183));
		this.addDataSlot(new DataSlot() {
			@Override
			public int get() {
				return tile.smeltTick;
			}

			@Override
			public void set(int v) {
				tile.smeltTick = v;
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

	public static BigFurnaceMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new BigFurnaceMenu(id, inv, (BigFurnaceBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
		if (index < 36) {
			if (!this.moveItemStackTo(stack, 36, this.slots.size(), true))
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
