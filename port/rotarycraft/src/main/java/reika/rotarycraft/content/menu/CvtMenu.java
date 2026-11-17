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

import reika.rotarycraft.content.blockentity.AdvancedGearBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/**
 * CVT: 31 ratio belt slots (4 rows of 8, the last row 7), the required belt slot at the right, the player inventory below. Buttons: 0 next
 * mode, 1/2 next redstone ratio without/with signal, 3 flips the manual ratio between speed and torque, 10+n manual ratio n, 100+n target torque n.
 */
public class CvtMenu extends AbstractContainerMenu {

	public static final int REQUIRED_X = 188, REQUIRED_Y = 18, INV_Y = 102;

	public final AdvancedGearBlockEntity tile;

	public CvtMenu(int id, Inventory inv, AdvancedGearBlockEntity te) {
		super(RotaryMenus.CVT.get(), id);
		tile = te;
		for (int i = 0; i < 31; i++)
			this.addSlot(new SlotItemHandler(te.belts, i, 8+(i%8)*18, 18+(i/8)*18));
		this.addSlot(new SlotItemHandler(te.belts, 31, REQUIRED_X, REQUIRED_Y));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k+i*9+9, 8+k*18, INV_Y+i*18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8+j*18, INV_Y+58));
	}

	public static CvtMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new CvtMenu(id, inv, (AdvancedGearBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (tile == null || !this.stillValid(player) || tile.kind != AdvancedGearBlockEntity.Kind.CVT)
			return false;
		switch (id) {
			case 0 -> tile.stepCvtMode();
			case 1 -> tile.stepCvtState(false);
			case 2 -> tile.stepCvtState(true);
			case 3 -> tile.setCvtRatio(-tile.getCvtRatio());
			default -> {
				if (id >= 10 && id <= 42)
					tile.setCvtRatio(Integer.signum(tile.getCvtRatio() == 0 ? 1 : tile.getCvtRatio())*(id-10));
				else if (id >= 100)
					tile.setTargetTorque(id-100);
				else
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
		if (index < 32) {
			if (!this.moveItemStackTo(stack, 32, this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!this.moveItemStackTo(stack, 31, 32, false) && !this.moveItemStackTo(stack, 0, 31, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
