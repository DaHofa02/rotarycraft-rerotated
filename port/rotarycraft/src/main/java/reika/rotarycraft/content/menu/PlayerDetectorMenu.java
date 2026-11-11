/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import reika.rotarycraft.content.blockentity.PlayerDetectorBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/**
 * Original: GuiPlayerDetector's CoreContainer (no slots). The range typed into the GUI reaches the server as a vanilla menu button
 * click whose button id is the range (0-999), instead of the original PacketRegistry.DETECTOR data packet.
 */
public class PlayerDetectorMenu extends AbstractContainerMenu {

	public final PlayerDetectorBlockEntity tile;

	public PlayerDetectorMenu(int id, Inventory inv, PlayerDetectorBlockEntity te) {
		super(RotaryMenus.PLAYER_DETECTOR.get(), id);
		tile = te;
	}

	public static PlayerDetectorMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new PlayerDetectorMenu(id, inv, (PlayerDetectorBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (tile == null || !this.stillValid(player) || id < 0 || id > 999)
			return false;
		tile.setSelectedRange(id);
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
