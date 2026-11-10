/*******************************************************************************
 * Checks that every ported item/machine has its model, texture and English name, and that every recipe references
 * existing items. Private port of RotaryCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import reika.rotarycraft.content.item.CraftingPart;

class ResourceCompletenessTest {

	private static final Path ROOT = Path.of(System.getProperty("port.projectDir", "."));
	private static final Path ASSETS = ROOT.resolve("src/main/resources/assets/rotarycraft");
	private static final Path DATA = ROOT.resolve("src/main/resources/data/rotarycraft");

	private static final String[] MATERIALS = {"wood", "stone", "steel", "tungsten", "diamond", "bedrock"};
	private static final String[] GEAR_PARTS = {"shaft", "gear", "unit2", "unit4", "unit8", "unit16", "bearing", "shaftcore"};
	private static final String[] FLYWHEELS = {"wood", "stone", "iron", "gold", "tungsten", "depletedu", "bedrock"};
	private static final String[] MACHINES = {"shaft", "gearbox", "flywheel", "bevel_gears", "clutch", "dynamometer", "shaft_junction",
			"dc_engine", "wind_engine", "steam_engine", "gas_engine", "ac_engine", "sport_engine", "micro_engine", "hydro_engine", "jet_engine", "ecu", "blast_furnace", "worktable", "friction_heater", "bedrock_breaker", "borer", "extractor", "grinder", "fermenter", "fan", "mob_harvester", "aggregator", "centrifuge", "fractionator", "pump", "reservoir", "pulse_jet_furnace", "blast_glass", "valve", "bypass", "separation", "suction",
			"hose", "pipe", "fuel_line", "bedrock_pipe", "cooling_fin", "fuel_enhancer", "solar_mirror", "solar_tower", "pipe_pump", "van_de_graff", "drying_bed", "winder", "refrigerator", "heat_ray", "compactor", "filling_station"};

	/** Original BlockDeco storage blocks (ported 2026-11-03). */
	private static final String[] DECO_BLOCKS = {"steel_block", "anthracite_block", "lonsdaleite_block", "shield_block", "bedrock_ingot_block", "coke_block"};

	static Set<String> plainItems() {
		Set<String> ids = new HashSet<>();
		for (CraftingPart p : CraftingPart.values())
			ids.add(p.id);
		for (String m : MATERIALS)
			for (String p : GEAR_PARTS)
				ids.add(m+"_"+p);
		for (String f : FLYWHEELS)
			ids.add(f+"_flywheel_core");
		ids.add("yeast");
		ids.add("ethanol");
		ids.add("screwdriver");
		ids.add("meter");
		ids.add("upgrade_performance");
		ids.add("upgrade_afterburner");
		ids.add("upgrade_redstone");
		for (String c : new String[] {"canola_seeds", "dense_canola_seeds", "canola_husks", "lubricant_bucket", "jet_fuel_bucket", "ethanol_bucket", "liquid_nitrogen_bucket", "spring", "strongcoil", "gear_upgrade"})
			ids.add(c);
		return ids;
	}

	@Test
	void plainItemsHaveTextureModelAndName() throws IOException {
		String lang = Files.readString(ASSETS.resolve("lang/en_us.json"));
		List<String> missing = new ArrayList<>();
		for (String id : plainItems()) {
			if (!Files.exists(ASSETS.resolve("textures/item/"+id+".png")))
				missing.add("texture "+id);
			if (!Files.exists(ASSETS.resolve("models/item/"+id+".json")))
				missing.add("model "+id);
			if (!lang.contains("\"item.rotarycraft."+id+"\""))
				missing.add("name "+id);
		}
		assertEquals(List.of(), missing);
	}

	@Test
	void machinesHaveBlockstateModelsAndName() throws IOException {
		String lang = Files.readString(ASSETS.resolve("lang/en_us.json"));
		List<String> missing = new ArrayList<>();
		List<String> blocks = new ArrayList<>(List.of(MACHINES));
		blocks.addAll(List.of(DECO_BLOCKS));
		for (String id : blocks) {
			for (String f : new String[] {"blockstates/"+id+".json", "models/block/"+id+".json", "models/item/"+id+".json"})
				if (!Files.exists(ASSETS.resolve(f)))
					missing.add(f);
			if (!lang.contains("\"block.rotarycraft."+id+"\""))
				missing.add("name "+id);
		}
		assertEquals(List.of(), missing);
	}

	@Test
	void recipesReferenceExistingItems() throws IOException {
		Set<String> known = plainItems();
		for (String m : MACHINES)
			known.add(m);
		known.addAll(List.of(DECO_BLOCKS));
		known.addAll(List.of("lubricant", "ethanol", "jet_fuel")); //fluid ids (centrifuge fluid outputs)
		known.addAll(List.of("gear_upgrade_2", "gear_upgrade_4", "gear_upgrade_8", "gear_upgrade_16")); //models with fill overrides, no own texture
		Pattern p = Pattern.compile("\"(?:item|id)\"\\s*:\\s*\"rotarycraft:([a-z0-9_]+)\"");
		List<String> bad = new ArrayList<>();
		int count = 0;
		try (Stream<Path> files = Files.walk(DATA.resolve("recipe")).filter(Files::isRegularFile)) {
			for (Path f : (Iterable<Path>)files::iterator) {
				count++;
				Matcher m = p.matcher(Files.readString(f));
				while (m.find())
					if (!known.contains(m.group(1)))
						bad.add(f.getFileName()+": "+m.group(1));
			}
		}
		assertTrue(count > 100, "only "+count+" recipes");
		assertEquals(List.of(), bad);
	}

	@Test
	void machineTexturesUsedByRenderersExist() {
		String[] used = {"transmission/shaft/shafttexs.png", "transmission/shaft/vshafttex.png", "transmission/shaft/crosstex.png",
				"transmission/gear/geartexw.png", "transmission/gear/geartexb.png", "transmission/beveltex.png", "transmission/shaft/shafttex.png",
				"monitortex.png", "transmission/splittertex.png", "transmission/bedsplittertex.png", "transmission/flywheel/depletedu.png",
				"engine/dc.png", "engine/windtex.png", "engine/steamtex.png", "engine/combtex.png", "engine/actex.png", "engine/perftex.png", "engine/microtex.png"};
		List<String> missing = new ArrayList<>();
		for (String u : used)
			if (!Files.exists(ASSETS.resolve("textures/machine/"+u)))
				missing.add(u);
		assertEquals(List.of(), missing);
	}
}
