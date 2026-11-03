/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import reika.reactorcraft.registry.ReactorMenus;
import reika.reactorcraft.waste.WasteContainerBlockEntity;
import reika.reactorcraft.waste.WasteDecayerBlockEntity;
import reika.reactorcraft.waste.WasteStorageBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/**
 * Original: ContainerWasteContainer (9x3 at 8+18k, 22+18i), ContainerWasteStorage (12 SlotNuclearWaste in a diamond) and
 * ContainerWasteDecayer (5x3 at 44+18k), each with the player inventory (CoreContainer.addPlayerInventoryWithOffset 0/9, 0/20).
 */
public final class WasteMenus {

	private WasteMenus() {}

	public abstract static class Base extends AbstractContainerMenu {
		public final RotaryCraftBlockEntity tile;
		private final int machineSlots;

		protected Base(MenuType<?> type, int id, Inventory inv, RotaryCraftBlockEntity te, ItemStackHandler h, int[][] slots, int dy) {
			this(type, id, inv, te, h, slots, 0, dy, dy);
		}

		protected Base(MenuType<?> type, int id, Inventory inv, RotaryCraftBlockEntity te, ItemStackHandler h, int[][] slots, int dx, int dy, int dyHotbar) {
			super(type, id);
			tile = te;
			machineSlots = slots.length;
			for (int i = 0; i < slots.length; i++)
				this.addSlot(new SlotItemHandler(h, i, slots[i][0], slots[i][1]));
			for (int i = 0; i < 3; i++)
				for (int k = 0; k < 9; k++)
					this.addSlot(new Slot(inv, k+i*9+9, dx+8+k*18, dy+84+i*18));
			for (int j = 0; j < 9; j++)
				this.addSlot(new Slot(inv, j, dx+8+j*18, dyHotbar+142));
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
			if (index < machineSlots) {
				if (!this.moveItemStackTo(stack, machineSlots, this.slots.size(), true))
					return ItemStack.EMPTY;
			}
			else if (!this.moveItemStackTo(stack, 0, machineSlots, false)) {
				return ItemStack.EMPTY;
			}
			if (stack.isEmpty())
				slot.set(ItemStack.EMPTY);
			else
				slot.setChanged();
			return copy;
		}
	}

	private static int[][] grid(int w, int h, int x0) {
		int[][] s = new int[w*h][];
		for (int i = 0; i < h; i++)
			for (int k = 0; k < w; k++)
				s[i*w+k] = new int[]{x0+k*18, 22+i*18};
		return s;
	}

	public static class Container extends Base {
		public Container(int id, Inventory inv, WasteContainerBlockEntity te) {
			super(ReactorMenus.WASTE_CONTAINER.get(), id, inv, te, te.getInventory(), grid(9, 3, 44-36), 9);
		}

		public static Container fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
			return new Container(id, inv, (WasteContainerBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
		}
	}

	public static class Storage extends Base {
		private static final int[][] SLOTS = {{70, 20}, {90, 20}, {50, 40}, {70, 40}, {90, 40}, {110, 40}, {50, 60}, {70, 60}, {90, 60}, {110, 60}, {70, 80}, {90, 80}};

		public Storage(int id, Inventory inv, WasteStorageBlockEntity te) {
			super(ReactorMenus.WASTE_STORAGE.get(), id, inv, te, te.getInventory(), SLOTS, 20);
		}

		public static Storage fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
			return new Storage(id, inv, (WasteStorageBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
		}
	}

	/** Original ContainerPebbleBed: rows of 11, 11, 9, 7, 5, 3, 1 pebbles, the player inventory moved by 31/77 (hotbar 73). */
	public static class PebbleBed extends Base {
		private static int[][] slots() {
			int[] num = {11, 11, 9, 7, 5, 3, 1};
			java.util.List<int[]> li = new java.util.ArrayList<>();
			for (int f = 0; f < num.length; f++)
				for (int i = 0; i < num[f]; i++)
					li.add(new int[]{21+18*i+18*(11-num[f])/2, 25+18*f});
			return li.toArray(new int[0][]);
		}

		public PebbleBed(int id, Inventory inv, reika.reactorcraft.reactor.PebbleBedBlockEntity te) {
			super(ReactorMenus.PEBBLE_BED.get(), id, inv, te, te.getInventory(), slots(), 31, 77, 73);
		}

		public static PebbleBed fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
			return new PebbleBed(id, inv, (reika.reactorcraft.reactor.PebbleBedBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
		}
	}

	public static class Decayer extends Base {
		public Decayer(int id, Inventory inv, WasteDecayerBlockEntity te) {
			super(ReactorMenus.WASTE_DECAYER.get(), id, inv, te, te.getInventory(), grid(5, 3, 44), 9);
		}

		public static Decayer fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
			return new Decayer(id, inv, (WasteDecayerBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
		}
	}
}
