/*******************************************************************************
 * Original: Reika.RotaryCraft.API (RotaryCraft API by Reika Kalseki).
 * Adapted to Minecraft 1.21.1 types for a private port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.api;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;

/** The positions an IO machine writes power to (the original exposed these as getWriteX/Y/Z and getWriteX2/Y2/Z2). */
public interface IOMachine {

	@Nullable
	BlockPos getWritePos();

	@Nullable
	BlockPos getWritePos2();
}
