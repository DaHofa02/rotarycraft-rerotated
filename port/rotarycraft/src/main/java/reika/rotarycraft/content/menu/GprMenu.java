/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import reika.rotarycraft.content.blockentity.GprBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/**
 * Original: GuiGPR's CoreContainer (no slots). The GUI's key presses reach the server as vanilla menu button clicks instead of the
 * original PacketRegistry.GPR packet: 0 moves the scan point +1 along the GUI direction, 1 moves it -1, 2 resets it.
 */
public class GprMenu extends AbstractContainerMenu {

	public final GprBlockEntity tile;

	public GprMenu(int id, Inventory inv, GprBlockEntity te) {
		super(RotaryMenus.GPR.get(), id);
		tile = te;
	}

	public static GprMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new GprMenu(id, inv, (GprBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (tile == null || !this.stillValid(player) || id < 0 || id > 2)
			return false;
		tile.shift(tile.getGuiDirection(), id == 0 ? 1 : id == 1 ? -1 : 0);
		return true;
	}

	@Override
	public boolean stillValid(Player player) {
		return tile != null && !tile.isRemoved() && tile.isPlayerAccessible(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}
}
