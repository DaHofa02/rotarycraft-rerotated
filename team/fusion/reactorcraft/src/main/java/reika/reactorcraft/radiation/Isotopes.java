/*******************************************************************************
 * @author Reika Kalseki (original DragonAPI). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.radiation;

import reika.rotarycraft.lib.math.ReikaEngLibrary;
import reika.rotarycraft.lib.math.ReikaMathLibrary;

/** Original: Reika.DragonAPI.Libraries.MathSci.Isotopes (name, half-life, element group; the order is the waste item's id). */
public enum Isotopes {

	C14(5730, Unit.YEAR, "Carbon-14", ElementGroup.NONMETAL),
	U235(704e6, Unit.YEAR, "Uranium-235", ElementGroup.LANTHACTINIDE),
	U238(4.468e9, Unit.YEAR, "Uranium-238", ElementGroup.LANTHACTINIDE),
	Pu239(2410, Unit.YEAR, "Plutonium-239", ElementGroup.LANTHACTINIDE, true),
	Pu244(80.8e6, Unit.YEAR, "Plutonium-244", ElementGroup.LANTHACTINIDE),
	Th232(14.05e9, Unit.YEAR, "Thorium-232", ElementGroup.LANTHACTINIDE),
	Rn222(3.8235, Unit.DAY, "Radon-222", ElementGroup.NONMETAL, true),
	Ra226(1601, Unit.YEAR, "Radium-226", ElementGroup.ALKALI, true),
	Sr90(28.9, Unit.YEAR, "Strontium-90", ElementGroup.ALKALI, true),
	Po210(138.376, Unit.DAY, "Polonium-210", ElementGroup.TRANSITION, true),
	Cs134(2.065, Unit.YEAR, "Cesium-134", ElementGroup.ALKALI),
	Xe135(6.57, Unit.HOUR, "Xenon-135", ElementGroup.NONMETAL),
	Zr93(1.53e6, Unit.YEAR, "Zirconium-93", ElementGroup.TRANSITION),
	Mo99(65.94, Unit.HOUR, "Molybdenum-99", ElementGroup.TRANSITION),
	Cs137(30.17, Unit.YEAR, "Cesium-137", ElementGroup.ALKALI, true),
	Tc99(211000, Unit.YEAR, "Technetium-99", ElementGroup.TRANSITION),
	I131(8.02, Unit.DAY, "Iodine-131", ElementGroup.NONMETAL, true),
	Pm147(2.62, Unit.YEAR, "Promethium-147", ElementGroup.LANTHACTINIDE),
	I129(15.7e6, Unit.YEAR, "Iodine-129", ElementGroup.NONMETAL),
	Sm151(90, Unit.YEAR, "Samarium-151", ElementGroup.LANTHACTINIDE),
	Ru106(373.6, Unit.DAY, "Ruthenium-106", ElementGroup.TRANSITION),
	Kr85(10.78, Unit.YEAR, "Krypton-85", ElementGroup.NONMETAL),
	Pd107(6.5e6, Unit.YEAR, "Palladium-107", ElementGroup.TRANSITION),
	Se79(327000, Unit.YEAR, "Selenium-79", ElementGroup.NONMETAL),
	Gd155(4.76, Unit.YEAR, "Gadolinium-155", ElementGroup.LANTHACTINIDE),
	Sb125(2.76, Unit.YEAR, "Antimony-125", ElementGroup.TRANSITION),
	Sn126(230000, Unit.YEAR, "Tin-126", ElementGroup.TRANSITION),
	Xe136(10e21, Unit.YEAR, "Xenon-136", ElementGroup.NONMETAL),
	I135(6.6, Unit.HOUR, "Iodine-135", ElementGroup.NONMETAL),
	Xe131(12, Unit.DAY, "Xenon-131", ElementGroup.NONMETAL),
	Ru103(1.69, Unit.TICK, "Ruthenium-103", ElementGroup.TRANSITION),
	Pm149(53.08, Unit.HOUR, "Promethium-149", ElementGroup.LANTHACTINIDE),
	Rh105(35.36, Unit.HOUR, "Rhodium-105", ElementGroup.TRANSITION);

	/** Original ReikaTimeHelper: abbreviation and getMinecraftDuration in ticks (year = 6 moon months = 48 days of 24000 ticks, hour = 25 s). */
	public enum Unit {
		YEAR("y", 24000*8*6), DAY("d", 24000), HOUR("h", 500), TICK("t", 1);

		final String abbreviation;
		final int minecraftDuration;

		Unit(String a, int mc) {
			abbreviation = a;
			minecraftDuration = mc;
		}
	}

	public enum ElementGroup {
		ALKALI("Alkali Metals"),
		TRANSITION("Transition Metals"),
		LANTHACTINIDE("Lanthanides and Actinides"),
		NONMETAL("Nonmetals");

		public final String displayName;

		ElementGroup(String n) {
			displayName = n;
		}
	}

	private final double half;
	private final Unit base;
	private final String name;
	public final boolean extraDanger;
	public final ElementGroup group;

	public static final Isotopes[] isoList = values();

	Isotopes(double t, Unit u, String n, ElementGroup g) {
		this(t, u, n, g, false);
	}

	Isotopes(double t, Unit u, String n, ElementGroup g, boolean d) {
		half = t;
		base = u;
		name = n;
		group = g;
		extraDanger = d;
	}

	public String getName() {
		return name;
	}

	/** Original atomicWeight and element (parsed from the constant name, e.g. Cs137 -> Cs, 137). */
	public int getAtomicWeight() {
		return Integer.parseInt(this.name().replaceAll("^[A-Za-z]+", ""));
	}

	public String getChemicalSymbol() {
		return this.name().replaceAll("[0-9]+$", "");
	}

	/** What an isotope decays into in the waste decayer: one of these isotopes or lead (Pb, isotope null). */
	public record DecayProduct(@javax.annotation.Nullable Isotopes isotope, String symbol, int weight) {}

	public record DecayData(DecayProduct product, double amount) {}

	private static final java.util.Map<Isotopes, DecayData> DECAY = new java.util.EnumMap<>(Isotopes.class);

	/** Original Isotopes.getDecay (computeDecayData); null = it decays into nothing. */
	@javax.annotation.Nullable
	public DecayData getDecay() {
		if (DECAY.isEmpty()) {
			for (Isotopes i : isoList)
				DECAY.put(i, computeDecayData(i));
		}
		return DECAY.get(this);
	}

	/** Original computeDecayData: the element one heavier (x1), else two heavier (x0.5), else the heaviest at least 3 lighter (weight ratio). */
	@javax.annotation.Nullable
	private static DecayData computeDecayData(Isotopes s) {
		java.util.List<DecayProduct> li = new java.util.ArrayList<>();
		for (Isotopes i : isoList)
			li.add(new DecayProduct(i, i.getChemicalSymbol(), i.getAtomicWeight()));
		for (int w : new int[]{204, 206, 207, 208})
			li.add(new DecayProduct(null, "Pb", w));
		int aw = s.getAtomicWeight();
		for (DecayProduct d : li)
			if (d.weight() == aw+1)
				return new DecayData(d, 1);
		for (DecayProduct d : li)
			if (d.weight() == aw+2)
				return new DecayData(d, 0.5);
		DecayProduct use = null;
		for (DecayProduct d : li) {
			if (d.isotope() == s || d.weight() > aw-3)
				continue;
			if (use == null || d.weight() > use.weight())
				use = d;
		}
		return use == null ? null : new DecayData(use, aw/(double)use.weight());
	}

	/** Original getMCHalfLife, in ticks. */
	public double getMCHalfLife() {
		return half*base.minecraftDuration;
	}

	public String getHalfLifeAsDisplay() {
		return String.format("%.3f%s%s", ReikaMathLibrary.getThousandBase(half), ReikaEngLibrary.getSIPrefix(half), base.abbreviation);
	}
}
