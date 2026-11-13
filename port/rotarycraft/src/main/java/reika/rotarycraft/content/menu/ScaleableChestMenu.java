/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

import reika.rotarycraft.content.blockentity.ScaleableChestBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/**
 * Original: ContainerScaleChest - one page of up to 54 open slots (8+18c, 18+18r) and the player inventory below six rows. Buttons 0
 * (next) and 1 (back) reopen the menu on the other page, as the original's PacketRegistry.CHEST did. Change: Next reaches every page
 * with slots (the original GUI stopped one short, so a partly open last page could not be reached).
 */
public class ScaleableChestMenu extends AbstractContainerMenu {

	public final ScaleableChestBlockEntity tile;
	public final int page;
	private final int pageSlots;

	public ScaleableChestMenu(int id, Inventory inv, ScaleableChestBlockEntity te, int page) {
		super(RotaryMenus.SCALEABLE_CHEST.get(), id);
		tile = te;
		this.page = page;
		pageSlots = Math.max(0, te.getSlotsOnPage(page));
		int offset = page*54;
		for (int i = 0; i < pageSlots; i++)
			this.addSlot(new SlotItemHandler(te.getInventory(), offset+i, 8+18*(i%9), 18+i/9*18));
		for (int k = 0; k < 3; k++)
			for (int m = 0; m < 9; m++)
				this.addSlot(new Slot(inv, m + k * 9 + 9, 8 + 18 * m, 139 + k * 18));
		for (int m = 0; m < 9; m++)
			this.addSlot(new Slot(inv, m, 8 + 18 * m, 197));
	}

	public static ScaleableChestMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		ScaleableChestBlockEntity te = (ScaleableChestBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos());
		return new ScaleableChestMenu(id, inv, te, buf.readVarInt());
	}

	public static void open(ServerPlayer sp, ScaleableChestBlockEntity te, int page) {
		sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new ScaleableChestMenu(id, inv, te, page), te.getBlockState().getBlock().getName()),
				buf -> {
					buf.writeBlockPos(te.getBlockPos());
					buf.writeVarInt(page);
				});
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		int target = page;
		if (id == 0 && page < tile.getMaxPage() && tile.getSlotsOnPage(page+1) > 0)
			target++;
		if (id == 1 && page > 0)
			target--;
		if (target == page || !(player instanceof ServerPlayer sp))
			return false;
		open(sp, tile, target);
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
		if (index < pageSlots) {
			if (!this.moveItemStackTo(stack, pageSlots, this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if (!this.moveItemStackTo(stack, 0, pageSlots, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}
}
