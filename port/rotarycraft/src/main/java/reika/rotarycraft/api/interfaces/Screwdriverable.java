/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft API). Adapted to Minecraft 1.21.1 types for a private port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.api.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Original: Reika.RotaryCraft.API.Interfaces.Screwdriverable - a block entity that reacts to the screwdriver itself. */
public interface Screwdriverable {

	/** Return true to prevent further processing. */
	boolean onShiftRightClick(Level world, BlockPos pos, Direction side);

	/** Return true to prevent further processing. */
	boolean onRightClick(Level world, BlockPos pos, Direction side);
}
