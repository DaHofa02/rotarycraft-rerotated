/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft;

import java.util.EnumMap;
import java.util.Map;

import net.neoforged.neoforge.common.ModConfigSpec;

import reika.reactorcraft.registry.ReactorOres;

/** Original: ReactorOptions (the options ported so far). */
public final class ReactorConfig {

	public static final ModConfigSpec SPEC;
	public static final ModConfigSpec.IntValue OREDENSITY;
	public static final ModConfigSpec.IntValue DISCRETE;
	public static final ModConfigSpec.IntValue TOROIDCHARGE;
	public static final ModConfigSpec.BooleanValue CHUNKLOADING, VERTNEUTRONS, FASTNEUTRONS, VISIBLENEUTRONS;
	private static final Map<ReactorOres, ModConfigSpec.BooleanValue> ALWAYS_GEN = new EnumMap<>(ReactorOres.class);

	static {
		ModConfigSpec.Builder b = new ModConfigSpec.Builder();
		b.push("options");
		OREDENSITY = b.comment("Ore Density Percentage (clamped to 50-200)").defineInRange("oreDensity", 100, 0, Integer.MAX_VALUE);
		DISCRETE = b.comment("Ore Discretization: how much to clump the ore into larger but fewer veins").defineInRange("discrete", 1, 1, Integer.MAX_VALUE);
		TOROIDCHARGE = b.comment("Toroid Spark Delay: how quickly toroid magnets conduct charge (clamped to 4-20)").defineInRange("toroidCharge", 4, 0, Integer.MAX_VALUE);
		CHUNKLOADING = b.comment("Fission Cores Chunkload When Active").define("chunkloading", true);
		VERTNEUTRONS = b.comment("Fission Neutrons Can Move Vertically").define("vertNeutrons", false);
		FASTNEUTRONS = b.comment("Enable Fast and Thermal Neutrons").define("fastNeutrons", false);
		VISIBLENEUTRONS = b.comment("Visible Neutrons").define("visibleNeutrons", true);
		ALWAYS_GEN.put(ReactorOres.SILVER, b.comment("Generate Silver Ore Even If Other Mods Do").define("silverOre", true));
		ALWAYS_GEN.put(ReactorOres.MAGNETITE, b.comment("Generate Magnetite Ore Even If Other Mods Do").define("magnetiteOre", true));
		ALWAYS_GEN.put(ReactorOres.CALCITE, b.comment("Generate Calcite Ore Even If Other Mods Do").define("calciteOre", true));
		ALWAYS_GEN.put(ReactorOres.CADMIUM, b.comment("Generate Cadmium Ore Even If Other Mods Do").define("cadmiumOre", true));
		ALWAYS_GEN.put(ReactorOres.INDIUM, b.comment("Generate Indium Ore Even If Other Mods Do").define("indiumOre", true));
		b.pop();
		SPEC = b.build();
	}

	private ReactorConfig() {}

	/** Original getToroidChargeRate: the spark delay clamped to 4..20 ticks. */
	public static int getToroidChargeRate() {
		return Math.max(Math.min(TOROIDCHARGE.get(), 20), 4);
	}

	/** Original getOreMultiplier: density/100, clamped to 0.5..2. */
	public static float getOreMultiplier() {
		return Math.max(0.5F, Math.min(2F, OREDENSITY.get()/100F));
	}

	/** Original shouldGen flag: ores without such an option always generate. */
	public static boolean alwaysGenerate(ReactorOres ore) {
		ModConfigSpec.BooleanValue v = ALWAYS_GEN.get(ore);
		return v == null || v.get();
	}
}
