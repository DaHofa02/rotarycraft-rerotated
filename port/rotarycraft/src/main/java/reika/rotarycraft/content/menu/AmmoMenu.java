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
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import reika.rotarycraft.content.blockentity.AimedCannonBlockEntity;
import reika.rotarycraft.content.blockentity.MachineGunBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** Ammo slots of a weapon (rows of nine; the machine gun's 27, a cannon's 9) and the player inventory. */
public class AmmoMenu extends AbstractContainerMenu {

	public final RotaryCraftBlockEntity tile;
	private final int ammo;

	public AmmoMenu(int id, Inventory inv, RotaryCraftBlockEntity te) {
		super(RotaryMenus.AMMO.get(), id);
		tile = te;
		ItemStackHandler h = inventory(te);
		ammo = h.getSlots();
		int top = ammo > 9 ? 18 : 50;
		for (int i = 0; i < ammo; i++)
			this.addSlot(new SlotItemHandler(h, i, 8+(i%9)*18, top+(i/9)*18));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k+i*9+9, 8+k*18, 84+i*18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8+j*18, 142));
	}

	public static ItemStackHandler inventory(RotaryCraftBlockEntity te) {
		if (te instanceof reika.rotarycraft.content.blockentity.turret.TurretBlockEntity t)
			return t.getInventory();
		return te instanceof AimedCannonBlockEntity c ? c.getInventory() : ((MachineGunBlockEntity)te).getInventory();
	}

	public static AmmoMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new AmmoMenu(id, inv, (RotaryCraftBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
		if (index < ammo) {
			if (!this.moveItemStackTo(stack, ammo, this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!this.moveItemStackTo(stack, 0, ammo, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
