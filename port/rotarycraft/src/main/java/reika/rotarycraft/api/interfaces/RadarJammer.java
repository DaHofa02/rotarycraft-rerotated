/*******************************************************************************
 * Port of Reika.RotaryCraft.API.Interfaces.RadarJammer. Private NeoForge 1.21.1 port of RotaryCraft by Reika Kalseki; not for
 * distribution.
 ******************************************************************************/
package reika.rotarycraft.api.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** For living entities that blank out a Mob Radar which has them in range. */
public interface RadarJammer {

	boolean jamRadar(Level world, BlockPos radar);

}
