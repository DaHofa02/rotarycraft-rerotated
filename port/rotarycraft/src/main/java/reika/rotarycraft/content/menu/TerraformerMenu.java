/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

import reika.rotarycraft.content.blockentity.TerraformerBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** 18 item slots (two rows) and the player inventory. Buttons: 0 next target, 1 radius -1, 2 radius +1. */
public class TerraformerMenu extends AbstractContainerMenu {

	public static final int INV_Y = 102;

	public final TerraformerBlockEntity tile;

	public TerraformerMenu(int id, Inventory inv, TerraformerBlockEntity te) {
		super(RotaryMenus.TERRAFORMER.get(), id);
		tile = te;
		for (int i = 0; i < 18; i++)
			this.addSlot(new SlotItemHandler(te.getInventory(), i, 8+(i%9)*18, 54+(i/9)*18));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k+i*9+9, 8+k*18, INV_Y+i*18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8+j*18, INV_Y+58));
	}

	public static TerraformerMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new TerraformerMenu(id, inv, (TerraformerBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (tile == null || !this.stillValid(player))
			return false;
		switch (id) {
			case 0 -> tile.setTarget(TerraformerBlockEntity.Target.values()[(tile.getTarget().ordinal()+1)%TerraformerBlockEntity.Target.values().length]);
			case 1 -> tile.setRadius(tile.getRadius()-1);
			case 2 -> tile.setRadius(tile.getRadius()+1);
			default -> {
				return false;
			}
		}
		return true;
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
