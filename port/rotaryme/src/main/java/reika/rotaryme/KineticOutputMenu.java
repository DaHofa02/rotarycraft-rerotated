/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/** Card slots (acceleration and redstone) and the player inventory; speed and torque are set through {@link SetOutputPayload}. */
public class KineticOutputMenu extends KineticCardMenu<KineticOutputBlockEntity> {

	/** Lower than the input's, to make room for the lubricant lines. */
	public static final int OUTPUT_INV_Y = 150;

	public KineticOutputMenu(int id, Inventory inv, KineticOutputBlockEntity te) {
		super(RotaryME.KINETIC_OUTPUT_MENU.get(), id, inv, te, te.getUpgrades(), OUTPUT_INV_Y);
	}

	/** Button 0: switch the redstone card's mode (runs with / without a signal). */
	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id != 0 || !tile.hasRedstoneCard())
			return false;
		tile.toggleRedstoneMode();
		return true;
	}

	public static KineticOutputMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new KineticOutputMenu(id, inv, (KineticOutputBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}
}
