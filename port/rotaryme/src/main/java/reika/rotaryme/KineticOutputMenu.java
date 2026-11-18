/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/** Acceleration cards and the player inventory; the speed and power are set through {@link SetOutputPayload}. */
public class KineticOutputMenu extends KineticCardMenu<KineticOutputBlockEntity> {

	public KineticOutputMenu(int id, Inventory inv, KineticOutputBlockEntity te) {
		super(RotaryME.KINETIC_OUTPUT_MENU.get(), id, inv, te, te.getUpgrades());
	}

	public static KineticOutputMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new KineticOutputMenu(id, inv, (KineticOutputBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}
}
