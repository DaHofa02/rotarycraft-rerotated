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

import reika.rotarycraft.content.blockentity.AutoCrafterBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/**
 * Pattern slots (two rows of 9), their outputs (two rows) and leftovers (two rows), then the player inventory. Buttons: 0 next mode,
 * 100+i one request for pattern i, 1000+100*i+n sustain threshold n (0-64) for pattern i.
 */
public class AutoCrafterMenu extends AbstractContainerMenu {

	public static final int INV_Y = 150;

	public final AutoCrafterBlockEntity tile;

	public AutoCrafterMenu(int id, Inventory inv, AutoCrafterBlockEntity te) {
		super(RotaryMenus.AUTO_CRAFTER.get(), id);
		tile = te;
		int[] rowY = {18, 36, 62, 80, 106, 124};
		for (int block = 0; block < 3; block++)
			for (int i = 0; i < 18; i++)
				this.addSlot(new SlotItemHandler(te.getInventory(), block*18+i, 8+(i%9)*18, rowY[block*2+i/9]) {
					@Override
					public boolean mayPlace(ItemStack is) {
						return this.getSlotIndex() < AutoCrafterBlockEntity.SIZE && super.mayPlace(is);
					}
				});
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k+i*9+9, 8+k*18, INV_Y+i*18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8+j*18, INV_Y+58));
	}

	public static AutoCrafterMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new AutoCrafterMenu(id, inv, (AutoCrafterBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (tile == null || !this.stillValid(player))
			return false;
		if (id == 0)
			tile.stepMode();
		else if (id >= 100 && id < 100+AutoCrafterBlockEntity.SIZE)
			tile.request(id-100);
		else if (id >= 1000 && id < 1000+100*AutoCrafterBlockEntity.SIZE && (id-1000)%100 <= 64)
			tile.setThreshold((id-1000)/100, (id-1000)%100);
		else
			return false;
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
		if (index < 54) {
			if (!this.moveItemStackTo(stack, 54, this.slots.size(), true))
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
