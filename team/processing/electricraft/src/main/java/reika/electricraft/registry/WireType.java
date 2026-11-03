/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.registry;

import java.util.Locale;

/**
 * Original: Reika.ElectriCraft.Registry.WireType - current limit (A) and resistance (V per block) per wire material. Values unchanged.
 * Crafting ingots: steel (RotaryCraft), gold (vanilla), the others from the c:ingots/* tags; superconductor wire has no recipe.
 */
public enum WireType {
	STEEL(			16, 				64),
	TIN(			64, 				32),
	NICKEL(			256, 				16),
	ALUMINUM(		1024, 				8),
	COPPER(			4096, 				2),
	SILVER(			32768, 				1),
	GOLD(			65536, 				4),
	PLATINUM(		131072, 			16),
	SUPERCONDUCTOR(	Integer.MAX_VALUE, 	0);

	public final int maxCurrent;
	public final int resistance;

	public static final WireType[] wireList = values();

	WireType(int max, int res) {
		maxCurrent = max;
		resistance = res;
	}

	public String getIconTexture() {
		return this.name().toLowerCase(Locale.ENGLISH);
	}

	public boolean hasGlowLayer() {
		return this == SUPERCONDUCTOR;
	}
}
