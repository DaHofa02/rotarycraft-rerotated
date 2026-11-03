/*******************************************************************************
 * Original: Reika.RotaryCraft.API (RotaryCraft API by Reika Kalseki).
 * Adapted to Minecraft 1.21.1 types for a private port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.api.power;

import net.minecraft.core.Direction;

/** Original: Reika.RotaryCraft.Auxiliary.ShaftPowerEmitter. A non-RC block entity that emits shaft power. */
public interface ShaftPowerEmitter extends ShaftMachine, PowerGenerator {

	/** Side to write to */
	boolean canWriteTo(Direction dir);

	/** Whether your machine is emitting power right now */
	boolean isEmitting();
}
