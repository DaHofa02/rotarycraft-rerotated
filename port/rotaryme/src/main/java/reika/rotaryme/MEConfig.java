/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server config: the limits of the kinetic input and output and the output's loss and lubricant use. */
public final class MEConfig {

	public static final ModConfigSpec SPEC;
	public static final ModConfigSpec.IntValue MAX_SPEED;
	public static final ModConfigSpec.IntValue MAX_TORQUE;
	public static final ModConfigSpec.DoubleValue OUTPUT_LOSS;
	public static final ModConfigSpec.IntValue WATTS_PER_LUBRICANT;

	static {
		ModConfigSpec.Builder b = new ModConfigSpec.Builder();
		MAX_SPEED = b.comment("Highest speed (rad/s) the kinetic input takes and the kinetic output gives").defineInRange("maxSpeed", 65536, 1, Integer.MAX_VALUE);
		MAX_TORQUE = b.comment("Highest torque (Nm) the kinetic input takes and the kinetic output gives").defineInRange("maxTorque", 65536, 1, Integer.MAX_VALUE);
		OUTPUT_LOSS = b.comment("Share of the energy lost when the kinetic output turns stored energy back into shaft power").defineInRange("outputLoss", 0.05, 0, 0.99);
		WATTS_PER_LUBRICANT = b.comment("Output power (W) per 1 mB/s of lubricant the kinetic output uses").defineInRange("wattsPerLubricant", 64000, 1, Integer.MAX_VALUE);
		SPEC = b.build();
	}

	private MEConfig() {}
}
