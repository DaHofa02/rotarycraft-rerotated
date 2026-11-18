/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme.compat;

import net.neoforged.fml.ModList;

/** Whether Project Red Transmission is installed; free of its classes, so {@link ProjectRedCompat} is only loaded after this says yes. */
public final class OptionalMods {

	public static final String PROJECTRED_TRANSMISSION = "projectred_transmission";

	private OptionalMods() {}

	public static boolean projectRed() {
		return ModList.get() != null && ModList.get().isLoaded(PROJECTRED_TRANSMISSION);
	}
}
