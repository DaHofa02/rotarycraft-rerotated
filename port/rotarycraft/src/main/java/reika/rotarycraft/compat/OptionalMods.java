/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.compat;

import net.neoforged.fml.ModList;

/**
 * Which optional mods are installed. Kept free of their classes: the compat classes ({@link PneumaticCompat}) are only touched after a
 * check here, so they are never loaded without their mod.
 */
public final class OptionalMods {

	public static final String PNEUMATICCRAFT = "pneumaticcraft";

	private OptionalMods() {}

	public static boolean isLoaded(String mod) {
		return ModList.get() != null && ModList.get().isLoaded(mod);
	}

	public static boolean pneumaticCraft() {
		return isLoaded(PNEUMATICCRAFT);
	}
}
