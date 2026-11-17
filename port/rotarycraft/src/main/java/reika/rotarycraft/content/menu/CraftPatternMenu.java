/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import reika.rotarycraft.content.item.CraftPatternItem;
import reika.rotarycraft.registry.RotaryItems;
import reika.rotarycraft.registry.RotaryMenus;

/** The pattern's 3x3 ghost grid (slots 0-8: clicking copies the carried item, or clears with an empty hand), the result (slot 9, display only) and the player inventory. */
public class CraftPatternMenu extends AbstractContainerMenu {

	private final InteractionHand hand;
	private final Player player;
	private final SimpleContainer grid = new SimpleContainer(10);

	public CraftPatternMenu(int id, Inventory inv, InteractionHand hand) {
		super(RotaryMenus.CRAFT_PATTERN.get(), id);
		this.hand = hand;
		player = inv.player;
		ItemStack pattern = this.pattern();
		var g = CraftPatternItem.getGrid(pattern);
		for (int i = 0; i < 9; i++)
			grid.setItem(i, g.get(i));
		grid.setItem(9, CraftPatternItem.getOutput(pattern));
		for (int i = 0; i < 9; i++)
			this.addSlot(new Ghost(grid, i, 30+(i%3)*18, 17+(i/3)*18));
		this.addSlot(new Ghost(grid, 9, 124, 35));
		for (int i = 0; i < 3; i++)
			for (int k = 0; k < 9; k++)
				this.addSlot(new Slot(inv, k+i*9+9, 8+k*18, 84+i*18));
		for (int j = 0; j < 9; j++)
			this.addSlot(new Slot(inv, j, 8+j*18, 142) {
				@Override
				public boolean mayPickup(Player p) {
					return !(hand == InteractionHand.MAIN_HAND && this.getContainerSlot() == p.getInventory().selected);
				}
			});
	}

	public static CraftPatternMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new CraftPatternMenu(id, inv, buf.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
	}

	private ItemStack pattern() {
		return player.getItemInHand(hand);
	}

	@Override
	public void clicked(int slot, int button, ClickType type, Player p) {
		if (slot >= 0 && slot < 9) {
			ItemStack carried = this.getCarried();
			grid.setItem(slot, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
			if (!p.level().isClientSide && this.pattern().is(RotaryItems.CRAFT_PATTERN.get())) {
				CraftPatternItem.setGrid(this.pattern(), grid.getItems().subList(0, 9), p.level());
				grid.setItem(9, CraftPatternItem.getOutput(this.pattern()));
			}
			this.broadcastChanges();
			return;
		}
		if (slot == 9)
			return;
		super.clicked(slot, button, type, p);
	}

	@Override
	public boolean stillValid(Player p) {
		return this.pattern().is(RotaryItems.CRAFT_PATTERN.get());
	}

	@Override
	public ItemStack quickMoveStack(Player p, int index) {
		return ItemStack.EMPTY;
	}

	private static class Ghost extends Slot {
		Ghost(Container c, int i, int x, int y) {
			super(c, i, x, y);
		}

		@Override
		public boolean mayPickup(Player p) {
			return false;
		}

		@Override
		public boolean mayPlace(ItemStack is) {
			return false;
		}
	}
}
