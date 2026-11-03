/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import reika.rotarycraft.content.blockentity.WorktableBlockEntity;
import reika.rotarycraft.content.recipe.WorktableRecipe;
import reika.rotarycraft.registry.RotaryMenus;

/**
 * Original: ContainerWorktable and DragonAPI CraftingContainer. Grid (26,17), outputs (98,17). Clicking the (empty) main output while
 * the outputs are clear crafts once and puts the result on the cursor; Shift-click crafts as often as the smallest input stack allows.
 * The crafting pattern slot (6,53) is not ported yet.
 */
public class WorktableMenu extends AbstractContainerMenu {

	public final WorktableBlockEntity tile;
	private static final int GRID = 0;
	private static final int OUTPUTS = 9;
	private static final int MAIN_OUTPUT = OUTPUTS+4;
	private static final int PLAYER = 18;

	public WorktableMenu(int id, Inventory inv, WorktableBlockEntity te) {
		super(RotaryMenus.WORKTABLE.get(), id);
		tile = te;
		ItemStackHandler h = te.getInventory();
		int dx = 0;
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				this.addSlot(new SlotItemHandler(h, i*3+j, dx+26+j*18, 17+i*18));
		dx += 96-28+4;
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				this.addSlot(new BlastFurnaceMenu.OutputSlot(h, 9+i*3+j, dx+26+j*18, 17+i*18));

		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k + i * 9 + 9, 8 + k * 18, 84 + i * 18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8 + j * 18, 142));
	}

	public static WorktableMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new WorktableMenu(id, inv, (WorktableBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	/** Original CraftingContainer.slotClick: the click goes through normally, then a click on the main output crafts. */
	@Override
	public void clicked(int slotId, int button, ClickType type, Player player) {
		super.clicked(slotId, button, type, player);
		if (slotId != MAIN_OUTPUT || player.level().isClientSide || (type != ClickType.PICKUP && type != ClickType.QUICK_MOVE))
			return;
		WorktableRecipe wr = WorktableBlockEntity.findMatchingRecipe(player.level(), tile.getInventory());
		if (wr != null && tile.isReadyToCraft()) {
			ItemStack drop = this.getCarried();
			ItemStack craft = wr.getResultItem(player.level().registryAccess());
			if (!drop.isEmpty() && (!ItemStack.isSameItemSameComponents(drop, craft) || drop.getCount()+craft.getCount() > drop.getMaxStackSize()))
				return;
			if (!tile.handleCrafting(wr, player, type == ClickType.QUICK_MOVE))
				return;
			ItemStack made = tile.getInventory().getStackInSlot(WorktableBlockEntity.OUTPUT);
			if (made.isEmpty()) //recycling puts its scrap into the output grid
				return;
			if (drop.isEmpty())
				this.setCarried(made.copy());
			else
				drop.grow(made.getCount());
			tile.getInventory().setStackInSlot(WorktableBlockEntity.OUTPUT, ItemStack.EMPTY);
			this.broadcastChanges();
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return tile != null && !tile.isRemoved() && tile.isPlayerAccessible(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (index == MAIN_OUTPUT && tile.getInventory().getStackInSlot(WorktableBlockEntity.OUTPUT).isEmpty())
			return ItemStack.EMPTY; //handled by the crafting click
		Slot slot = this.slots.get(index);
		if (!slot.hasItem())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index < PLAYER) {
			if (!this.moveItemStackTo(stack, PLAYER, this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!this.moveItemStackTo(stack, GRID, OUTPUTS, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
