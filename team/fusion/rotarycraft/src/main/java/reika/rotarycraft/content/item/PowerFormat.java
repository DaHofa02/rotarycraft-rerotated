/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.item;

import reika.rotarycraft.lib.math.ReikaEngLibrary;
import reika.rotarycraft.lib.math.ReikaMathLibrary;

/** Original: RotaryAux.formatTorque/formatSpeed/formatPower/formatPowerIO. The "old textures" imperial-unit mode is not ported. */
public final class PowerFormat {

	private PowerFormat() {}

	public static String formatTorque(double t) {
		return String.format("%.0f %s%s", ReikaMathLibrary.getThousandBase(t), ReikaEngLibrary.getSIPrefix(t), "Nm");
	}

	public static String formatSpeed(double s) {
		return String.format("%.0f %s%s", ReikaMathLibrary.getThousandBase(s), ReikaEngLibrary.getSIPrefix(s), "rad/s");
	}

	public static String formatPower(double p) {
		return String.format("%.3f%s%s", ReikaMathLibrary.getThousandBase(p), ReikaEngLibrary.getSIPrefix(p), "W");
	}

	public static String formatPowerIO(double speed, double power) {
		return String.format("%.3f%s%s @ %.0f %s", ReikaMathLibrary.getThousandBase(power), ReikaEngLibrary.getSIPrefix(power), "W", speed, "rad/s");
	}
}
