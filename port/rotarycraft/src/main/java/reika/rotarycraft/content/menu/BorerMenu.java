/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import reika.rotarycraft.content.blockentity.BorerBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/**
 * Original: GuiBorer's CoreContainer (no slots). The GUI buttons reach the server as vanilla menu button clicks (the client's
 * ServerboundContainerButtonClickPacket, which the server only applies to the player's open, still valid menu) instead of the original
 * PacketRegistry.BORER / BORERTOGGLEALL / BORERRESET / BORERDROPS data packets:
 * buttons 0-34 toggle the pattern cell {@code i + 7*j} (column i 0-6, row j 0-4 from the top), {@link #TOGGLE_ALL}, {@link #RESET},
 * {@link #DROPS}.
 */
public class BorerMenu extends AbstractContainerMenu {

	public static final int TOGGLE_ALL = 35;
	public static final int RESET = 36;
	public static final int DROPS = 37;

	public final BorerBlockEntity tile;

	public BorerMenu(int id, Inventory inv, BorerBlockEntity te) {
		super(RotaryMenus.BORER.get(), id);
		tile = te;
	}

	public static BorerMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new BorerMenu(id, inv, (BorerBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	/** Button id of pattern cell (i, j). */
	public static int cellButton(int i, int j) {
		return i+BorerBlockEntity.WIDTH*j;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (tile == null || !this.stillValid(player))
			return false;
		if (id >= 0 && id < BorerBlockEntity.WIDTH*BorerBlockEntity.HEIGHT) {
			int i = id%BorerBlockEntity.WIDTH, j = id/BorerBlockEntity.WIDTH;
			tile.setCut(i, j, !tile.cutShape[i][j]);
			return true;
		}
		switch (id) {
			case TOGGLE_ALL -> tile.toggleAll();
			case RESET -> tile.reset();
			case DROPS -> tile.toggleDrops();
			default -> {
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
		return ItemStack.EMPTY;
	}
}
