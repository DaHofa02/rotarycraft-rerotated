/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.item;

import net.minecraft.client.gui.screens.Screen;

/** Client-only key state for tooltips; only loaded on the physical client. */
final class ClientKeys {

	private ClientKeys() {}

	static boolean isShiftDown() {
		return Screen.hasShiftDown();
	}
}
