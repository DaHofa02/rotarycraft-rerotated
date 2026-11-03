/*******************************************************************************
 * @author Reika Kalseki (original DragonAPI). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.lib.math;

import net.minecraft.util.RandomSource;

/** Original: DragonAPI ReikaRandomHelper (the parts used so far). */
public final class RandomHelper {

	private RandomHelper() {}

	/** Original doWithChance: values above 1 are percent, values up to 1 a fraction. */
	public static boolean doWithChance(double num, RandomSource r) {
		if (num >= 100)
			return true;
		if (num > 1)
			num /= 100D;
		if (num >= 1)
			return true;
		if (num <= 0)
			return false;
		if (num < 10e-15) { //to help precision
			return r.nextDouble()*10e12 < num*10e12;
		}
		return r.nextDouble() < num;
	}
}
