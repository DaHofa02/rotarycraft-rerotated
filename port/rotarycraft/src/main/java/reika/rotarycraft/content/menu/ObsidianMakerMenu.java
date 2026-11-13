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

import reika.rotarycraft.content.blockentity.ObsidianMakerBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** Original: ContainerObsidian - the 9 output slots (62+18c, 18+18r) and the player inventory; the temperature goes to the open GUI. */
public class ObsidianMakerMenu extends AbstractContainerMenu {

	public final ObsidianMakerBlockEntity tile;

	public ObsidianMakerMenu(int id, Inventory inv, ObsidianMakerBlockEntity te) {
		super(RotaryMenus.OBSIDIAN_MAKER.get(), id);
		tile = te;
		for (int r = 0; r < 3; r++)
			for (int c = 0; c < 3; c++)
				this.addSlot(new SlotItemHandler(te.getInventory(), c + r * 3, 62 + c * 18, 18 + r * 18));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 84 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 142));
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

	public static ObsidianMakerMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new ObsidianMakerMenu(id, inv, (ObsidianMakerBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
		if (index < 9) {
			if (!this.moveItemStackTo(stack, 9, this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
