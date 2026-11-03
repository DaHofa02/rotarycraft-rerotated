/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.registry;

import java.util.Locale;

/**
 * Original: Reika.ElectriCraft.Registry.ElectriOres - vein data (heights in 1.7.10 world coordinates, kept as absolute Y), smelting
 * XP and harvest level. Copper is not registered: the original only generated its ore when no other "oreCopper" existed, and vanilla
 * 1.21 has copper ore and ingots, which the port uses (c:ores/copper, c:ingots/copper).
 */
public enum ElectriOres {

	COPPER(		32, 64, 	6, 	8, 	0.5F,	1),
	TIN(		48, 72, 	8, 	8,	0.2F,	1),
	SILVER(		0, 32, 		6, 	4, 	0.8F,	2),
	NICKEL(		16, 48, 	8, 	6, 	0.3F,	1),
	ALUMINUM(	60, 128, 	8, 	10,	0.4F,	1),
	PLATINUM(	0, 16, 		4, 	2, 	1F,		2);

	public final int minY;
	public final int maxY;
	public final int veinSize;
	public final int perChunk;
	public final float xpDropped;
	public final int harvestLevel;

	public static final ElectriOres[] oreList = values();

	ElectriOres(int min, int max, int size, int count, float xp, int level) {
		minY = min;
		maxY = max;
		veinSize = size;
		perChunk = count;
		xpDropped = xp;
		harvestLevel = level;
	}

	public String getName() {
		return this.name().toLowerCase(Locale.ENGLISH);
	}

	/** Copper comes from vanilla. */
	public boolean isPorted() {
		return this != COPPER;
	}
}
