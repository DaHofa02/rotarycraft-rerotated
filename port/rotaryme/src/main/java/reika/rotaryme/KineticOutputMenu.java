/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** No slots; the speed and power are set through {@link SetOutputPayload}, the values shown come from the synced block entity. */
public class KineticOutputMenu extends AbstractContainerMenu {

	public final KineticOutputBlockEntity tile;

	public KineticOutputMenu(int id, Inventory inv, KineticOutputBlockEntity te) {
		super(RotaryME.KINETIC_OUTPUT_MENU.get(), id);
		tile = te;
	}

	public static KineticOutputMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new KineticOutputMenu(id, inv, (KineticOutputBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
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
