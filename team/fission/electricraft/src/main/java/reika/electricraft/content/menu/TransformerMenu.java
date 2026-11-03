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

import reika.electricraft.content.blockentity.TransformerBlockEntity;
import reika.electricraft.registry.ElectriMenus;

/** Original: CoreContainer for the transformer - no slots, only keeps the GUI tied to the block. */
public class TransformerMenu extends AbstractContainerMenu {

	@Nullable
	public final TransformerBlockEntity tile;

	public TransformerMenu(int id, Inventory inv, @Nullable TransformerBlockEntity tile) {
		super(ElectriMenus.TRANSFORMER.get(), id);
		this.tile = tile;
	}

	public static TransformerMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		BlockPos pos = buf.readBlockPos();
		return new TransformerMenu(id, inv, inv.player.level().getBlockEntity(pos) instanceof TransformerBlockEntity t ? t : null);
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
