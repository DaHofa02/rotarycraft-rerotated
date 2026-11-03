/*******************************************************************************
 * Original: Reika.DragonAPI.Libraries.World.ReikaWorldHelper.getAmbientPressureAt, by Reika Kalseki.
 * Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.lib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.FlatLevelSource;

/**
 * Ambient pressure in kPa: 101.3 at the surface, 20000 in the Nether, 24 in the End; weather lowers it; deep underground raises it,
 * high altitude lowers it. The absolute heights of the 1.7.10 original are kept, limited to its world height (0-255), so the deeper
 * and higher 1.21 worlds do not push the factors past the original range (a negative pressure above y=300). Not ported: the liquid column (only used with
 * checkLiquidColumn) and other mods' atmosphere densities (AtmosphereHandler, 1 without Galacticraft).
 */
public final class AmbientPressure {

	private AmbientPressure() {}

	private static boolean isFlat(Level world) {
		return world instanceof ServerLevel sl && sl.getChunkSource().getGenerator() instanceof FlatLevelSource;
	}

	public static double getAmbientPressureAt(Level world, BlockPos pos) {
		double Pamb = 101.3;
		if (world.dimension() == Level.NETHER)
			Pamb = 20000;
		else if (world.dimension() == Level.END)
			Pamb = 24; //0.2atm

		if (world.isThundering())
			Pamb -= 5;
		else if (world.isRaining())
			Pamb -= 2.5;

		int y = Math.min(255, Math.max(0, pos.getY()));
		boolean flat = isFlat(world);
		if (y < 30 && !flat) {
			double f = (30-y)/30D;
			Pamb *= 1+0.5*f;
		}
		if (y > 64 && flat) {
			double f = (y-64)/(128D-64D);
			Pamb *= 1-0.2*f;
		}
		if (y > 128) {
			double f = (y-128)/(192D-128D);
			Pamb *= 1-0.2*f;
		}
		if (y > 192) {
			double f = (y-128)/(256D-192D);
			Pamb *= 1-0.4*f;
		}
		return Pamb;
	}
}
