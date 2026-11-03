/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.radiation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import reika.reactorcraft.content.item.NuclearWasteItem;

/** Original: Reika.ReactorCraft.Auxiliary.WasteManager - fission product yields (%) for the waste a fuel rod makes. */
public final class WasteManager {

	private static final List<Isotopes> wastes = new ArrayList<>();
	private static final List<Double> yields = new ArrayList<>();
	private static double total;

	private static void addWaste(Isotopes iso, double percent) {
		wastes.add(iso);
		yields.add(percent);
		total += percent;
	}

	static {
		addWaste(Isotopes.Cs134, 6.79);
		addWaste(Isotopes.Xe135, 6.33);
		addWaste(Isotopes.Zr93, 6.30);
		addWaste(Isotopes.Mo99, 6.10);
		addWaste(Isotopes.Cs137, 6.09);
		addWaste(Isotopes.Tc99, 6.05);
		addWaste(Isotopes.Sr90, 5.75);
		addWaste(Isotopes.I131, 2.83);
		addWaste(Isotopes.Pm147, 2.27);
		addWaste(Isotopes.I129, 0.66);
		addWaste(Isotopes.Sm151, 0.42);
		addWaste(Isotopes.Ru106, 0.39);
		addWaste(Isotopes.Kr85, 0.27);
		addWaste(Isotopes.Pd107, 0.16);
		addWaste(Isotopes.Se79, 0.05);
		addWaste(Isotopes.Gd155, 0.03);
		addWaste(Isotopes.Sb125, 0.03);
		addWaste(Isotopes.Sn126, 0.02);
	}

	private WasteManager() {}

	public static Isotopes getRandomWaste(RandomSource r) {
		double d = r.nextDouble()*total;
		for (int i = 0; i < wastes.size(); i++) {
			d -= yields.get(i);
			if (d < 0)
				return wastes.get(i);
		}
		return wastes.get(wastes.size()-1);
	}

	public static ItemStack getRandomWasteItem(RandomSource r) {
		return NuclearWasteItem.of(getRandomWaste(r));
	}

	public static List<Isotopes> getWasteList() {
		return Collections.unmodifiableList(wastes);
	}
}
