/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.content.menu;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import reika.electricraft.content.blockentity.RFCableBlockEntity;
import reika.electricraft.registry.ElectriMenus;

/** Original: CoreContainer for the RF cable - no slots. */
public class RFCableMenu extends AbstractContainerMenu {

	@Nullable
	public final RFCableBlockEntity tile;

	public RFCableMenu(int id, Inventory inv, @Nullable RFCableBlockEntity tile) {
		super(ElectriMenus.RF_CABLE.get(), id);
		this.tile = tile;
	}

	public static RFCableMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		BlockPos pos = buf.readBlockPos();
		return new RFCableMenu(id, inv, inv.player.level().getBlockEntity(pos) instanceof RFCableBlockEntity t ? t : null);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return tile != null && !tile.isRemoved() && tile.isPlayerAccessible(player);
	}
}
