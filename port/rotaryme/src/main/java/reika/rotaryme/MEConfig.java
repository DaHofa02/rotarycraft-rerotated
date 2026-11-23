/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Config: the limits of the kinetic input and output, the input's loss, the output's lubricant use, the cards' effects and the cells' joules per bit. */
public final class MEConfig {

	public static final ModConfigSpec SPEC;
	public static final ModConfigSpec.IntValue MAX_SPEED;
	public static final ModConfigSpec.IntValue MAX_TORQUE;
	public static final ModConfigSpec.DoubleValue INPUT_LOSS;
	public static final ModConfigSpec.IntValue WATTS_PER_LUBRICANT;
	public static final ModConfigSpec.DoubleValue LOSS_PER_CARD;
	public static final ModConfigSpec.DoubleValue LUBRICANT_PER_CARD;
	public static final ModConfigSpec.IntValue JOULES_PER_BIT;

	static {
		ModConfigSpec.Builder b = new ModConfigSpec.Builder();
		MAX_SPEED = b.comment("Highest speed (rad/s) the kinetic input takes and the kinetic output gives").defineInRange("maxSpeed", 65536, 1, Integer.MAX_VALUE);
		MAX_TORQUE = b.comment("Highest torque (Nm) the kinetic input takes and the kinetic output gives").defineInRange("maxTorque", 65536, 1, Integer.MAX_VALUE);
		INPUT_LOSS = b.comment("Share of the shaft energy lost when the kinetic input stores it, without acceleration cards (see lossPerCard)").defineInRange("inputLoss", 0.05, 0, 0.99);
		LOSS_PER_CARD = b.comment("Loss each acceleration card (at most 4) takes off the kinetic input's loss").defineInRange("lossPerCard", 0.01, 0, 1);
		WATTS_PER_LUBRICANT = b.comment("Output power (W) per 1 mB/s of lubricant the kinetic output uses").defineInRange("wattsPerLubricant", 64000, 1, Integer.MAX_VALUE);
		LUBRICANT_PER_CARD = b.comment("Share of the kinetic output's lubricant use each acceleration card (at most 4) takes off").defineInRange("lubricantPerCard", 0.1875, 0, 0.25);
		JOULES_PER_BIT = b.comment("Kinetic energy (J) one bit of cell space holds; changing it changes every cell's capacity, not the joules already stored").defineInRange("joulesPerBit", 1024, 1, 1 << 20);
		SPEC = b.build();
	}

	private MEConfig() {}
}
