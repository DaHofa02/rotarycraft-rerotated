/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.api.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Original: Reika.RotaryCraft.API.Interfaces.Laserable - a block entity, block or entity the heat ray gives special effects. */
public interface Laserable {

	/** Called every tick it is in the beam of the heat ray. Args: heat ray power input, distance from the heat ray. */
	void whenInBeam(Level world, BlockPos pos, long power, int range);

	/** Whether it blocks the beam; true shields everything behind it. */
	boolean blockBeam(Level world, BlockPos pos, long power);
}
