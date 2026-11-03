/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import reika.reactorcraft.reactor.CpuBlockEntity;
import reika.reactorcraft.registry.ReactorMenus;

/** Original: CoreContainer for GuiCPU - no slots. */
public class CpuMenu extends AbstractContainerMenu {

	public final CpuBlockEntity tile;

	public CpuMenu(int id, Inventory inv, CpuBlockEntity te) {
		super(ReactorMenus.CPU.get(), id);
		tile = te;
	}

	public static CpuMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new CpuMenu(id, inv, (CpuBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}

	@Override
	public boolean stillValid(Player player) {
		return tile != null && !tile.isRemoved(); //original CoreContainer.setAlwaysInteractable: also opened from afar with the remote
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}
}
