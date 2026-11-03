/*******************************************************************************
 * Original: Reika.RotaryCraft.API (RotaryCraft API by Reika Kalseki).
 * Adapted to Minecraft 1.21.1 types for a private port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.api.power;

/** A base class for the shaft power interfaces. Do not implement this directly; doing so is harmless but useless. */
public interface ShaftMachine {

	/** For fetching the current rotational speed. This can be called from both client and server. */
	int getOmega();

	/** For fetching the current torque. This can be called from both client and server. */
	int getTorque();

	/** For fetching the current power value. This can be called from both client and server. */
	long getPower();

	/** For when to write it to chat or the like. */
	String getName();

	/** Analogous to the IO machine "iotick". Used to control I/O render opacity. */
	int getIORenderAlpha();

	/** Analogous to the IO machine "iotick". Used to control I/O render opacity. This one is called by tools. */
	void setIORenderAlpha(int io);
}
