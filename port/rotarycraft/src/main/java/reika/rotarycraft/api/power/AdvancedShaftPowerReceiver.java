/*******************************************************************************
 * Original: Reika.RotaryCraft.API (RotaryCraft API by Reika Kalseki).
 * Adapted to Minecraft 1.21.1 types for a private port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.api.power;

import net.minecraft.core.Direction;

/** Implement this instead of {@link ShaftPowerReceiver} for more advanced and dynamic control over power reception.
 * Set the power to zero at the end of the tick, allowing for it to be set again if the machine remains powered. */
public interface AdvancedShaftPowerReceiver extends PowerAcceptor {

	/** Called every tick to add power at a given torque and speed, to a given side. Standard RC behavior for multiple inputs is to sum
	 * the torque of all the inputs at the highest speed and ignore all others (eg 512@32+256@256+64@256 = 320@256). */
	boolean addPower(int torque, int omega, long power, Direction side);
}
