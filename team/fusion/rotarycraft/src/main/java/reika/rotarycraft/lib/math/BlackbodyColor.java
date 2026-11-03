/*******************************************************************************
 * @author Reika Kalseki (original DragonAPI). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.lib.math;

/** Original: DragonAPI ReikaPhysicsHelper.getColorForTemperature (Interpolation over tempColorList, colour mixing per channel). */
public final class BlackbodyColor {

	private static final int[] TEMPS = {500, 700, 1700, 2700, 3700, 5700, 9700};
	private static final int[] COLORS = {0x000000, 0x9C0000, 0xff0000, 0xFF9C00, 0xFFFF00, 0xFFFFFF, 0x56C4FF};

	private BlackbodyColor() {}

	public static int getColorForTemperature(int temp) {
		if (temp <= TEMPS[0])
			return COLORS[0];
		for (int i = 1; i < TEMPS.length; i++) {
			if (temp <= TEMPS[i]) {
				if (temp == TEMPS[i])
					return COLORS[i];
				float f = (float)(temp-TEMPS[i-1])/(TEMPS[i]-TEMPS[i-1]);
				return mixColors(COLORS[i], COLORS[i-1], f);
			}
		}
		return COLORS[COLORS.length-1];
	}

	/** Original: ReikaColorAPI.mixColors - ratio is the weight of c1. */
	public static int mixColors(int c1, int c2, float ratio) {
		int r = (int)(((c1 >> 16) & 0xff)*ratio+((c2 >> 16) & 0xff)*(1-ratio));
		int g = (int)(((c1 >> 8) & 0xff)*ratio+((c2 >> 8) & 0xff)*(1-ratio));
		int b = (int)((c1 & 0xff)*ratio+(c2 & 0xff)*(1-ratio));
		return (r << 16) | (g << 8) | b;
	}
}
