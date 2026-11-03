/*******************************************************************************
 * Original: Reika.RotaryCraft.API (RotaryCraft API by Reika Kalseki).
 * Adapted to Minecraft 1.21.1 types for a private port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.api.power;

import net.minecraft.core.BlockPos;

/** Implement this if your block entity "spawns" power. */
public interface PowerGenerator {

	/** The maximum amount of power your machine can ever generate, in watts. If there is no cap, use Long.MAX_VALUE. */
	long getMaxPower();

	/** The current power your machine is producing, in watts. */
	long getCurrentPower();

	/** The block position power is emitted into (the original exposed this as three getEmittingX/Y/Z methods). */
	BlockPos getEmittingPos();
}
