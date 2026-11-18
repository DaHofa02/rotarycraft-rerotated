/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import mrtjp.projectred.api.IBundledTileInteraction;
import mrtjp.projectred.api.ProjectRedAPI;

import reika.rotaryme.BundledBusBlockEntity;

/** Everything that touches Project Red's API. Callers check {@link OptionalMods#projectRed()} first; without Project Red this class is never loaded. */
public final class ProjectRedCompat {

	private ProjectRedCompat() {}

	/** Strongest bundled input per colour (0-255) over all six sides. */
	public static int[] getBundledInput(Level world, BlockPos pos) {
		int[] ret = new int[16];
		if (ProjectRedAPI.transmissionAPI == null)
			return ret;
		for (Direction d : Direction.values()) {
			byte[] data = ProjectRedAPI.transmissionAPI.getBundledInput(world, pos, d);
			if (data != null)
				for (int k = 0; k < 16; k++)
					ret[k] = Math.max(ret[k], data[k] & 255);
		}
		return ret;
	}

	/** Lets bundled cables connect to the bus (it only listens, so its own signal is empty). */
	public static void registerInteraction() {
		if (ProjectRedAPI.transmissionAPI == null)
			return;
		ProjectRedAPI.transmissionAPI.registerBundledTileInteraction(new IBundledTileInteraction() {
			@Override
			public boolean isValidInteractionFor(Level world, BlockPos pos, Direction side) {
				return world.getBlockEntity(pos) instanceof BundledBusBlockEntity;
			}

			@Override
			public boolean canConnectBundled(Level world, BlockPos pos, Direction side) {
				return true;
			}

			@Override
			public byte[] getBundledSignal(Level world, BlockPos pos, Direction side) {
				return new byte[16];
			}
		});
	}
}
