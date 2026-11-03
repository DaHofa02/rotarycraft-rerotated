/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import reika.reactorcraft.reactor.ThoriumCoreBlockEntity;
import reika.reactorcraft.registry.ReactorMenus;

/** Original: ContainerThoriumCore - no slots; the three tanks are synced with the block entity. */
public class ThoriumCoreMenu extends AbstractContainerMenu {

	public final ThoriumCoreBlockEntity tile;

	public ThoriumCoreMenu(int id, Inventory inv, ThoriumCoreBlockEntity te) {
		super(ReactorMenus.THORIUM_CORE.get(), id);
		tile = te;
	}

	public static ThoriumCoreMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new ThoriumCoreMenu(id, inv, (ThoriumCoreBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
