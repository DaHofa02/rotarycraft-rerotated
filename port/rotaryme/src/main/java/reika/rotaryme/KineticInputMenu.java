/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/** Acceleration cards and the player inventory. */
public class KineticInputMenu extends KineticCardMenu<KineticInputBlockEntity> {

	public KineticInputMenu(int id, Inventory inv, KineticInputBlockEntity te) {
		super(RotaryME.KINETIC_INPUT_MENU.get(), id, inv, te, te.getUpgrades());
	}

	public static KineticInputMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
		return new KineticInputMenu(id, inv, (KineticInputBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()));
	}
}
