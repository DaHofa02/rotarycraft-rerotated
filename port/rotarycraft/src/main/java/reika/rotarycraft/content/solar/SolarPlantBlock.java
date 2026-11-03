/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.solar;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Original: Reika.RotaryCraft.Auxiliary.Interfaces.SolarPlantBlock - mirrors and solar tower blocks, which share one SolarPlant. */
public interface SolarPlantBlock {

	@Nullable
	SolarPlant getPlant();

	void setPlant(@Nullable SolarPlant p);

	void searchForPlant(Level world, BlockPos pos);
}
