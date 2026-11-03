/*******************************************************************************
 * Original: Reika.RotaryCraft.API (RotaryCraft API by Reika Kalseki).
 * Adapted to Minecraft 1.21.1 types for a private port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.api.power;

import net.minecraft.core.Direction;

/** Extremely basic boolean sensitivity to RC power. Used for things like switches and detectors. */
public interface SimpleShaftPowerReceiver {

	void setPowered(boolean power);

	boolean canReadFrom(Direction dir);
}
