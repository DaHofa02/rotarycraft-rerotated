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

import reika.rotarycraft.content.blockentity.CrystallizerBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/**
 * Original: ContainerCrystallizer - the output (80,35), dry ice (125,35) and the player inventory. The freezing progress and the
 * temperature are sent to the open GUI (the tank comes with the block entity's own sync).
 */
public class CrystallizerMenu extends AbstractContainerMenu {

	public final CrystallizerBlockEntity tile;

	public CrystallizerMenu(int id, Inventory inv, CrystallizerBlockEntity te) {
		super(RotaryMenus.COMPOSTER.get(), id);
		tile = te;
		this.addSlot(new SlotItemHandler(te.getInventory(), 0, 80, 35));
		this.addSlot(new SlotItemHandler(te.getInventory(), 1, 125, 35));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 84 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 142));
		this.addDataSlot(new DataSlot() {
			@Override
			public int get() {
				return tile.freezeTick;
			}

			@Override
			public void set(int v) {
				tile.freezeTick = v;
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

	public static CrystallizerMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new CrystallizerMenu(id, inv, (CrystallizerBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
		if (index < 2) {
			if (!this.moveItemStackTo(stack, 2, this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!this.moveItemStackTo(stack, 1, 2, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
