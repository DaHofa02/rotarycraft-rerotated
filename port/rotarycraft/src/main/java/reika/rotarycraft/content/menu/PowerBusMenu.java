/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.menu;

import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

import reika.rotarycraft.content.blockentity.PowerBusBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/**
 * Original: ContainerPowerBus - one slot per horizontal side that may hold a part (north top, south bottom, west left, east right), the
 * player inventory below. Menu buttons 0-3 (north, south, west, east) switch a side between torque and speed (original POWERBUS packet).
 */
public class PowerBusMenu extends AbstractContainerMenu {

	public static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
	public static final int[] SLOT_X = {102, 102, 66, 138}, SLOT_Y = {33, 105, 69, 69};

	public final PowerBusBlockEntity tile;
	private final int busSlots;

	public PowerBusMenu(int id, Inventory inv, PowerBusBlockEntity te) {
		super(RotaryMenus.POWER_BUS.get(), id);
		tile = te;
		int n = 0;
		for (int i = 0; i < 4; i++) {
			if (te.canHaveItemInSlot(SIDES[i])) {
				this.addSlot(new SlotItemHandler(te.getInventory(), i, SLOT_X[i], SLOT_Y[i]));
				n++;
			}
		}
		busSlots = n;
		int dx = 22, dy = 57;
		for (int r = 0; r < 3; r++)
			for (int c = 0; c < 9; c++)
				this.addSlot(new Slot(inv, c+r*9+9, 8+c*18+dx, 84+r*18+dy));
		for (int c = 0; c < 9; c++)
			this.addSlot(new Slot(inv, c, 8+c*18+dx, 142+dy-4));
	}

	public static PowerBusMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new PowerBusMenu(id, inv, (PowerBusBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (tile == null || !this.stillValid(player) || id < 0 || id > 3 || !tile.canHaveItemInSlot(SIDES[id]))
			return false;
		tile.setMode(SIDES[id], !tile.isSideSpeedMode(SIDES[id]));
		return true;
	}

	@Override
	public boolean stillValid(Player player) {
		return tile != null && !tile.isRemoved() && tile.isPlayerAccessible(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem())
			return ItemStack.EMPTY;
		ItemStack is = slot.getItem(), copy = is.copy();
		if (index < busSlots) {
			if (!this.moveItemStackTo(is, busSlots, slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!this.moveItemStackTo(is, 0, busSlots, false))
			return ItemStack.EMPTY;
		if (is.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
