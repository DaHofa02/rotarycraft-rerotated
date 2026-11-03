/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft;

import java.util.EnumMap;
import java.util.Map;

import net.neoforged.neoforge.common.ModConfigSpec;

import reika.electricraft.registry.ElectriOres;

/** Original: ElectriConfig (per-ore "Ore Control") and ElectriOptions.DISCRETE. */
public final class ElectriConfig {

	public static final ModConfigSpec SPEC;
	public static final ModConfigSpec.IntValue DISCRETE;
	private static final Map<ElectriOres, ModConfigSpec.BooleanValue> ORES = new EnumMap<>(ElectriOres.class);

	static {
		ModConfigSpec.Builder b = new ModConfigSpec.Builder();
		b.push("options");
		DISCRETE = b.comment("Ore Discretization: how much to clump the ore into larger but fewer veins").defineInRange("discrete", 1, 1, Integer.MAX_VALUE);
		b.pop();
		b.comment("Ore Control: always generate this ore (if off, it only generates when no other mod provides it)").push("ore_control");
		for (ElectriOres ore : ElectriOres.oreList)
			if (ore.isPorted())
				ORES.put(ore, b.define(ore.getName(), true));
		b.pop();
		SPEC = b.build();
	}

	private ElectriConfig() {}

	public static boolean isOreGenEnabled(ElectriOres ore) {
		ModConfigSpec.BooleanValue v = ORES.get(ore);
		return v != null && v.get();
	}
}
