/*******************************************************************************
 * Original: Reika.RotaryCraft.API (RotaryCraft API by Reika Kalseki).
 * Adapted to Minecraft 1.21.1 types for a private port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.api.power;

import net.minecraft.core.Direction;

/** Anything that receives power implements this, usually indirectly. Do not implement this directly. */
public interface PowerAcceptor extends ShaftMachine {

	/** Can the machine receive power from this side. Usually either "dir == facing" for 1D machines and "true" for omnidirectional machines. */
	boolean canReadFrom(Direction dir);

	/** Whether your machine is able to receive power right now. If this returns false, no other power code is called. */
	boolean isReceiving();

	/** The minimum torque the machine requires to operate. Also controls flywheel deceleration; a value of zero means flywheels driving it
	 * never decelerate, yielding infinite energy. To have no effective minimum, pick a value of one; a torque of zero yields zero power. */
	int getMinTorque(int available);
}
