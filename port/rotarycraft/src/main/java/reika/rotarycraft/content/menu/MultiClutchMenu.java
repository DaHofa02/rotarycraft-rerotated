/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import reika.rotarycraft.content.blockentity.MultiClutchBlockEntity;
import reika.rotarycraft.registry.RotaryMenus;

/** Original: CoreContainer for GuiMultiClutch - no slots; button i moves redstone level i on to the next side (PacketRegistry.MULTISIDE). */
public class MultiClutchMenu extends AbstractContainerMenu {

	public final MultiClutchBlockEntity tile;

	public MultiClutchMenu(int id, Inventory inv, MultiClutchBlockEntity te) {
		super(RotaryMenus.MULTI_CLUTCH.get(), id);
		tile = te;
	}

	public static MultiClutchMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new MultiClutchMenu(id, inv, (MultiClutchBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id < 0 || id >= 16)
			return false;
		tile.setSideOfState(id, tile.getNextSideForState(id));
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
