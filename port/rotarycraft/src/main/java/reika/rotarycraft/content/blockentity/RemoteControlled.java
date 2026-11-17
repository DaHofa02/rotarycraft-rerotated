/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.server.level.ServerPlayer;

/**
 * Original: Reika.RotaryCraft.Base.TileEntity.RemoteControlMachine (the camera side): a machine a screen with the same three dye
 * colours can call up.
 */
public interface RemoteControlled {

	/** The three dye colours (DyeColor ids), -1 for an empty or non-dye slot. */
	int[] getColors();

	/** Called by a screen whose colours match. */
	void activate(ServerPlayer ep);
}
