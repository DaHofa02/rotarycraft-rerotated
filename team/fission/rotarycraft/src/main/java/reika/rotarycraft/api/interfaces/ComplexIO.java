/*******************************************************************************
 * Original: Reika.RotaryCraft.API (RotaryCraft API by Reika Kalseki).
 * Adapted to Minecraft 1.21.1 types for a private port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.api.interfaces;

import net.minecraft.core.Direction;

public interface ComplexIO {

	int getSpeedToSide(Direction dir);

	int getTorqueToSide(Direction dir);
}
