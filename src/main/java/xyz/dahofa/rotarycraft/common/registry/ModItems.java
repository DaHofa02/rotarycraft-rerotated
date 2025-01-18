package xyz.dahofa.rotarycraft.common.registry;

import xyz.dahofa.rotarycraft.api.lib.Names;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import xyz.dahofa.rotarycraft.common.item.gear.bedrock.*;
import xyz.dahofa.rotarycraft.common.item.gear.hslasteel.*;

import java.util.function.Supplier;

public class ModItems {
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Names.MOD_ID);
	public static final DeferredRegister.Items TOOLS = DeferredRegister.createItems(Names.MOD_ID);
	public static final DeferredRegister.Items ORES = DeferredRegister.createItems(Names.MOD_ID);

	/* -------------- ITEMS -------------- */
	public static final DeferredItem<Item> AFTERBURNER_UPGRADE = registerItems("afterburner_upgrade");
	public static final DeferredItem<Item> ALLUMINIUM_ALLOY_CYLINDER = registerItems("alluminium_alloy_cylinder");
	public static final DeferredItem<Item> ALUMINUM_ALLOY_INGOT = registerItems("aluminum_alloy_ingot");
	public static final DeferredItem<Item> ALUMINUM_INGOT = registerItems("aluminum_ingot");
	public static final DeferredItem<Item> ANGULAR_TRANSDUCER = registerItems("angular_transducer");
	public static final DeferredItem<Item> ANTHRACITE = registerItems("anthracite");
	public static final DeferredItem<Item> BALL_BEARING = registerItems("ball_bearing");
	public static final DeferredItem<Item> BASE_PANEL = registerItems("base_panel");
	public static final DeferredItem<Item> BASE_PANEL_CAST = registerItems("base_panel_cast");
	public static final DeferredItem<Item> BEDROCK_16X_GEAR_UNIT = registerItems("bedrock_16x_gear_unit");
	public static final DeferredItem<Item> BEDROCK_2X_GEAR_UNIT = registerItems("bedrock_2x_gear_unit");
	public static final DeferredItem<Item> BEDROCK_4X_GEAR_UNIT = registerItems("bedrock_4x_gear_unit");
	public static final DeferredItem<Item> BEDROCK_8X_GEAR_UNIT = registerItems("bedrock_8x_gear_unit");
	public static final DeferredItem<Item> BEDROCK_ALLOY_INGOT = registerItems("bedrock_alloy_ingot");
	public static final DeferredItem<Item> BEDROCK_DRILL = registerItems("bedrock_drill");
	public static final DeferredItem<Item> BEDROCK_DRILL_HEAD = registerItems("bedrock_drill_head");
	public static final DeferredItem<Item> BEDROCK_GEAR = registerItems("bedrock_gear");
	public static final DeferredItem<Item> BEDROCK_ROD = registerItems("bedrock_rod");
	public static final DeferredItem<Item> BEDROCK_SHAFT_BEARING = registerItems("bedrock_shaft_bearing");
	public static final DeferredItem<Item> BEDROCK_SHAFT_CORE = registerItems("bedrock_shaft_core");
	public static final DeferredItem<Item> BEDROCK_TENSION_COIL = registerItems("bedrock_tension_coil");
	public static final DeferredItem<Item> BELT = registerItems("belt");
	public static final DeferredItem<Item> BRAKE_DISC = registerItems("brake_disc");
	public static final DeferredItem<Item> CANOLA_SEEDS = registerItems("canola_seeds");
	public static final DeferredItem<Item> CHAIN_LINK = registerItems("chain_link");
	public static final DeferredItem<Item> CIRCUIT_BOARD = registerItems("circuit_board");
	public static final DeferredItem<Item> COKE_COAL = registerItems("coke_coal");
	public static final DeferredItem<Item> COMBUSTER = registerItems("combuster");
	public static final DeferredItem<Item> COMPOST = registerItems("compost");
	public static final DeferredItem<Item> COMPRESSOR = registerItems("compressor");
	public static final DeferredItem<Item> COMPUND_COMPRESSOR = registerItems("compund_compressor");
	public static final DeferredItem<Item> COMPUND_TURBINE = registerItems("compund_turbine");
	public static final DeferredItem<Item> CONDENSER = registerItems("condenser");
	public static final DeferredItem<Item> CRAFTING_PATTERN = registerItems("crafting_pattern");
	public static final DeferredItem<Item> CYLINDER = registerItems("cylinder");
	public static final DeferredItem<Item> DIAMOND_16X_GEAR_UNIT = registerItems("diamond_16x_gear_unit");
	public static final DeferredItem<Item> DIAMOND_2X_GEAR_UNIT = registerItems("diamond_2x_gear_unit");
	public static final DeferredItem<Item> DIAMOND_4X_GEAR_UNIT = registerItems("diamond_4x_gear_unit");
	public static final DeferredItem<Item> DIAMOND_8X_GEAR_UNIT = registerItems("diamond_8x_gear_unit");
	public static final DeferredItem<Item> DIAMOND_GEAR = registerItems("diamond_gear");
	public static final DeferredItem<Item> DIAMOND_ROD = registerItems("diamond_rod");
	public static final DeferredItem<Item> DIAMOND_SHAFT_BEARING = registerItems("diamond_shaft_bearing");
	public static final DeferredItem<Item> DIAMOND_SHAFT_CORE = registerItems("diamond_shaft_core");
	public static final DeferredItem<Item> DIFFUSER = registerItems("diffuser");
	public static final DeferredItem<Item> DRILL = registerItems("drill");
	public static final DeferredItem<Item> DRILL_CAST = registerItems("drill_cast");
	public static final DeferredItem<Item> DRY_ICE = registerItems("dry_ice");
	public static final DeferredItem<Item> ETHANOL_BUCKET = registerItems("ethanol_bucket");
	public static final DeferredItem<Item> ETHANOL_CRYSTAL = registerItems("ethanol_crystal");
	public static final DeferredItem<Item> ETHANOL_EXTRACT = registerItems("ethanol_extract");
	public static final DeferredItem<Item> ETHANOL_MINECART = registerItems("ethanol_minecart");
	public static final DeferredItem<Item> EXPLOSIVE_SHELL = registerItems("explosive_shell");
	public static final DeferredItem<Item> FLOUR = registerItems("flour");
	public static final DeferredItem<Item> FLUX_AMPLITUDE_UPGRADE = registerItems("flux_amplitude_upgrade");
	public static final DeferredItem<Item> FLUX_CONDUCTANCE_UPGRADE = registerItems("flux_conductance_upgrade");
	public static final DeferredItem<Item> GEAR_CAST = registerItems("gear_cast");
	public static final DeferredItem<Item> GENERATOR = registerItems("generator");
	public static final DeferredItem<Item> GOLD_COIL = registerItems("gold_coil");
	public static final DeferredItem<Item> PRISMARINE = registerItems("prismarine");
	public static final DeferredItem<Item> HEAT_RAY_BARREL = registerItems("heat_ray_barrel");
	public static final DeferredItem<Item> HEAT_RAY_CORE = registerItems("heat_ray_core");
	public static final DeferredItem<Item> HIGH_STRENGTH_SPRING = registerItems("high_strength_spring");
	public static final DeferredItem<Item> HIGH_TEMPERATURE_COMBUSTER = registerItems("high_temperature_combuster");
	public static final DeferredItem<Item> HSLA_STEEL_16X_GEAR_UNIT = registerItems("hsla_steel_16x_gear_unit");
	public static final DeferredItem<Item> HSLA_STEEL_2X_GEAR_UNIT = registerItems("hsla_steel_2x_gear_unit");
	public static final DeferredItem<Item> HSLA_STEEL_4X_GEAR_UNIT = registerItems("hsla_steel_4x_gear_unit");
	public static final DeferredItem<Item> HSLA_STEEL_8X_GEAR_UNIT = registerItems("hsla_steel_8x_gear_unit");
	public static final DeferredItem<Item> HSLA_STEEL_GEAR = registerItems("hsla_steel_gear");
	public static final DeferredItem<Item> HSLA_STEEL_INGOT = registerItems("hsla_steel_ingot");
	public static final DeferredItem<Item> HSLA_STEEL_ROD = registerItems("hsla_steel_rod");
	public static final DeferredItem<Item> HSLA_STEEL_SCRAP = registerItems("hsla_steel_scrap");
	public static final DeferredItem<Item> HSLA_STEEL_SHAFT_BEARING = registerItems("hsla_steel_shaft_bearing");
	public static final DeferredItem<Item> HSLA_STEEL_SHAFT_CORE = registerItems("hsla_steel_shaft_core");
	public static final DeferredItem<Item> HUB = registerItems("hub");
	public static final DeferredItem<Item> IGNITION_UNIT = registerItems("ignition_unit");
	public static final DeferredItem<Item> IMPELLER = registerItems("impeller");
	public static final DeferredItem<Item> INDUCTIVE_INGOT = registerItems("inductive_ingot");
	public static final DeferredItem<Item> INDUCTIVE_METAL_BLEND = registerItems("inductive_metal_blend");
	public static final DeferredItem<Item> INTEGRATED_GEARBOX_UPGRADE = registerItems("integrated_gearbox_upgrade");
	public static final DeferredItem<Item> INTEGRATED_GEARBOX_UPGRADE_FLUID = registerItems(
			"integrated_gearbox_upgrade_fluid");
	public static final DeferredItem<Item> INTEGRATED_GEARBOX_UPGRADE_SPEED = registerItems(
			"integrated_gearbox_upgrade_speed");
	public static final DeferredItem<Item> INTEGRATED_GEARBOX_UPGRADE_TORQUE = registerItems(
			"integrated_gearbox_upgrade_torque");
	public static final DeferredItem<Item> INTEGRATED_REDSTONE_UPGRADE = registerItems("integrated_redstone_upgrade");
	public static final DeferredItem<Item> IRON_SCRAP = registerItems("iron_scrap");
	public static final DeferredItem<Item> JET_FUEL_BUCKET = registerItems("jet_fuel_bucket");
	public static final DeferredItem<Item> LENS = registerItems("lens");
	public static final DeferredItem<Item> LINEAR_INDUCTION_MOTOR = registerItems("linear_induction_motor");
	public static final DeferredItem<Item> LIQUID_NITROGEN_BUCKET = registerItems("liquid_nitrogen_bucket");
	public static final DeferredItem<Item> LIVINGROCK_16X_GEAR_UNIT = registerItems("livingrock_16x_gear_unit");
	public static final DeferredItem<Item> LIVINGROCK_2X_GEAR_UNIT = registerItems("livingrock_2x_gear_unit");
	public static final DeferredItem<Item> LIVINGROCK_4X_GEAR_UNIT = registerItems("livingrock_4x_gear_unit");
	public static final DeferredItem<Item> LIVINGROCK_8X_GEAR_UNIT = registerItems("livingrock_8x_gear_unit");
	public static final DeferredItem<Item> LIVINGROCK_GEAR = registerItems("livingrock_gear");
	public static final DeferredItem<Item> LIVINGROCK_ROD = registerItems("livingrock_rod");
	public static final DeferredItem<Item> LIVINGWOOD_16X_GEAR_UNIT = registerItems("livingwood_16x_gear_unit");
	public static final DeferredItem<Item> LIVINGWOOD_2X_GEAR_UNIT = registerItems("livingwood_2x_gear_unit");
	public static final DeferredItem<Item> LIVINGWOOD_4X_GEAR_UNIT = registerItems("livingwood_4x_gear_unit");
	public static final DeferredItem<Item> LIVINGWOOD_8X_GEAR_UNIT = registerItems("livingwood_8x_gear_unit");
	public static final DeferredItem<Item> LIVINGWOOD_GEAR = registerItems("livingwood_gear");
	public static final DeferredItem<Item> LIVINGWOOD_STICK = registerItems("livingwood_stick");
	public static final DeferredItem<Item> LONSDALEITE = registerItems("lonsdaleite");
	public static final DeferredItem<Item> LUBRICANT_BUCKET = registerItems("lubricant_bucket");
	public static final DeferredItem<Item> MAGNETIC_COIL_UPGRADE = registerItems("magnetic_coil_upgrade");
	public static final DeferredItem<Item> MATCH_FILTER = registerItems("match_filter");
	public static final DeferredItem<Item> MIRROR = registerItems("mirror");
	public static final DeferredItem<Item> MIXER = registerItems("mixer");
	public static final DeferredItem<Item> MOLTEN_HSLA_STEEL_BUCKET = registerItems("molten_hsla_steel_bucket");
	public static final DeferredItem<Item> MOTION_TRACKER = registerItems("motion_tracker");
	public static final DeferredItem<Item> MOUNT = registerItems("mount");
	public static final DeferredItem<Item> MULCH = registerItems("mulch");
	public static final DeferredItem<Item> MUSIC_BOX_RECORD = registerItems("music_box_record");
	public static final DeferredItem<Item> PADDLE_PANEL = registerItems("paddle_panel");
	public static final DeferredItem<Item> PERFORMANCE_ENGINE_UPGRADE = registerItems("performance_engine_upgrade");
	public static final DeferredItem<Item> PERMANENT_MAGNET_UPGRADE = registerItems("permanent_magnet_upgrade");
	public static final DeferredItem<Item> PORTABLE_BACKUP_FUEL_TANK = registerItems("portable_backup_fuel_tank");
	public static final DeferredItem<Item> POWER_MODULE = registerItems("power_module");
	public static final DeferredItem<Item> PRESSURE_HEAD = registerItems("pressure_head");
	public static final DeferredItem<Item> PROJECTOR_SLIDE = registerItems("projector_slide");
	public static final DeferredItem<Item> PROPELLER_BLADE = registerItems("propeller_blade");
	public static final DeferredItem<Item> PROPELLER_BLADE_CAST = registerItems("propeller_blade_cast");
	public static final DeferredItem<Item> RADAR_UNIT = registerItems("radar_unit");
	public static final DeferredItem<Item> RADIATOR = registerItems("radiator");
	public static final DeferredItem<Item> RAILGUN_ACCELERATOR = registerItems("railgun_accelerator");
	public static final DeferredItem<Item> RAILGUN_AMMUNITION_EXTREME = registerItems("railgun_ammunition_extreme");
	public static final DeferredItem<Item> RAILGUN_AMMUNITION_HIGH = registerItems("railgun_ammunition_high");
	public static final DeferredItem<Item> RAILGUN_AMMUNITION_LOW = registerItems("railgun_ammunition_low");
	public static final DeferredItem<Item> RAILGUN_AMMUNITION_MID = registerItems("railgun_ammunition_mid");
	public static final DeferredItem<Item> RAILGUN_AMMUNITION_VOIDMETAL = registerItems("railgun_ammunition_voidmetal");
	public static final DeferredItem<Item> REDSTONE_COOLING_UPGRADE = registerItems("redstone_cooling_upgrade");
	public static final DeferredItem<Item> RESONANCE_STABILITY_UPGRADE = registerItems("resonance_stability_upgrade");
	public static final DeferredItem<Item> SALT = registerItems("salt");
	public static final DeferredItem<Item> SAWDUST = registerItems("sawdust");
	public static final DeferredItem<Item> SCREEN = registerItems("screen");
	public static final DeferredItem<Item> SHAFT_UNIT_CAST = registerItems("shaft_unit_cast");
	public static final DeferredItem<Item> SILICON = registerItems("silicon");
	public static final DeferredItem<Item> SINTERED_TUNGSTEN_INGOT = registerItems("sintered_tungsten_ingot");
	public static final DeferredItem<Item> SLIPPERY_COMB = registerItems("slippery_comb");
	public static final DeferredItem<Item> SLIPPERY_PROPOLIS = registerItems("slippery_propolis");
	public static final DeferredItem<Item> SLUDGE = registerItems("sludge");
	public static final DeferredItem<Item> SONAR_UNIT = registerItems("sonar_unit");
	public static final DeferredItem<Item> SPRING_STEEL_INGOT = registerItems("spring_steel_ingot");
	public static final DeferredItem<Item> STONE_16X_GEAR_UNIT = registerItems("stone_16x_gear_unit");
	public static final DeferredItem<Item> STONE_2X_GEAR_UNIT = registerItems("stone_2x_gear_unit");
	public static final DeferredItem<Item> STONE_4X_GEAR_UNIT = registerItems("stone_4x_gear_unit");
	public static final DeferredItem<Item> STONE_8X_GEAR_UNIT = registerItems("stone_8x_gear_unit");
	public static final DeferredItem<Item> STONE_GEAR = registerItems("stone_gear");
	public static final DeferredItem<Item> STONE_ROD = registerItems("stone_rod");
	public static final DeferredItem<Item> STONE_SHAFT_BEARING = registerItems("stone_shaft_bearing");
	public static final DeferredItem<Item> STONE_SHAFT_CORE = registerItems("stone_shaft_core");
	public static final DeferredItem<Item> TAR_SAND = registerItems("tar_sand");
	public static final DeferredItem<Item> TENSION_COIL = registerItems("tension_coil");
	public static final DeferredItem<Item> THERMAL_STABILITY_UPGRADE = registerItems("thermal_stability_upgrade");
	public static final DeferredItem<Item> TILE_SELECTOR = registerItems("tile_selector");
	public static final DeferredItem<Item> TNT_CANNON_TARGETING_AID = registerItems("tnt_cannon_targeting_aid");
	public static final DeferredItem<Item> TORSION_RESISTANCE_UPGRADE = registerItems("torsion_resistance_upgrade");
	public static final DeferredItem<Item> TUNGSTEN_ALLOY_16X_GEAR_UNIT = registerItems("tungsten_alloy_16x_gear_unit");
	public static final DeferredItem<Item> TUNGSTEN_ALLOY_2X_GEAR_UNIT = registerItems("tungsten_alloy_2x_gear_unit");
	public static final DeferredItem<Item> TUNGSTEN_ALLOY_4X_GEAR_UNIT = registerItems("tungsten_alloy_4x_gear_unit");
	public static final DeferredItem<Item> TUNGSTEN_ALLOY_8X_GEAR_UNIT = registerItems("tungsten_alloy_8x_gear_unit");
	public static final DeferredItem<Item> TUNGSTEN_ALLOY_GEAR = registerItems("tungsten_alloy_gear");
	public static final DeferredItem<Item> TUNGSTEN_ALLOY_INGOT = registerItems("tungsten_alloy_ingot");
	public static final DeferredItem<Item> TUNGSTEN_ALLOY_ROD = registerItems("tungsten_alloy_rod");
	public static final DeferredItem<Item> TUNGSTEN_ALLOY_SHAFT_BEARING = registerItems("tungsten_alloy_shaft_bearing");
	public static final DeferredItem<Item> TUNGSTEN_ALLOY_SHAFT_CORE = registerItems("tungsten_alloy_shaft_core");
	public static final DeferredItem<Item> TUNGSTEN_FLAKES = registerItems("tungsten_flakes");
	public static final DeferredItem<Item> TURBINE = registerItems("turbine");
	public static final DeferredItem<Item> TURRET_AIMING_UNIT = registerItems("turret_aiming_unit");
	public static final DeferredItem<Item> TURRET_BASE = registerItems("turret_base");
	public static final DeferredItem<Item> ULTRASOUND = registerItems("ultrasound");
	public static final DeferredItem<Item> VACUUM = registerItems("vacuum");
	public static final DeferredItem<Item> WIND_SPRING = registerItems("wind_spring");
	public static final DeferredItem<Item> WOODEN_16X_GEAR_UNIT = registerItems("wooden_16x_gear_unit");
	public static final DeferredItem<Item> WOODEN_2X_GEAR_UNIT = registerItems("wooden_2x_gear_unit");
	public static final DeferredItem<Item> WOODEN_4X_GEAR_UNIT = registerItems("wooden_4x_gear_unit");
	public static final DeferredItem<Item> WOODEN_8X_GEAR_UNIT = registerItems("wooden_8x_gear_unit");
	public static final DeferredItem<Item> WOODEN_GEAR = registerItems("wooden_gear");
	public static final DeferredItem<Item> WOODEN_SHAFT_BEARING = registerItems("wooden_shaft_bearing");
	public static final DeferredItem<Item> WOODEN_SHAFT_CORE = registerItems("wooden_shaft_core");
	public static final DeferredItem<Item> WORLDEDIT_TOOL = registerItems("worldedit_tool");
	public static final DeferredItem<Item> WORM_GEAR = registerItems("worm_gear");
	public static final DeferredItem<Item> YEAST = registerItems("yeast");

	/* ------------- ORES ------------- */
	public static final DeferredItem<Item> NETHERRACK_DUST = registerOres("netherrack_dust");
	public static final DeferredItem<Item> NETHER_QUARTZ_FLAKES = registerOres("nether_quartz_flakes");
	public static final DeferredItem<Item> NETHER_QUARTZ_SLURRY = registerOres("nether_quartz_slurry");
	public static final DeferredItem<Item> NETHER_QUARTZ_SOLUTION = registerOres("nether_quartz_solution");
	public static final DeferredItem<Item> ALUMINUM_POWDER = registerOres("aluminum_powder");
	public static final DeferredItem<Item> AMMONIUM_NITRATE = registerOres("ammonium_nitrate");
	public static final DeferredItem<Item> BEDROCK_DUST = registerOres("bedrock_dust");
	public static final DeferredItem<Item> COAL_ORE_SOLUTION = registerOres("coal_ore_solution");
	public static final DeferredItem<Item> COAL_DUST = registerOres("coal_dust");
	public static final DeferredItem<Item> COAL_ORE_FLAKES = registerOres("coal_ore_flakes");
	public static final DeferredItem<Item> COAL_ORE_SLURRY = registerOres("coal_ore_slurry");
	public static final DeferredItem<Item> DIAMOND_ORE_FLAKES = registerOres("diamond_ore_flakes");
	public static final DeferredItem<Item> DIAMOND_ORE_SLURRY = registerOres("diamond_ore_slurry");
	public static final DeferredItem<Item> DIAMOND_ORE_SOLUTION = registerOres("diamond_ore_solution");
	public static final DeferredItem<Item> EMERALD_ORE_FLAKES = registerOres("emerald_ore_flakes");
	public static final DeferredItem<Item> EMERALD_ORE_SLURRY = registerOres("emerald_ore_slurry");
	public static final DeferredItem<Item> EMERALD_ORE_SOLUTION = registerOres("emerald_ore_solution");
	public static final DeferredItem<Item> GOLD_ORE_FLAKES = registerOres("gold_ore_flakes");
	public static final DeferredItem<Item> GOLD_ORE_SLURRY = registerOres("gold_ore_slurry");
	public static final DeferredItem<Item> GOLD_ORE_SOLUTION = registerOres("gold_ore_solution");
	public static final DeferredItem<Item> IRON_ORE_FLAKES = registerOres("iron_ore_flakes");
	public static final DeferredItem<Item> IRON_ORE_SLURRY = registerOres("iron_ore_slurry");
	public static final DeferredItem<Item> IRON_ORE_SOLUTION = registerOres("iron_ore_solution");
	public static final DeferredItem<Item> LAPIS_ORE_FLAKES = registerOres("lapis_ore_flakes");
	public static final DeferredItem<Item> LAPIS_ORE_SLURRY = registerOres("lapis_ore_slurry");
	public static final DeferredItem<Item> LAPIS_ORE_SOLUTION = registerOres("lapis_ore_solution");
	public static final DeferredItem<Item> POWDERED_DIAMOND_ORE = registerOres("powdered_diamond_ore");
	public static final DeferredItem<Item> POWDERED_EMERALD_ORE = registerOres("powdered_emerald_ore");
	public static final DeferredItem<Item> POWDERED_GOLD_ORE = registerOres("powdered_gold_ore");
	public static final DeferredItem<Item> POWDERED_IRON_ORE = registerOres("powdered_iron_ore");
	public static final DeferredItem<Item> POWDERED_LAPIS_ORE = registerOres("powdered_lapis_ore");
	public static final DeferredItem<Item> POWDERED_NETHER_QUARTZ = registerOres("powdered_nether_quartz");
	public static final DeferredItem<Item> POWDERED_REDSTONE_ORE = registerOres("powdered_redstone_ore");
	public static final DeferredItem<Item> POWDERED_COAL_ORE = registerOres("powdered_coal_ore");
	public static final DeferredItem<Item> SILICON_POWDER = registerOres("silicon_powder");
	public static final DeferredItem<Item> SILVER_FLAKES = registerOres("silver_flakes");
	public static final DeferredItem<Item> SILVER_INGOT = registerOres("silver_ingot");
	public static final DeferredItem<Item> SILVER_IODIDE = registerOres("silver_iodide");
	public static final DeferredItem<Item> REDSTONE_ORE_FLAKES = registerOres("redstone_ore_flakes");
	public static final DeferredItem<Item> REDSTONE_ORE_SLURRY = registerOres("redstone_ore_slurry");
	public static final DeferredItem<Item> REDSTONE_ORE_SOLUTION = registerOres("redstone_ore_solution");

	/* ------------- TOOLS ------------- */
	public static final DeferredItem<Item> SPRING_BOOTS = registerTools("spring_boots");
	public static final DeferredItem<Item> SPRING_POWERED_GRAFTER = registerTools("spring_powered_grafter");
	public static final DeferredItem<Item> SPRING_POWERED_PUMP = registerTools("spring_powered_pump");
	public static final DeferredItem<Item> SAW = registerTools("saw");
	public static final DeferredItem<Item> ROTARYCRAFT_HANDBOOK = registerTools("rotarycraft_handbook");
	public static final DeferredItem<Item> RANGE_FINDER = registerTools("range_finder");
	public static final DeferredItem<Item> NIGHTVISION_GOOGLES = registerTools("nightvision_googles");
	public static final DeferredItem<Item> MAGIC_WAND = registerTools("magic_wand");
	public static final DeferredItem<Item> KNOCKBACK_GUN = registerTools("knockback_gun");
	public static final DeferredItem<Item> IO_GOOGLES = registerTools("io_googles");
	public static final DeferredItem<Item> GRAVEL_GUN = registerTools("gravel_gun");
	public static final DeferredItem<Item> HANDHELD_CRAFTING_TOOL = registerTools("handheld_crafting_tool");
	public static final DeferredItem<Item> HANDHELD_PISTON = registerTools("handheld_piston");
	public static final DeferredItem<Item> ETHANOL_JETPACK = registerTools("ethanol_jetpack");
	public static final DeferredItem<Item> BEDROCK_GRAFTER = registerTools("bedrock_grafter");
	public static final DeferredItem<Item> BEDROCK_HANDSAW = registerTools("bedrock_handsaw");
	public static final DeferredItem<Item> BEDROCK_HELMET_OF_REVEALING = registerTools("bedrock_helmet_of_revealing");
	public static final DeferredItem<Item> BEDROCK_JETPACK = registerTools("bedrock_jetpack");

	public static final DeferredItem<Item> HSLA_STEEL_SCYTHE = registerTools("hsla_steel_scythe");
	public static final DeferredItem<Item> HSLA_STEEL_JETPACK = registerTools("hsla_steel_jetpack");
	public static final DeferredItem<Item> BEDROCK_NIGHTVISION = registerTools("bedrock_nightvision");
	public static final DeferredItem<Item> BEDROCK_CHISEL = registerTools("bedrock_chisel");
	public static final DeferredItem<Item> BEDROCK_SCYTHE = registerTools("bedrock_scythe");
	public static final DeferredItem<Item> BEDROCK_SPRING_BOOTS = registerTools("bedrock_spring_boots");
	public static final DeferredItem<Item> CANNON_KEY = registerTools("cannon_key");
	public static final DeferredItem<Item> FIREBALL_LAUNCHER = registerTools("fireball_launcher");

	public static final DeferredItem<Item> SCREWDRIVER = registerTools("screwdriver");

	public static final DeferredItem<Item> HSLA_STEEL_HELMET = registerTools("hsla_steel_helmet",
			() -> new HSLASteelArmorItem(ArmorItem.Type.HELMET));
	public static final DeferredItem<Item> HSLA_STEEL_CHESTPLATE = registerTools("hsla_steel_chestplate",
			() -> new HSLASteelArmorItem(ArmorItem.Type.CHESTPLATE));
	public static final DeferredItem<Item> HSLA_STEEL_LEGGINGS = registerTools("hsla_steel_leggings",
			() -> new HSLASteelArmorItem(ArmorItem.Type.LEGGINGS));
	public static final DeferredItem<Item> HSLA_STEEL_BOOTS = registerTools("hsla_steel_boots",
			() -> new HSLASteelArmorItem(ArmorItem.Type.BOOTS));

	public static final DeferredItem<Item> HSLA_STEEL_SWORD = registerTools("hsla_steel_sword",
			() -> new HSLASteelSwordItem());
	public static final DeferredItem<Item> HSLA_STEEL_PICKAXE = registerTools("hsla_steel_pickaxe",
			() -> new HSLASteelPickaxeItem());
	public static final DeferredItem<Item> HSLA_STEEL_AXE = registerTools("hsla_steel_axe",
			() -> new HSLASteelAxeItem());
	public static final DeferredItem<Item> HSLA_STEEL_SHOVEL = registerTools("hsla_steel_shovel",
			() -> new HSLASteelShovelItem());
	public static final DeferredItem<Item> HSLA_STEEL_HOE = registerTools("hsla_steel_hoe",
			() -> new HSLASteelHoeItem());
	public static final DeferredItem<Item> HSLA_STEEL_SHEARS = registerTools("hsla_steel_shears",
			() -> new HSLASteelShearsItem());

	public static final DeferredItem<Item> BEDROCK_HELMET = registerTools("bedrock_helmet",
			() -> new BedrockArmorItem(ArmorItem.Type.HELMET));
	public static final DeferredItem<Item> BEDROCK_CHESTPLATE = registerTools("bedrock_chestplate",
			() -> new BedrockArmorItem(ArmorItem.Type.CHESTPLATE));
	public static final DeferredItem<Item> BEDROCK_LEGGINGS = registerTools("bedrock_leggings",
			() -> new BedrockArmorItem(ArmorItem.Type.LEGGINGS));
	public static final DeferredItem<Item> BEDROCK_BOOTS = registerTools("bedrock_boots",
			() -> new BedrockArmorItem(ArmorItem.Type.BOOTS));

	public static final DeferredItem<Item> BEDROCK_SWORD = registerTools("bedrock_sword", () -> new BedrockSwordItem());
	public static final DeferredItem<Item> BEDROCK_PICKAXE = registerTools("bedrock_pickaxe",
			() -> new BedrockPickaxeItem());
	public static final DeferredItem<Item> BEDROCK_AXE = registerTools("bedrock_axe", () -> new BedrockAxeItem());
	public static final DeferredItem<Item> BEDROCK_SHOVEL = registerTools("bedrock_shovel",
			() -> new BedrockShovelItem());
	public static final DeferredItem<Item> BEDROCK_HOE = registerTools("bedrock_hoe", () -> new BedrockHoeItem());
	public static final DeferredItem<Item> BEDROCK_SHEARS = registerTools("bedrock_shears",
			() -> new BedrockShearsItem());

	/* --------------------------------- */

	public static Item.Properties defaultProps() {
		return new Item.Properties();
	}

	public static Item.Properties toolProps() {
		return defaultProps().stacksTo(1);
	}

	public static <T extends Item> DeferredItem<T> registerTools(final String name, final Supplier<T> sup) {
		return TOOLS.register(name, sup);
	}

	public static <T extends Item> DeferredItem<T> registerItems(final String name, final Supplier<T> sup) {
		return ITEMS.register(name, sup);
	}

	public static <T extends Item> DeferredItem<T> registerOres(final String name, final Supplier<T> sup) {
		return ORES.register(name, sup);
	}

	private static DeferredItem<Item> registerTools(final String name) {
		return registerTools(name, () -> new Item(ModItems.defaultProps()));
	}

	private static DeferredItem<Item> registerItems(final String name) {
		return registerItems(name, () -> new Item(ModItems.defaultProps()));
	}

	private static DeferredItem<Item> registerOres(final String name) {
		return registerOres(name, () -> new Item(ModItems.defaultProps()));
	}
	/*
	 * private static DeferredItem<BucketItem> registerBucket(String name,
	 * Supplier<? extends Fluid> sup) { return register(name, () -> new
	 * PneumaticCraftBucketItem(sup.get())); }
	 */

	/*
	 * private static DeferredItem<Item> registerFood(final String name,
	 * FoodProperties food) { return register(name, () -> new
	 * Item(defaultProps().food(food))); }
	 */
}
