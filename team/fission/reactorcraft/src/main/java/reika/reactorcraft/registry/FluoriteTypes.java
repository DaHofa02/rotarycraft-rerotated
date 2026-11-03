/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.registry;

import java.util.Locale;

/** Original: Reika.ReactorCraft.Registry.FluoriteTypes - the eight fluorite colours and their particle colour. */
public enum FluoriteTypes {

	BLUE(0, 38, 255),
	PINK(255, 255, 236),
	ORANGE(255, 155, 0),
	MAGENTA(178, 0, 255),
	GREEN(0, 188, 18),
	RED(255, 50, 50),
	WHITE(255, 255, 255),
	YELLOW(255, 216, 0);

	public final int red;
	public final int green;
	public final int blue;

	public static final FluoriteTypes[] colorList = values();

	FluoriteTypes(int r, int g, int b) {
		red = r;
		green = g;
		blue = b;
	}

	public String getColorName() {
		return this.name().toLowerCase(Locale.ENGLISH);
	}

	/** Original getName: the colour with a capital first letter, in every language. */
	public String getName() {
		return this.name().charAt(0)+this.getColorName().substring(1);
	}

	public int getColor() {
		return (red << 16) | (green << 8) | blue;
	}
}
