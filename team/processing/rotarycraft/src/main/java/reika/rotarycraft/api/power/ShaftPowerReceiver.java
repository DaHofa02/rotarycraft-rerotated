/*******************************************************************************
 * Original: Reika.RotaryCraft.API (RotaryCraft API by Reika Kalseki).
 * Adapted to Minecraft 1.21.1 types for a private port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.api.power;

/** This is for basic RC power compatibility. All "set" methods are called every tick. */
public interface ShaftPowerReceiver extends PowerAcceptor {

	/** RC machines set your machine's rotational speed with this. */
	void setOmega(int omega);

	/** RC machines set your machine's torque with this. */
	void setTorque(int torque);

	/** RC machines set your machine's power with this. You do not need to calculate power=omega*torque. */
	void setPower(long power);

	/** When there is no input machine. Usually used to set power, speed, torque = 0 */
	void noInputMachine();
}
