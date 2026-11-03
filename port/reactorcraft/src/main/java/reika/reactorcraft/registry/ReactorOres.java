/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.registry;

import java.util.Locale;

/**
 * Original: Reika.ReactorCraft.Registry.ReactorOres - vein data (heights in 1.7.10 coordinates, kept as absolute Y), dimension
 * (0 overworld, 1 End, -1 Nether), harvest level, XP and the ore dictionary names the "generate even if other mods do" check uses.
 */
public enum ReactorOres {

	FLUORITE(		32, 60, 8, 	12, 0, 	0,	0.4F,	"fluorite"),
	PITCHBLENDE(	8, 	24, 16, 3, 	0,	1,	1F,		"uranium"),
	CADMIUM(		12, 32, 9, 	3, 	0,	2,	0.7F,	"cadmium"),
	INDIUM(			0, 	16, 7, 	2, 	0,	2,	1F,		"indium"),
	SILVER(			16, 40, 9, 	2, 	0,	2,	0.5F, 	"silver"),
	ENDBLENDE(		0, 	64, 16, 6, 	1,	1,	1F,		"uranium"),
	AMMONIUM(		32,	32,	8,	6,	-1,	1,	0.8F,	"ammonium"),
	CALCITE(		32, 60,	4,	12,	0,	0,	0.4F,	"calcite"),
	MAGNETITE(		60,	128,16,	7,	0,	1,	0.8F,	"magnetite"),
	THORIUM(		0,	32,	24,	1,	-1,	2,	0.8F,	"thorium");

	public final int minY;
	public final int maxY;
	public final int veinSize;
	/** Before the "Ore Density Percentage" multiplier. */
	public final int baseCount;
	public final int dimensionID;
	public final int harvestLevel;
	public final float xpDropped;
	/** Ore dictionary material (c:ores/&lt;name&gt;); both uraninite ores count as uranium. */
	public final String dictName;

	public static final ReactorOres[] oreList = values();

	ReactorOres(int min, int max, int size, int count, int dim, int level, float xp, String dict) {
		minY = min;
		maxY = max;
		veinSize = size;
		baseCount = count;
		dimensionID = dim;
		harvestLevel = level;
		xpDropped = xp;
		dictName = dict;
	}

	public String getName() {
		return this.name().toLowerCase(Locale.ENGLISH);
	}

	/** Original dropsSelf: calcite, ammonium and magnetite drop their mineral, fluorite its crystals. */
	public boolean dropsSelf() {
		return this != FLUORITE && this != CALCITE && this != AMMONIUM && this != MAGNETITE;
	}

	public boolean isUranium() {
		return this == PITCHBLENDE || this == ENDBLENDE;
	}

	public boolean isRadioactive() {
		return this == THORIUM || this.isUranium();
	}
}
