/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.registry;

import java.util.Locale;

import reika.rotarycraft.lib.math.ReikaEngLibrary;
import reika.rotarycraft.lib.math.ReikaMathLibrary;

/** Original: Reika.ElectriCraft.Registry.BatteryType - capacity 2^n J and the fixed output (V, A) of each battery tier. */
public enum BatteryType {

	REDSTONE(	20,		256, 		32),
	GLOWSTONE(	24, 	512, 		64),
	LAPIS(		28, 	2048, 		128),
	ENDER(		33, 	4096, 		256),
	DIAMOND(	40, 	16384, 		1024),
	STAR(		48, 	65536,		4096);

	public final long maxCapacity;
	public final int outputVoltage;
	public final int outputCurrent;

	public static final BatteryType[] batteryList = values();

	BatteryType(int cap, int v, int a) {
		maxCapacity = 1L << cap;
		outputCurrent = a;
		outputVoltage = v;
	}

	public String getName() {
		return this.name().toLowerCase(Locale.ENGLISH);
	}

	public String getFormattedCapacity() {
		return String.format("%.3f%sJ", ReikaMathLibrary.getThousandBase(maxCapacity), ReikaEngLibrary.getSIPrefix(maxCapacity));
	}
}
