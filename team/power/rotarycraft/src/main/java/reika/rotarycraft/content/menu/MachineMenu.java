/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.menu;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;

/**
 * Original: DragonAPI CoreContainer / RotaryCraft ContainerIOMachine and the per-machine containers. Machine values are read from the
 * block entity, which is already synced to nearby clients; only item slots go through the menu.
 */
public class MachineMenu extends AbstractContainerMenu {

	/** One entry per original container layout: machine slots (x, y) and whether the player inventory is shown. */
	public enum Layout {
		STEAM(false),				// ContainerSteam
		ETHANOL(true, 61, 36),		// ContainerEthanol
		PERFORMANCE(true, 58, 36, 103, 36),	// ContainerPerformance
		JET(true),					// ContainerJet (also the microturbine)
		ONE_SLOT(true, 80, 35),		// OneSlotContainer (AC engine)
		GEARBOX(false),				// ContainerGearbox
		SPLITTER(false),			// CoreContainer
		BEVEL(false);				// CoreContainer

		final boolean playerInv;
		final int[] slots;

		Layout(boolean playerInv, int... slots) {
			this.playerInv = playerInv;
			this.slots = slots;
		}
	}

	public final Layout layout;
	@Nullable
	public final RotaryCraftBlockEntity tile;
	private final int machineSlots;

	public MachineMenu(MenuType<?> type, int id, Inventory inv, Layout layout, @Nullable RotaryCraftBlockEntity tile) {
		super(type, id);
		this.layout = layout;
		this.tile = tile;
		IItemHandler handler = tile instanceof EngineBlockEntity e ? e.getInventory() : null;
		int n = 0;
		for (int i = 0; i+1 < layout.slots.length; i += 2) {
			if (handler != null)
				this.addSlot(new SlotItemHandler(handler, n, layout.slots[i], layout.slots[i+1]));
			n++;
		}
		machineSlots = handler != null ? n : 0;
		if (layout.playerInv) {
			for (int i = 0; i < 3; i++)
				for (int k = 0; k < 9; k++)
					this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 84 + i * 18));
			for (int j = 0; j < 9; j++)
				this.addSlot(new Slot(inv, j, 8 + j * 18, 142));
		}
	}

	public static MachineMenu fromNetwork(MenuType<?> type, int id, Inventory inv, Layout layout, RegistryFriendlyByteBuf buf) {
		BlockPos pos = buf.readBlockPos();
		BlockEntity be = inv.player.level().getBlockEntity(pos);
		return new MachineMenu(type, id, inv, layout, be instanceof RotaryCraftBlockEntity r ? r : null);
	}

	@Override
	public boolean stillValid(Player player) {
		return tile != null && !tile.isRemoved() && tile.isPlayerAccessible(player);
	}

	/** Shift-click between the machine slots and the player inventory (original: ReikaInventoryHelper transfer rules). */
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
		else if (machineSlots == 0 || !this.moveItemStackTo(stack, 0, machineSlots, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
