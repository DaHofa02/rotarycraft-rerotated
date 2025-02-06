package xyz.dahofa.rotarycraft.common.registry;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Unbreakable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import xyz.dahofa.rotarycraft.api.lib.Names;
import xyz.dahofa.rotarycraft.common.item.gear.bedrock.BedrockArmorItem;
import xyz.dahofa.rotarycraft.common.item.gear.bedrock.BedrockHoeItem;
import xyz.dahofa.rotarycraft.common.item.gear.bedrock.BedrockItemConfig;
import xyz.dahofa.rotarycraft.common.item.gear.hslasteel.HSLASteelArmorItem;
import xyz.dahofa.rotarycraft.common.item.gear.hslasteel.HSLASteelItemConfig;

import java.util.function.Supplier;

public class RCItems {
    public static final DeferredRegister.Items BLOCKS = DeferredRegister.createItems(Names.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Names.MOD_ID);
    public static final DeferredRegister.Items TOOLS = DeferredRegister.createItems(Names.MOD_ID);
    public static final DeferredRegister.Items ORES = DeferredRegister.createItems(Names.MOD_ID);

    public static final DeferredRegister.Items ENCHANTED_TOOLS = DeferredRegister.createItems(Names.MOD_ID);


    /* -------------- ITEMS -------------- */

    public static final DeferredItem<Item> YEAST = registerItems("yeast");
    public static final DeferredItem<Item> ETHANOL_CRYSTALS = registerItems("ethanol_crystals");
    public static final DeferredItem<Item> CANOLA_SEEDS = registerItems("canola_seeds",
            () -> new ItemNameBlockItem(RCBlocks.CANOLA_SEED.get(), new Item.Properties()));
    public static final DeferredItem<Item> BASE_PANEL = registerItems("base_panel");
    public static final DeferredItem<Item> MOUNT = registerItems("mount");
    public static final DeferredItem<Item> HSLA_STEEL_SCRAP = registerItems("hsla_steel_scrap");
    public static final DeferredItem<Item> IRON_SCRAP = registerItems("iron_scrap");
    public static final DeferredItem<Item> WORM_GEAR = registerItems("worm_gear");

    public static final DeferredItem<Item> WOODEN_GEAR = registerItems("wooden_gear");
    public static final DeferredItem<Item> WOODEN_2X_GEAR_UNIT = registerItems("wooden_2x_gear_unit");
    public static final DeferredItem<Item> WOODEN_4X_GEAR_UNIT = registerItems("wooden_4x_gear_unit");
    public static final DeferredItem<Item> WOODEN_8X_GEAR_UNIT = registerItems("wooden_8x_gear_unit");
    public static final DeferredItem<Item> WOODEN_16X_GEAR_UNIT = registerItems("wooden_16x_gear_unit");
    public static final DeferredItem<Item> WOODEN_SHAFT_BEARING = registerItems("wooden_shaft_bearing");
    public static final DeferredItem<Item> WOODEN_SHAFT_CORE = registerItems("wooden_shaft_core");

    public static final DeferredItem<Item> STONE_ROD = registerItems("stone_rod");
    public static final DeferredItem<Item> STONE_GEAR = registerItems("stone_gear");
    public static final DeferredItem<Item> STONE_2X_GEAR_UNIT = registerItems("stone_2x_gear_unit");
    public static final DeferredItem<Item> STONE_4X_GEAR_UNIT = registerItems("stone_4x_gear_unit");
    public static final DeferredItem<Item> STONE_8X_GEAR_UNIT = registerItems("stone_8x_gear_unit");
    public static final DeferredItem<Item> STONE_16X_GEAR_UNIT = registerItems("stone_16x_gear_unit");
    public static final DeferredItem<Item> STONE_SHAFT_BEARING = registerItems("stone_shaft_bearing");
    public static final DeferredItem<Item> STONE_SHAFT_CORE = registerItems("stone_shaft_core");

    public static final DeferredItem<Item> HSLA_STEEL_ROD = registerItems("hsla_steel_rod");
    public static final DeferredItem<Item> HSLA_STEEL_GEAR = registerItems("hsla_steel_gear");
    public static final DeferredItem<Item> HSLA_STEEL_2X_GEAR_UNIT = registerItems("hsla_steel_2x_gear_unit");
    public static final DeferredItem<Item> HSLA_STEEL_4X_GEAR_UNIT = registerItems("hsla_steel_4x_gear_unit");
    public static final DeferredItem<Item> HSLA_STEEL_8X_GEAR_UNIT = registerItems("hsla_steel_8x_gear_unit");
    public static final DeferredItem<Item> HSLA_STEEL_16X_GEAR_UNIT = registerItems("hsla_steel_16x_gear_unit");
    public static final DeferredItem<Item> HSLA_STEEL_SHAFT_BEARING = registerItems("hsla_steel_shaft_bearing");
    public static final DeferredItem<Item> HSLA_STEEL_SHAFT_CORE = registerItems("hsla_steel_shaft_core");

    public static final DeferredItem<Item> DIAMOND_ROD = registerItems("diamond_rod");
    public static final DeferredItem<Item> DIAMOND_GEAR = registerItems("diamond_gear");
    public static final DeferredItem<Item> DIAMOND_2X_GEAR_UNIT = registerItems("diamond_2x_gear_unit");
    public static final DeferredItem<Item> DIAMOND_4X_GEAR_UNIT = registerItems("diamond_4x_gear_unit");
    public static final DeferredItem<Item> DIAMOND_8X_GEAR_UNIT = registerItems("diamond_8x_gear_unit");
    public static final DeferredItem<Item> DIAMOND_16X_GEAR_UNIT = registerItems("diamond_16x_gear_unit");
    public static final DeferredItem<Item> DIAMOND_SHAFT_BEARING = registerItems("diamond_shaft_bearing");
    public static final DeferredItem<Item> DIAMOND_SHAFT_CORE = registerItems("diamond_shaft_core");

    public static final DeferredItem<Item> TUNGSTEN_ALLOY_ROD = registerItems("tungsten_alloy_rod");
    public static final DeferredItem<Item> TUNGSTEN_ALLOY_GEAR = registerItems("tungsten_alloy_gear");
    public static final DeferredItem<Item> TUNGSTEN_ALLOY_2X_GEAR_UNIT = registerItems("tungsten_alloy_2x_gear_unit");
    public static final DeferredItem<Item> TUNGSTEN_ALLOY_4X_GEAR_UNIT = registerItems("tungsten_alloy_4x_gear_unit");
    public static final DeferredItem<Item> TUNGSTEN_ALLOY_8X_GEAR_UNIT = registerItems("tungsten_alloy_8x_gear_unit");
    public static final DeferredItem<Item> TUNGSTEN_ALLOY_16X_GEAR_UNIT = registerItems("tungsten_alloy_16x_gear_unit");
    public static final DeferredItem<Item> TUNGSTEN_ALLOY_SHAFT_BEARING = registerItems("tungsten_alloy_shaft_bearing");
    public static final DeferredItem<Item> TUNGSTEN_ALLOY_SHAFT_CORE = registerItems("tungsten_alloy_shaft_core");

    public static final DeferredItem<Item> BEDROCK_ROD = registerItems("bedrock_rod");
    public static final DeferredItem<Item> BEDROCK_GEAR = registerItems("bedrock_gear");
    public static final DeferredItem<Item> BEDROCK_2X_GEAR_UNIT = registerItems("bedrock_2x_gear_unit");
    public static final DeferredItem<Item> BEDROCK_4X_GEAR_UNIT = registerItems("bedrock_4x_gear_unit");
    public static final DeferredItem<Item> BEDROCK_8X_GEAR_UNIT = registerItems("bedrock_8x_gear_unit");
    public static final DeferredItem<Item> BEDROCK_16X_GEAR_UNIT = registerItems("bedrock_16x_gear_unit");
    public static final DeferredItem<Item> BEDROCK_SHAFT_BEARING = registerItems("bedrock_shaft_bearing");
    public static final DeferredItem<Item> BEDROCK_SHAFT_CORE = registerItems("bedrock_shaft_core");

    public static final DeferredItem<Item> LIVINGROCK_ROD = registerItems("livingrock_rod");
    public static final DeferredItem<Item> LIVINGROCK_GEAR = registerItems("livingrock_gear");
    public static final DeferredItem<Item> LIVINGROCK_2X_GEAR_UNIT = registerItems("livingrock_2x_gear_unit");
    public static final DeferredItem<Item> LIVINGROCK_4X_GEAR_UNIT = registerItems("livingrock_4x_gear_unit");
    public static final DeferredItem<Item> LIVINGROCK_8X_GEAR_UNIT = registerItems("livingrock_8x_gear_unit");
    public static final DeferredItem<Item> LIVINGROCK_16X_GEAR_UNIT = registerItems("livingrock_16x_gear_unit");

    public static final DeferredItem<Item> LIVINGWOOD_STICK = registerItems("livingwood_stick");
    public static final DeferredItem<Item> LIVINGWOOD_GEAR = registerItems("livingwood_gear");
    public static final DeferredItem<Item> LIVINGWOOD_2X_GEAR_UNIT = registerItems("livingwood_2x_gear_unit");
    public static final DeferredItem<Item> LIVINGWOOD_4X_GEAR_UNIT = registerItems("livingwood_4x_gear_unit");
    public static final DeferredItem<Item> LIVINGWOOD_8X_GEAR_UNIT = registerItems("livingwood_8x_gear_unit");
    public static final DeferredItem<Item> LIVINGWOOD_16X_GEAR_UNIT = registerItems("livingwood_16x_gear_unit");

    public static final DeferredItem<Item> WOODEN_FLYWHEEL_CORE = registerItems("wooden_flywheel_core");
    public static final DeferredItem<Item> STONE_FLYWHEEL_CORE = registerItems("stone_flywheel_core");
    public static final DeferredItem<Item> IRON_FLYWHEEL_CORE = registerItems("iron_flywheel_core");
    public static final DeferredItem<Item> GOLD_FLYWHEEL_CORE = registerItems("gold_flywheel_core");
    public static final DeferredItem<Item> TUNGSTEN_ALLOY_FLYWHEEL_CORE = registerItems("tungsten_alloy_flywheel_core");
    public static final DeferredItem<Item> DEPLETED_URANIUM_FLYWHEEL_CORE = registerItems("depleted_uranium_flywheel_core");
    public static final DeferredItem<Item> BEDROCK_FLYWHEEL_CORE = registerItems("bedrock_flywheel_core");

    public static final DeferredItem<Item> IMPELLER = registerItems("impeller");
    public static final DeferredItem<Item> COMPRESSOR = registerItems("compressor");
    public static final DeferredItem<Item> TURBINE = registerItems("turbine");
    public static final DeferredItem<Item> DIFFUSER = registerItems("diffuser");
    public static final DeferredItem<Item> COMBUSTER = registerItems("combuster");
    public static final DeferredItem<Item> CYLINDER = registerItems("cylinder");
    public static final DeferredItem<Item> RADIATOR = registerItems("radiator");
    public static final DeferredItem<Item> CONDENSER = registerItems("condenser");
    public static final DeferredItem<Item> GOLD_COIL = registerItems("gold_coil");
    public static final DeferredItem<Item> PADDLE_PANEL = registerItems("paddle_panel");
    public static final DeferredItem<Item> IGNITION_UNIT = registerItems("ignition_unit");
    public static final DeferredItem<Item> COMPUND_COMPRESSOR = registerItems("compund_compressor");
    public static final DeferredItem<Item> ALUMINUM_ALLOY_CYLINDER = registerItems("aluminum_alloy_cylinder");
    public static final DeferredItem<Item> HIGH_TEMPERATURE_COMBUSTER = registerItems("high_temperature_combuster");
    public static final DeferredItem<Item> HEAT_RAY_BARREL = registerItems("heat_ray_barrel");
    public static final DeferredItem<Item> LENS = registerItems("lens");
    public static final DeferredItem<Item> POWER_MODULE = registerItems("power_module");
    public static final DeferredItem<Item> HEAT_RAY_CORE = registerItems("heat_ray_core");
    public static final DeferredItem<Item> LINEAR_INDUCTION_MOTOR = registerItems("linear_induction_motor");
    public static final DeferredItem<Item> PROPELLER_BLADE = registerItems("propeller_blade");
    public static final DeferredItem<Item> HUB = registerItems("hub");
    public static final DeferredItem<Item> MIRROR = registerItems("mirror");
    public static final DeferredItem<Item> GENERATOR = registerItems("generator");
    public static final DeferredItem<Item> RAILGUN_ACCELERATOR = registerItems("railgun_accelerator");
    public static final DeferredItem<Item> TURRET_BASE = registerItems("turret_base");
    public static final DeferredItem<Item> TURRET_AIMING_UNIT = registerItems("turret_aiming_unit");
    public static final DeferredItem<Item> COMPUND_TURBINE = registerItems("compund_turbine");
    public static final DeferredItem<Item> BEDROCK_TENSION_COIL = registerItems("bedrock_tension_coil");
    public static final DeferredItem<Item> CHAIN_LINK = registerItems("chain_link");
    public static final DeferredItem<Item> BEDROCK_DRILL = registerItems("bedrock_drill");
    public static final DeferredItem<Item> DRILL = registerItems("drill");
    public static final DeferredItem<Item> PRESSURE_HEAD = registerItems("pressure_head");
    public static final DeferredItem<Item> RADAR_UNIT = registerItems("radar_unit");
    public static final DeferredItem<Item> SONAR_UNIT = registerItems("sonar_unit");
    public static final DeferredItem<Item> CIRCUIT_BOARD = registerItems("circuit_board");
    public static final DeferredItem<Item> SCREEN = registerItems("screen");
    public static final DeferredItem<Item> MIXER = registerItems("mixer");
    public static final DeferredItem<Item> SAW = registerItems("saw");
    public static final DeferredItem<Item> BELT = registerItems("belt");
    public static final DeferredItem<Item> BALL_BEARING = registerItems("ball_bearing");
    public static final DeferredItem<Item> BRAKE_DISC = registerItems("brake_disc");
    public static final DeferredItem<Item> TENSION_COIL = registerItems("tension_coil");


    public static final DeferredItem<Item> SLIPPERY_COMB = registerItems("slippery_comb");
    public static final DeferredItem<Item> SLIPPERY_PROPOLIS = registerItems("slippery_propolis");
    public static final DeferredItem<Item> GEAR_CAST = registerItems("gear_cast");
    public static final DeferredItem<Item> BASE_PANEL_CAST = registerItems("base_panel_cast");
    public static final DeferredItem<Item> SHAFT_UNIT_CAST = registerItems("shaft_unit_cast");
    public static final DeferredItem<Item> PROPELLER_BLADE_CAST = registerItems("propeller_blade_cast");
    public static final DeferredItem<Item> DRILL_CAST = registerItems("drill_cast");


    public static final DeferredItem<Item> ALUMINUM_ALLOY_INGOT = registerItems("aluminum_alloy_ingot");
    public static final DeferredItem<Item> ALUMINUM_INGOT = registerItems("aluminum_ingot");
    public static final DeferredItem<Item> ANGULAR_TRANSDUCER = registerItems("angular_transducer");
    public static final DeferredItem<Item> ANTHRACITE = registerItems("anthracite");
    public static final DeferredItem<Item> BEDROCK_ALLOY_INGOT = registerItems("bedrock_alloy_ingot");
    public static final DeferredItem<Item> BEDROCK_DRILL_HEAD = registerItems("bedrock_drill_head");
    public static final DeferredItem<Item> COKE_COAL = registerItems("coke_coal");
    public static final DeferredItem<Item> COMPOST = registerItems("compost");
    public static final DeferredItem<Item> CRAFTING_PATTERN = registerItems("crafting_pattern");
    public static final DeferredItem<Item> DRY_ICE = registerItems("dry_ice");
    public static final DeferredItem<Item> ETHANOL_BUCKET = registerItems("ethanol_bucket");
    public static final DeferredItem<Item> ETHANOL_EXTRACT = registerItems("ethanol_extract");
    public static final DeferredItem<Item> ETHANOL_MINECART = registerItems("ethanol_minecart");
    public static final DeferredItem<Item> EXPLOSIVE_SHELL = registerItems("explosive_shell");
    public static final DeferredItem<Item> FLOUR = registerItems("flour");
    public static final DeferredItem<Item> FLUX_AMPLITUDE_UPGRADE = registerItems("flux_amplitude_upgrade");
    public static final DeferredItem<Item> FLUX_CONDUCTANCE_UPGRADE = registerItems("flux_conductance_upgrade");
    public static final DeferredItem<Item> PRISMARINE = registerItems("prismarine");
    public static final DeferredItem<Item> HIGH_STRENGTH_SPRING = registerItems("high_strength_spring");
    public static final DeferredItem<Item> HSLA_STEEL_INGOT = registerItems("hsla_steel_ingot");
    public static final DeferredItem<Item> INDUCTIVE_INGOT = registerItems("inductive_ingot");
    public static final DeferredItem<Item> INDUCTIVE_METAL_BLEND = registerItems("inductive_metal_blend");
    public static final DeferredItem<Item> INTEGRATED_GEARBOX_UPGRADE = registerItems("integrated_gearbox_upgrade");
    public static final DeferredItem<Item> INTEGRATED_GEARBOX_UPGRADE_FLUID = registerItems("integrated_gearbox_upgrade_fluid");
    public static final DeferredItem<Item> INTEGRATED_GEARBOX_UPGRADE_SPEED = registerItems("integrated_gearbox_upgrade_speed");
    public static final DeferredItem<Item> INTEGRATED_GEARBOX_UPGRADE_TORQUE = registerItems("integrated_gearbox_upgrade_torque");
    public static final DeferredItem<Item> INTEGRATED_REDSTONE_UPGRADE = registerItems("integrated_redstone_upgrade");
    public static final DeferredItem<Item> JET_FUEL_BUCKET = registerItems("jet_fuel_bucket");
    public static final DeferredItem<Item> LIQUID_NITROGEN_BUCKET = registerItems("liquid_nitrogen_bucket");
    public static final DeferredItem<Item> LONSDALEITE = registerItems("lonsdaleite");
    public static final DeferredItem<Item> LUBRICANT_BUCKET = registerItems("lubricant_bucket");
    public static final DeferredItem<Item> MAGNETIC_COIL_UPGRADE = registerItems("magnetic_coil_upgrade");
    public static final DeferredItem<Item> MATCH_FILTER = registerItems("match_filter");
    public static final DeferredItem<Item> MOLTEN_HSLA_STEEL_BUCKET = registerItems("molten_hsla_steel_bucket");
    public static final DeferredItem<Item> MOTION_TRACKER = registerItems("motion_tracker");
    public static final DeferredItem<Item> MULCH = registerItems("mulch");
    public static final DeferredItem<Item> MUSIC_BOX_RECORD = registerItems("music_box_record");
    public static final DeferredItem<Item> PERFORMANCE_ENGINE_UPGRADE = registerItems("performance_engine_upgrade");
    public static final DeferredItem<Item> PERMANENT_MAGNET_UPGRADE = registerItems("permanent_magnet_upgrade");
    public static final DeferredItem<Item> PORTABLE_BACKUP_FUEL_TANK = registerItems("portable_backup_fuel_tank");
    public static final DeferredItem<Item> PROJECTOR_SLIDE = registerItems("projector_slide");
    public static final DeferredItem<Item> RAILGUN_AMMUNITION_EXTREME = registerItems("railgun_ammunition_extreme");
    public static final DeferredItem<Item> RAILGUN_AMMUNITION_HIGH = registerItems("railgun_ammunition_high");
    public static final DeferredItem<Item> RAILGUN_AMMUNITION_LOW = registerItems("railgun_ammunition_low");
    public static final DeferredItem<Item> RAILGUN_AMMUNITION_MID = registerItems("railgun_ammunition_mid");
    public static final DeferredItem<Item> RAILGUN_AMMUNITION_VOIDMETAL = registerItems("railgun_ammunition_voidmetal");
    public static final DeferredItem<Item> REDSTONE_COOLING_UPGRADE = registerItems("redstone_cooling_upgrade");
    public static final DeferredItem<Item> RESONANCE_STABILITY_UPGRADE = registerItems("resonance_stability_upgrade");
    public static final DeferredItem<Item> SALT = registerItems("salt");
    public static final DeferredItem<Item> SAWDUST = registerItems("sawdust");
    public static final DeferredItem<Item> SINTERED_TUNGSTEN_INGOT = registerItems("sintered_tungsten_ingot");
    public static final DeferredItem<Item> SLUDGE = registerItems("sludge");
    public static final DeferredItem<Item> SPRING_STEEL_INGOT = registerItems("spring_steel_ingot");
    public static final DeferredItem<Item> TAR_SAND = registerItems("tar_sand");
    public static final DeferredItem<Item> THERMAL_STABILITY_UPGRADE = registerItems("thermal_stability_upgrade");
    public static final DeferredItem<Item> TILE_SELECTOR = registerItems("tile_selector");
    public static final DeferredItem<Item> TNT_CANNON_TARGETING_AID = registerItems("tnt_cannon_targeting_aid");
    public static final DeferredItem<Item> TORSION_RESISTANCE_UPGRADE = registerItems("torsion_resistance_upgrade");
    public static final DeferredItem<Item> TUNGSTEN_ALLOY_INGOT = registerItems("tungsten_alloy_ingot");
    public static final DeferredItem<Item> ULTRASOUND = registerItems("ultrasound");
    public static final DeferredItem<Item> VACUUM = registerItems("vacuum");
    public static final DeferredItem<Item> WIND_SPRING = registerItems("wind_spring");
    public static final DeferredItem<Item> WORLDEDIT_TOOL = registerItems("worldedit_tool");
    public static final DeferredItem<Item> AFTERBURNER_UPGRADE = registerItems("afterburner_upgrade");


    /* ------------- TOOLS ------------- */

    public static final DeferredItem<Item> BEDROCK_HELMET = registerEnchantedTools("bedrock_helmet",
            () -> new BedrockArmorItem(ArmorItem.Type.HELMET));
    public static final DeferredItem<Item> BEDROCK_CHESTPLATE = registerEnchantedTools("bedrock_chestplate",
            () -> new BedrockArmorItem(ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<Item> BEDROCK_LEGGINGS = registerEnchantedTools("bedrock_leggings",
            () -> new BedrockArmorItem(ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<Item> BEDROCK_BOOTS = registerEnchantedTools("bedrock_boots",
            () -> new BedrockArmorItem(ArmorItem.Type.BOOTS));
    public static final DeferredItem<SwordItem> BEDROCK_SWORD = registerEnchantedTools("bedrock_sword",
            BedrockItemConfig.BEDROCK_SWORD_ITEM_SUPPLIER);
    public static final DeferredItem<PickaxeItem> BEDROCK_PICKAXE = registerEnchantedTools("bedrock_pickaxe",
            BedrockItemConfig.BEDROCK_PICKAXE_ITEM_SUPPLIER);
    public static final DeferredItem<AxeItem> BEDROCK_AXE = registerTools("bedrock_axe",
            BedrockItemConfig.BEDROCK_AXE_ITEM_SUPPLIER);
    public static final DeferredItem<ShovelItem> BEDROCK_SHOVEL = registerTools("bedrock_shovel",
            BedrockItemConfig.BEDROCK_SHOVEL_ITEM_SUPPLIER);
    public static final DeferredItem<BedrockHoeItem> BEDROCK_HOE = registerTools("bedrock_hoe",
            BedrockItemConfig.BEDROCK_HOE_ITEM_SUPPLIER);
    public static final DeferredItem<ShearsItem> BEDROCK_SHEARS = registerTools("bedrock_shears",
            BedrockItemConfig.BEDROCK_SHEARS_ITEM_SUPPLIER);

    public static final DeferredItem<Item> HSLA_STEEL_HELMET = registerTools("hsla_steel_helmet",
            () -> new HSLASteelArmorItem(ArmorItem.Type.HELMET));
    public static final DeferredItem<Item> HSLA_STEEL_CHESTPLATE = registerTools("hsla_steel_chestplate",
            () -> new HSLASteelArmorItem(ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<Item> HSLA_STEEL_LEGGINGS = registerTools("hsla_steel_leggings",
            () -> new HSLASteelArmorItem(ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<Item> HSLA_STEEL_BOOTS = registerTools("hsla_steel_boots",
            () -> new HSLASteelArmorItem(ArmorItem.Type.BOOTS));
    public static final DeferredItem<SwordItem> HSLA_STEEL_SWORD = registerTools("hsla_steel_sword",
            HSLASteelItemConfig.HSLA_STEEL_SWORD_ITEM_SUPPLIER);
    public static final DeferredItem<PickaxeItem> HSLA_STEEL_PICKAXE = registerTools("hsla_steel_pickaxe",
            HSLASteelItemConfig.HSLA_STEEL_PICKAXE_ITEM_SUPPLIER);
    public static final DeferredItem<AxeItem> HSLA_STEEL_AXE = registerTools("hsla_steel_axe",
            HSLASteelItemConfig.HSLA_STEEL_AXE_ITEM_SUPPLIER);
    public static final DeferredItem<ShovelItem> HSLA_STEEL_SHOVEL = registerTools("hsla_steel_shovel",
            HSLASteelItemConfig.HSLA_STEEL_SHOVEL_ITEM_SUPPLIER);
    public static final DeferredItem<HoeItem> HSLA_STEEL_HOE = registerTools("hsla_steel_hoe",
            HSLASteelItemConfig.HSLA_STEEL_HOE_ITEM_SUPPLIER);
    public static final DeferredItem<ShearsItem> HSLA_STEEL_SHEARS = registerTools("hsla_steel_shears",
            HSLASteelItemConfig.HSLA_STEEL_SHEARS_ITEM_SUPPLIER);

    public static final DeferredItem<Item> SCREWDRIVER = registerTools("screwdriver");

    public static final DeferredItem<Item> SPRING_BOOTS = registerTools("spring_boots");
    public static final DeferredItem<Item> SPRING_POWERED_GRAFTER = registerTools("spring_powered_grafter");
    public static final DeferredItem<Item> SPRING_POWERED_PUMP = registerTools("spring_powered_pump");
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
    public static final DeferredItem<Item> SILVER_IODIDE = registerOres("silver_iodide");
    public static final DeferredItem<Item> REDSTONE_ORE_FLAKES = registerOres("redstone_ore_flakes");
    public static final DeferredItem<Item> REDSTONE_ORE_SLURRY = registerOres("redstone_ore_slurry");
    public static final DeferredItem<Item> REDSTONE_ORE_SOLUTION = registerOres("redstone_ore_solution");
    public static final DeferredItem<Item> AIR_INFUSED = registerOres("air_infused");
    public static final DeferredItem<Item> AIR_INFUSED_FLAKES = registerOres("air_infused_flakes");
    public static final DeferredItem<Item> AIR_INFUSED_SLURRY = registerOres("air_infused_slurry");
    public static final DeferredItem<Item> AIR_INFUSED_SOLUTION = registerOres("air_infused_solution");
    public static final DeferredItem<Item> ALUMINUM_FLAKES = registerOres("aluminum_flakes");
    public static final DeferredItem<Item> ALUMINUM_SLURRY = registerOres("aluminum_slurry");
    public static final DeferredItem<Item> ALUMINUM_SOLUTION = registerOres("aluminum_solution");
    public static final DeferredItem<Item> AMBER = registerOres("amber");
    public static final DeferredItem<Item> AMBER_FLAKES = registerOres("amber_flakes");
    public static final DeferredItem<Item> AMBER_SLURRY = registerOres("amber_slurry");
    public static final DeferredItem<Item> AMBER_SOLUTION = registerOres("amber_solution");
    public static final DeferredItem<Item> AMETHYST = registerOres("amethyst");
    public static final DeferredItem<Item> AMETHYST_FLAKES = registerOres("amethyst_flakes");
    public static final DeferredItem<Item> AMETHYST_SLURRY = registerOres("amethyst_slurry");
    public static final DeferredItem<Item> AMETHYST_SOLUTION = registerOres("amethyst_solution");
    public static final DeferredItem<Item> AMMONIUM_CHLORIDE_DUST = registerOres("ammonium_chloride_dust");
    public static final DeferredItem<Item> AMMONIUM_CHLORIDE_FLAKES = registerOres("ammonium_chloride_flakes");
    public static final DeferredItem<Item> AMMONIUM_CHLORIDE_SLURRY = registerOres("ammonium_chloride_slurry");
    public static final DeferredItem<Item> AMMONIUM_CHLORIDE_SOLUTION = registerOres("ammonium_chloride_solution");
    public static final DeferredItem<Item> APATITE = registerOres("apatite");
    public static final DeferredItem<Item> APATITE_FLAKES = registerOres("apatite_flakes");
    public static final DeferredItem<Item> APATITE_SLURRY = registerOres("apatite_slurry");
    public static final DeferredItem<Item> APATITE_SOLUTION = registerOres("apatite_solution");
    public static final DeferredItem<Item> ARDITE_FLAKES = registerOres("ardite_flakes");
    public static final DeferredItem<Item> ARDITE_SLURRY = registerOres("ardite_slurry");
    public static final DeferredItem<Item> ARDITE_SOLUTION = registerOres("ardite_solution");
    public static final DeferredItem<Item> BAUXITE_DUST = registerOres("bauxite_dust");
    public static final DeferredItem<Item> BAUXITE_FLAKES = registerOres("bauxite_flakes");
    public static final DeferredItem<Item> BAUXITE_SLURRY = registerOres("bauxite_slurry");
    public static final DeferredItem<Item> BAUXITE_SOLUTION = registerOres("bauxite_solution");
    public static final DeferredItem<Item> BLUE_TOPAZ = registerOres("blue_topaz");
    public static final DeferredItem<Item> BLUE_TOPAZ_FLAKES = registerOres("blue_topaz_flakes");
    public static final DeferredItem<Item> BLUE_TOPAZ_SLURRY = registerOres("blue_topaz_slurry");
    public static final DeferredItem<Item> BLUE_TOPAZ_SOLUTION = registerOres("blue_topaz_solution");
    public static final DeferredItem<Item> CADMIUM_FLAKES = registerOres("cadmium_flakes");
    public static final DeferredItem<Item> CADMIUM_INGOT = registerOres("cadmium_ingot");
    public static final DeferredItem<Item> CADMIUM_SLURRY = registerOres("cadmium_slurry");
    public static final DeferredItem<Item> CADMIUM_SOLUTION = registerOres("cadmium_solution");
    public static final DeferredItem<Item> CALCITE = registerOres("calcite");
    public static final DeferredItem<Item> CALCITE_FLAKES = registerOres("calcite_flakes");
    public static final DeferredItem<Item> CALCITE_SLURRY = registerOres("calcite_slurry");
    public static final DeferredItem<Item> CALCITE_SOLUTION = registerOres("calcite_solution");
    public static final DeferredItem<Item> CERTUS_QUARTZ_FLAKES = registerOres("certus_quartz_flakes");
    public static final DeferredItem<Item> CERTUS_QUARTZ_GEM = registerOres("certus_quartz_gem");
    public static final DeferredItem<Item> CERTUS_QUARTZ_SLURRY = registerOres("certus_quartz_slurry");
    public static final DeferredItem<Item> CERTUS_QUARTZ_SOLUTION = registerOres("certus_quartz_solution");
    public static final DeferredItem<Item> CHIMERITE = registerOres("chimerite");
    public static final DeferredItem<Item> CHIMERITE_FLAKES = registerOres("chimerite_flakes");
    public static final DeferredItem<Item> CHIMERITE_SLURRY = registerOres("chimerite_slurry");
    public static final DeferredItem<Item> CHIMERITE_SOLUTION = registerOres("chimerite_solution");
    public static final DeferredItem<Item> CHROMITE_FLAKES = registerOres("chromite_flakes");
    public static final DeferredItem<Item> CHROMITE_INGOT = registerOres("chromite_ingot");
    public static final DeferredItem<Item> CHROMITE_SLURRY = registerOres("chromite_slurry");
    public static final DeferredItem<Item> CHROMITE_SOLUTION = registerOres("chromite_solution");
    public static final DeferredItem<Item> COBALT_FLAKES = registerOres("cobalt_flakes");
    public static final DeferredItem<Item> COBALT_INGOT = registerOres("cobalt_ingot");
    public static final DeferredItem<Item> COBALT_SLURRY = registerOres("cobalt_slurry");
    public static final DeferredItem<Item> COBALT_SOLUTION = registerOres("cobalt_solution");
    public static final DeferredItem<Item> COPPER_FLAKES = registerOres("copper_flakes");
    public static final DeferredItem<Item> COPPER_INGOT = registerOres("copper_ingot");
    public static final DeferredItem<Item> COPPER_SLURRY = registerOres("copper_slurry");
    public static final DeferredItem<Item> COPPER_SOLUTION = registerOres("copper_solution");
    public static final DeferredItem<Item> DARK_IRON_FLAKES = registerOres("dark_iron_flakes");
    public static final DeferredItem<Item> DARK_IRON_INGOT = registerOres("dark_iron_ingot");
    public static final DeferredItem<Item> DARK_IRON_SLURRY = registerOres("dark_iron_slurry");
    public static final DeferredItem<Item> DARK_IRON_SOLUTION = registerOres("dark_iron_solution");
    public static final DeferredItem<Item> DILITHIUM_DUST = registerOres("dilithium_dust");
    public static final DeferredItem<Item> DILITHIUM_FLAKES = registerOres("dilithium_flakes");
    public static final DeferredItem<Item> DILITHIUM_SLURRY = registerOres("dilithium_slurry");
    public static final DeferredItem<Item> DILITHIUM_SOLUTION = registerOres("dilithium_solution");
    public static final DeferredItem<Item> DRACONIUM_DUST = registerOres("draconium_dust");
    public static final DeferredItem<Item> DRACONIUM_FLAKES = registerOres("draconium_flakes");
    public static final DeferredItem<Item> DRACONIUM_SLURRY = registerOres("draconium_slurry");
    public static final DeferredItem<Item> DRACONIUM_SOLUTION = registerOres("draconium_solution");
    public static final DeferredItem<Item> EARTH_INFUSED_ = registerOres("earth_infused_");
    public static final DeferredItem<Item> EARTH_INFUSED_FLAKES = registerOres("earth_infused_flakes");
    public static final DeferredItem<Item> EARTH_INFUSED_GEM = registerOres("earth_infused_gem");
    public static final DeferredItem<Item> EARTH_INFUSED_SOLUTION = registerOres("earth_infused_solution");
    public static final DeferredItem<Item> ENDIUM_FLAKES = registerOres("endium_flakes");
    public static final DeferredItem<Item> ENDIUM_INGOT = registerOres("endium_ingot");
    public static final DeferredItem<Item> ENDIUM_SLURRY = registerOres("endium_slurry");
    public static final DeferredItem<Item> ENDIUM_SOLUTION = registerOres("endium_solution");
    public static final DeferredItem<Item> ENTROPY_INFUSED_FLAKES = registerOres("entropy_infused_flakes");
    public static final DeferredItem<Item> ENTROPY_INFUSED_GEM = registerOres("entropy_infused_gem");
    public static final DeferredItem<Item> ENTROPY_INFUSED_SLURRY = registerOres("entropy_infused_slurry");
    public static final DeferredItem<Item> ENTROPY_INFUSED_SOLUTION = registerOres("entropy_infused_solution");
    public static final DeferredItem<Item> ESSENCE = registerOres("essence");
    public static final DeferredItem<Item> ESSENCE_FLAKES = registerOres("essence_flakes");
    public static final DeferredItem<Item> ESSENCE_SLURRY = registerOres("essence_slurry");
    public static final DeferredItem<Item> ESSENCE_SOLUTION = registerOres("essence_solution");
    public static final DeferredItem<Item> EXIMITE_FLAKES = registerOres("eximite_flakes");
    public static final DeferredItem<Item> EXIMITE_INGOT = registerOres("eximite_ingot");
    public static final DeferredItem<Item> EXIMITE_SLURRY = registerOres("eximite_slurry");
    public static final DeferredItem<Item> EXIMITE_SOLUTION = registerOres("eximite_solution");
    public static final DeferredItem<Item> FIRESTONE = registerOres("firestone");
    public static final DeferredItem<Item> FIRESTONE_FLAKES = registerOres("firestone_flakes");
    public static final DeferredItem<Item> FIRESTONE_SLURRY = registerOres("firestone_slurry");
    public static final DeferredItem<Item> FIRESTONE_SOLUTION = registerOres("firestone_solution");
    public static final DeferredItem<Item> FIRE_INFUSED = registerOres("fire_infused");
    public static final DeferredItem<Item> FIRE_INFUSED_FLAKES = registerOres("fire_infused_flakes");
    public static final DeferredItem<Item> FIRE_INFUSED_SLURRY = registerOres("fire_infused_slurry");
    public static final DeferredItem<Item> FIRE_INFUSED_SOLUTION = registerOres("fire_infused_solution");
    public static final DeferredItem<Item> FLUORITE = registerOres("fluorite");
    public static final DeferredItem<Item> FLUORITE_FLAKES = registerOres("fluorite_flakes");
    public static final DeferredItem<Item> FLUORITE_SLURRY = registerOres("fluorite_slurry");
    public static final DeferredItem<Item> FLUORITE_SOLUTION = registerOres("fluorite_solution");
    public static final DeferredItem<Item> FORCE = registerOres("force");
    public static final DeferredItem<Item> FORCE_FLAKES = registerOres("force_flakes");
    public static final DeferredItem<Item> FORCE_SLURRY = registerOres("force_slurry");
    public static final DeferredItem<Item> FORCE_SOLUTION = registerOres("force_solution");
    public static final DeferredItem<Item> GALENA_DUST = registerOres("galena_dust");
    public static final DeferredItem<Item> GALENA_FLAKES = registerOres("galena_flakes");
    public static final DeferredItem<Item> GALENA_SLURRY = registerOres("galena_slurry");
    public static final DeferredItem<Item> GALENA_SOLUTION = registerOres("galena_solution");
    public static final DeferredItem<Item> INDIUM_FLAKES = registerOres("indium_flakes");
    public static final DeferredItem<Item> INDIUM_INGOT = registerOres("indium_ingot");
    public static final DeferredItem<Item> INDIUM_SLURRY = registerOres("indium_slurry");
    public static final DeferredItem<Item> INDIUM_SOLUTION = registerOres("indium_solution");
    public static final DeferredItem<Item> IRIDIUM_FLAKES = registerOres("iridium_flakes");
    public static final DeferredItem<Item> IRIDIUM_INGOT = registerOres("iridium_ingot");
    public static final DeferredItem<Item> IRIDIUM_SLURRY = registerOres("iridium_slurry");
    public static final DeferredItem<Item> IRIDIUM_SOLUTION = registerOres("iridium_solution");
    public static final DeferredItem<Item> LEAD_FLAKES = registerOres("lead_flakes");
    public static final DeferredItem<Item> LEAD_INGOT = registerOres("lead_ingot");
    public static final DeferredItem<Item> LEAD_SLURRY = registerOres("lead_slurry");
    public static final DeferredItem<Item> LEAD_SOLUTION = registerOres("lead_solution");
    public static final DeferredItem<Item> MAGMANITE = registerOres("magmanite");
    public static final DeferredItem<Item> MAGMANITE_FLAKES = registerOres("magmanite_flakes");
    public static final DeferredItem<Item> MAGMANITE_SLURRY = registerOres("magmanite_slurry");
    public static final DeferredItem<Item> MAGMANITE_SOLUTION = registerOres("magmanite_solution");
    public static final DeferredItem<Item> MAGNETITE = registerOres("magnetite");
    public static final DeferredItem<Item> MAGNETITE_FLAKES = registerOres("magnetite_flakes");
    public static final DeferredItem<Item> MAGNETITE_SLURRY = registerOres("magnetite_slurry");
    public static final DeferredItem<Item> MAGNETITE_SOLUTION = registerOres("magnetite_solution");
    public static final DeferredItem<Item> MANA_FLAKES = registerOres("mana_flakes");
    public static final DeferredItem<Item> MANA_INGOT = registerOres("mana_ingot");
    public static final DeferredItem<Item> MANA_SLURRY = registerOres("mana_slurry");
    public static final DeferredItem<Item> MANA_SOLUTION = registerOres("mana_solution");
    public static final DeferredItem<Item> MERCURY = registerOres("mercury");
    public static final DeferredItem<Item> MERCURY_FLAKES = registerOres("mercury_flakes");
    public static final DeferredItem<Item> MERCURY_SLURRY = registerOres("mercury_slurry");
    public static final DeferredItem<Item> MERCURY_SOLUTION = registerOres("mercury_solution");
    public static final DeferredItem<Item> MEUTOITE_FLAKES = registerOres("meutoite_flakes");
    public static final DeferredItem<Item> MEUTOITE_INGOT = registerOres("meutoite_ingot");
    public static final DeferredItem<Item> MEUTOITE_SLURRY = registerOres("meutoite_slurry");
    public static final DeferredItem<Item> MEUTOITE_SOLUTION = registerOres("meutoite_solution");
    public static final DeferredItem<Item> MIMICHITE = registerOres("mimichite");
    public static final DeferredItem<Item> MIMICHITE_FLAKES = registerOres("mimichite_flakes");
    public static final DeferredItem<Item> MIMICHITE_SLURRY = registerOres("mimichite_slurry");
    public static final DeferredItem<Item> MIMICHITE_SOLUTION = registerOres("mimichite_solution");
    public static final DeferredItem<Item> MONAZIT_FLAKES = registerOres("monazit_flakes");
    public static final DeferredItem<Item> MONAZIT_SLURRY = registerOres("monazit_slurry");
    public static final DeferredItem<Item> MONAZIT_SOLUTION = registerOres("monazit_solution");
    public static final DeferredItem<Item> MONZANITE_GEM = registerOres("monzanite_gem");
    public static final DeferredItem<Item> MOONSTONE = registerOres("moonstone");
    public static final DeferredItem<Item> MOONSTONE_FLAKES = registerOres("moonstone_flakes");
    public static final DeferredItem<Item> MOONSTONE_SLURRY = registerOres("moonstone_slurry");
    public static final DeferredItem<Item> MOONSTONE_SOLUTION = registerOres("moonstone_solution");
    public static final DeferredItem<Item> NETHER_COAL_FLAKES = registerOres("nether_coal_flakes");
    public static final DeferredItem<Item> NETHER_COAL_GEM = registerOres("nether_coal_gem");
    public static final DeferredItem<Item> NETHER_COAL_SLURRY = registerOres("nether_coal_slurry");
    public static final DeferredItem<Item> NETHER_COAL_SOLUTION = registerOres("nether_coal_solution");
    public static final DeferredItem<Item> NETHER_COPPER_FLAKES = registerOres("nether_copper_flakes");
    public static final DeferredItem<Item> NETHER_COPPER_INGOT = registerOres("nether_copper_ingot");
    public static final DeferredItem<Item> NETHER_COPPER_SLURRY = registerOres("nether_copper_slurry");
    public static final DeferredItem<Item> NETHER_COPPER_SOLUTION = registerOres("nether_copper_solution");
    public static final DeferredItem<Item> NETHER_DIAMOND = registerOres("nether_diamond");
    public static final DeferredItem<Item> NETHER_DIAMOND_FLAKES = registerOres("nether_diamond_flakes");
    public static final DeferredItem<Item> NETHER_DIAMOND_SLURRY = registerOres("nether_diamond_slurry");
    public static final DeferredItem<Item> NETHER_DIAMOND_SOLUTION = registerOres("nether_diamond_solution");
    public static final DeferredItem<Item> NETHER_EMERALD = registerOres("nether_emerald");
    public static final DeferredItem<Item> NETHER_EMERALD_FLAKES = registerOres("nether_emerald_flakes");
    public static final DeferredItem<Item> NETHER_EMERALD_SLURRY = registerOres("nether_emerald_slurry");
    public static final DeferredItem<Item> NETHER_EMERALD_SOLUTION = registerOres("nether_emerald_solution");
    public static final DeferredItem<Item> NETHER_GOLD_FLAKES = registerOres("nether_gold_flakes");
    public static final DeferredItem<Item> NETHER_GOLD_INGOT = registerOres("nether_gold_ingot");
    public static final DeferredItem<Item> NETHER_GOLD_SLURRY = registerOres("nether_gold_slurry");
    public static final DeferredItem<Item> NETHER_GOLD_SOLUTION = registerOres("nether_gold_solution");
    public static final DeferredItem<Item> NETHER_IRIDIUM_FLAKES = registerOres("nether_iridium_flakes");
    public static final DeferredItem<Item> NETHER_IRIDIUM_INGOT = registerOres("nether_iridium_ingot");
    public static final DeferredItem<Item> NETHER_IRIDIUM_SLURRY = registerOres("nether_iridium_slurry");
    public static final DeferredItem<Item> NETHER_IRIDIUM_SOLUTION = registerOres("nether_iridium_solution");
    public static final DeferredItem<Item> NETHER_IRON_FLAKES = registerOres("nether_iron_flakes");
    public static final DeferredItem<Item> NETHER_IRON_INGOT = registerOres("nether_iron_ingot");
    public static final DeferredItem<Item> NETHER_IRON_SLURRY = registerOres("nether_iron_slurry");
    public static final DeferredItem<Item> NETHER_IRON_SOLUTION = registerOres("nether_iron_solution");
    public static final DeferredItem<Item> NETHER_LAPIS_FLAKES = registerOres("nether_lapis_flakes");
    public static final DeferredItem<Item> NETHER_LAPIS_GEM = registerOres("nether_lapis_gem");
    public static final DeferredItem<Item> NETHER_LAPIS_SLURRY = registerOres("nether_lapis_slurry");
    public static final DeferredItem<Item> NETHER_LAPIS_SOLUTION = registerOres("nether_lapis_solution");
    public static final DeferredItem<Item> NETHER_LEAD_FLAKES = registerOres("nether_lead_flakes");
    public static final DeferredItem<Item> NETHER_LEAD_INGOT = registerOres("nether_lead_ingot");
    public static final DeferredItem<Item> NETHER_LEAD_SLURRY = registerOres("nether_lead_slurry");
    public static final DeferredItem<Item> NETHER_LEAD_SOLUTION = registerOres("nether_lead_solution");
    public static final DeferredItem<Item> NETHER_NICKEL_FLAKES = registerOres("nether_nickel_flakes");
    public static final DeferredItem<Item> NETHER_NICKEL_INGOT = registerOres("nether_nickel_ingot");
    public static final DeferredItem<Item> NETHER_NICKEL_SLURRY = registerOres("nether_nickel_slurry");
    public static final DeferredItem<Item> NETHER_NICKEL_SOLUTION = registerOres("nether_nickel_solution");
    public static final DeferredItem<Item> NETHER_NIKOLITE_DUST = registerOres("nether_nikolite_dust");
    public static final DeferredItem<Item> NETHER_NIKOLITE_FLAKES = registerOres("nether_nikolite_flakes");
    public static final DeferredItem<Item> NETHER_NIKOLITE_SLURRY = registerOres("nether_nikolite_slurry");
    public static final DeferredItem<Item> NETHER_NIKOLITE_SOLUTION = registerOres("nether_nikolite_solution");
    public static final DeferredItem<Item> NETHER_OSMIUM_FLAKES = registerOres("nether_osmium_flakes");
    public static final DeferredItem<Item> NETHER_OSMIUM_INGOT = registerOres("nether_osmium_ingot");
    public static final DeferredItem<Item> NETHER_OSMIUM_SLURRY = registerOres("nether_osmium_slurry");
    public static final DeferredItem<Item> NETHER_OSMIUM_SOLUTION = registerOres("nether_osmium_solution");
    public static final DeferredItem<Item> NETHER_PERIDOT = registerOres("nether_peridot");
    public static final DeferredItem<Item> NETHER_PERIDOT_FLAKES = registerOres("nether_peridot_flakes");
    public static final DeferredItem<Item> NETHER_PERIDOT_SLURRY = registerOres("nether_peridot_slurry");
    public static final DeferredItem<Item> NETHER_PERIDOT_SOLUTION = registerOres("nether_peridot_solution");
    public static final DeferredItem<Item> NETHER_PLATINUM_FLAKES = registerOres("nether_platinum_flakes");
    public static final DeferredItem<Item> NETHER_PLATINUM_INGOT = registerOres("nether_platinum_ingot");
    public static final DeferredItem<Item> NETHER_PLATINUM_SLURRY = registerOres("nether_platinum_slurry");
    public static final DeferredItem<Item> NETHER_PLATINUM_SOLUTION = registerOres("nether_platinum_solution");
    public static final DeferredItem<Item> NETHER_REDSTONE_DUST = registerOres("nether_redstone_dust");
    public static final DeferredItem<Item> NETHER_REDSTONE_FLAKES = registerOres("nether_redstone_flakes");
    public static final DeferredItem<Item> NETHER_REDSTONE_SLURRY = registerOres("nether_redstone_slurry");
    public static final DeferredItem<Item> NETHER_REDSTONE_SOLUTION = registerOres("nether_redstone_solution");
    public static final DeferredItem<Item> NETHER_SALTPETER = registerOres("nether_saltpeter");
    public static final DeferredItem<Item> NETHER_SALTPETER_FLAKES = registerOres("nether_saltpeter_flakes");
    public static final DeferredItem<Item> NETHER_SALTPETER_SLURRY = registerOres("nether_saltpeter_slurry");
    public static final DeferredItem<Item> NETHER_SALTPETER_SOLUTIOM = registerOres("nether_saltpeter_solutiom");
    public static final DeferredItem<Item> NETHER_SAPPHIRE = registerOres("nether_sapphire");
    public static final DeferredItem<Item> NETHER_SAPPHIRE_FLAKES = registerOres("nether_sapphire_flakes");
    public static final DeferredItem<Item> NETHER_SAPPHIRE_SLURRY = registerOres("nether_sapphire_slurry");
    public static final DeferredItem<Item> NETHER_SAPPHIRE_SOLUTION = registerOres("nether_sapphire_solution");
    public static final DeferredItem<Item> NETHER_SILICON_FLAKES = registerOres("nether_silicon_flakes");
    public static final DeferredItem<Item> NETHER_SILICON_SLURRY = registerOres("nether_silicon_slurry");
    public static final DeferredItem<Item> NETHER_SILICON_SOLUTION = registerOres("nether_silicon_solution");
    public static final DeferredItem<Item> NETHER_SILVER_FLAKES = registerOres("nether_silver_flakes");
    public static final DeferredItem<Item> NETHER_SILVER_INGOT = registerOres("nether_silver_ingot");
    public static final DeferredItem<Item> NETHER_SILVER_SLURRY = registerOres("nether_silver_slurry");
    public static final DeferredItem<Item> NETHER_SILVER_SOLUTION = registerOres("nether_silver_solution");
    public static final DeferredItem<Item> NETHER_SULFUR_DUST = registerOres("nether_sulfur_dust");
    public static final DeferredItem<Item> NETHER_SULFUR_FLAKES = registerOres("nether_sulfur_flakes");
    public static final DeferredItem<Item> NETHER_SULFUR_SLURRY = registerOres("nether_sulfur_slurry");
    public static final DeferredItem<Item> NETHER_SULFUR_SOLUTION = registerOres("nether_sulfur_solution");
    public static final DeferredItem<Item> NETHER_TIN_FLAKES = registerOres("nether_tin_flakes");
    public static final DeferredItem<Item> NETHER_TIN_INGOT = registerOres("nether_tin_ingot");
    public static final DeferredItem<Item> NETHER_TIN_SLURRY = registerOres("nether_tin_slurry");
    public static final DeferredItem<Item> NETHER_TIN_SOLUTION = registerOres("nether_tin_solution");
    public static final DeferredItem<Item> NETHER_TITANIUM_FLAKES = registerOres("nether_titanium_flakes");
    public static final DeferredItem<Item> NETHER_TITANIUM_INGOT = registerOres("nether_titanium_ingot");
    public static final DeferredItem<Item> NETHER_TITANIUM_SLURRY = registerOres("nether_titanium_slurry");
    public static final DeferredItem<Item> NETHER_TITANIUM_SOLUTION = registerOres("nether_titanium_solution");
    public static final DeferredItem<Item> NETHER_URANIUM_FLAKES = registerOres("nether_uranium_flakes");
    public static final DeferredItem<Item> NETHER_URANIUM_INGOT = registerOres("nether_uranium_ingot");
    public static final DeferredItem<Item> NETHER_URANIUM_SLURRY = registerOres("nether_uranium_slurry");
    public static final DeferredItem<Item> NETHER_URANIUM_SOLUTION = registerOres("nether_uranium_solution");
    public static final DeferredItem<Item> NICKEL_FLAKES = registerOres("nickel_flakes");
    public static final DeferredItem<Item> NICKEL_INGOT = registerOres("nickel_ingot");
    public static final DeferredItem<Item> NICKEL_SLURRY = registerOres("nickel_slurry");
    public static final DeferredItem<Item> NICKEL_SOLUTION = registerOres("nickel_solution");
    public static final DeferredItem<Item> NIKOLITE_DUST = registerOres("nikolite_dust");
    public static final DeferredItem<Item> NIKOLITE_FLAKES = registerOres("nikolite_flakes");
    public static final DeferredItem<Item> NIKOLITE_SLURRY = registerOres("nikolite_slurry");
    public static final DeferredItem<Item> ORDER_INFUSED_FLAKES = registerOres("order_infused_flakes");
    public static final DeferredItem<Item> ORDER_INFUSED_GEM = registerOres("order_infused_gem");
    public static final DeferredItem<Item> ORDER_INFUSED_SLURRY = registerOres("order_infused_slurry");
    public static final DeferredItem<Item> ORDER_INFUSED_SOLUTION = registerOres("order_infused_solution");
    public static final DeferredItem<Item> OSMIUM_FLAKES = registerOres("osmium_flakes");
    public static final DeferredItem<Item> OSMIUM_INGOT = registerOres("osmium_ingot");
    public static final DeferredItem<Item> OSMIUM_SLURRY = registerOres("osmium_slurry");
    public static final DeferredItem<Item> OSMIUM_SOLUTION = registerOres("osmium_solution");
    public static final DeferredItem<Item> PERIDOT = registerOres("peridot");
    public static final DeferredItem<Item> PERIDOT_FLAKES = registerOres("peridot_flakes");
    public static final DeferredItem<Item> PERIDOT_SLURRY = registerOres("peridot_slurry");
    public static final DeferredItem<Item> PERIDOT_SOLUTION = registerOres("peridot_solution");
    public static final DeferredItem<Item> PIG_IRON_FLAKES = registerOres("pig_iron_flakes");
    public static final DeferredItem<Item> PIG_IRON_INGOT = registerOres("pig_iron_ingot");
    public static final DeferredItem<Item> PIG_IRON_SLURRY = registerOres("pig_iron_slurry");
    public static final DeferredItem<Item> PIG_IRON_SOLUTION = registerOres("pig_iron_solution");
    public static final DeferredItem<Item> PITCHBLENDE_FLAKES = registerOres("pitchblende_flakes");
    public static final DeferredItem<Item> PITCHBLENDE_INGOT = registerOres("pitchblende_ingot");
    public static final DeferredItem<Item> PITCHBLENDE_SLURRY = registerOres("pitchblende_slurry");
    public static final DeferredItem<Item> PITCHBLENDE_SOLUTION = registerOres("pitchblende_solution");
    public static final DeferredItem<Item> PLATINUM_FLAKES = registerOres("platinum_flakes");
    public static final DeferredItem<Item> PLATINUM_INGOT = registerOres("platinum_ingot");
    public static final DeferredItem<Item> PLATINUM_SLURRY = registerOres("platinum_slurry");
    public static final DeferredItem<Item> PLATINUM_SOLUTION = registerOres("platinum_solution");
    public static final DeferredItem<Item> POWDERED_AIR_INFUSED = registerOres("powdered_air_infused");
    public static final DeferredItem<Item> POWDERED_ALUMINUM = registerOres("powdered_aluminum");
    public static final DeferredItem<Item> POWDERED_AMBER = registerOres("powdered_amber");
    public static final DeferredItem<Item> POWDERED_AMETHYST = registerOres("powdered_amethyst");
    public static final DeferredItem<Item> POWDERED_AMMONIUM_CHLORIDE = registerOres("powdered_ammonium_chloride");
    public static final DeferredItem<Item> POWDERED_APATITE = registerOres("powdered_apatite");
    public static final DeferredItem<Item> POWDERED_ARDITE = registerOres("powdered_ardite");
    public static final DeferredItem<Item> POWDERED_BAUXITE = registerOres("powdered_bauxite");
    public static final DeferredItem<Item> POWDERED_BLUE_TOPAZ = registerOres("powdered_blue_topaz");
    public static final DeferredItem<Item> POWDERED_CADMIUM = registerOres("powdered_cadmium");
    public static final DeferredItem<Item> POWDERED_CALCITE = registerOres("powdered_calcite");
    public static final DeferredItem<Item> POWDERED_CERTUS_QUARTZ = registerOres("powdered_certus_quartz");
    public static final DeferredItem<Item> POWDERED_CHIMERITE = registerOres("powdered_chimerite");
    public static final DeferredItem<Item> POWDERED_CHROMITE = registerOres("powdered_chromite");
    public static final DeferredItem<Item> POWDERED_COBALT = registerOres("powdered_cobalt");
    public static final DeferredItem<Item> POWDERED_COPPER = registerOres("powdered_copper");
    public static final DeferredItem<Item> POWDERED_DARK_IRON = registerOres("powdered_dark_iron");
    public static final DeferredItem<Item> POWDERED_DILITHIUM = registerOres("powdered_dilithium");
    public static final DeferredItem<Item> POWDERED_DRACONIUM = registerOres("powdered_draconium");
    public static final DeferredItem<Item> POWDERED_EARTH_INFUSED = registerOres("powdered_earth_infused");
    public static final DeferredItem<Item> POWDERED_ENDIUM = registerOres("powdered_endium");
    public static final DeferredItem<Item> POWDERED_ENTROPY_INFUSED = registerOres("powdered_entropy_infused");
    public static final DeferredItem<Item> POWDERED_ESSENCE = registerOres("powdered_essence");
    public static final DeferredItem<Item> POWDERED_EXIMITE = registerOres("powdered_eximite");
    public static final DeferredItem<Item> POWDERED_FIRESTONE = registerOres("powdered_firestone");
    public static final DeferredItem<Item> POWDERED_FIRE_INFUSED = registerOres("powdered_fire_infused");
    public static final DeferredItem<Item> POWDERED_FLUORITE = registerOres("powdered_fluorite");
    public static final DeferredItem<Item> POWDERED_FORCE = registerOres("powdered_force");
    public static final DeferredItem<Item> POWDERED_GALENA = registerOres("powdered_galena");
    public static final DeferredItem<Item> POWDERED_INDIUM = registerOres("powdered_indium");
    public static final DeferredItem<Item> POWDERED_IRIDIUM = registerOres("powdered_iridium");
    public static final DeferredItem<Item> POWDERED_LEAD = registerOres("powdered_lead");
    public static final DeferredItem<Item> POWDERED_MAGMANITE = registerOres("powdered_magmanite");
    public static final DeferredItem<Item> POWDERED_MAGNETITE = registerOres("powdered_magnetite");
    public static final DeferredItem<Item> POWDERED_MANA = registerOres("powdered_mana");
    public static final DeferredItem<Item> POWDERED_MERCURY = registerOres("powdered_mercury");
    public static final DeferredItem<Item> POWDERED_MEUTOITE = registerOres("powdered_meutoite");
    public static final DeferredItem<Item> POWDERED_MIMICHITE = registerOres("powdered_mimichite");
    public static final DeferredItem<Item> POWDERED_MONZANITE = registerOres("powdered_monzanite");
    public static final DeferredItem<Item> POWDERED_MOONSTONE = registerOres("powdered_moonstone");
    public static final DeferredItem<Item> POWDERED_NETHER_COAL = registerOres("powdered_nether_coal");
    public static final DeferredItem<Item> POWDERED_NETHER_COPPER = registerOres("powdered_nether_copper");
    public static final DeferredItem<Item> POWDERED_NETHER_DIAMOND = registerOres("powdered_nether_diamond");
    public static final DeferredItem<Item> POWDERED_NETHER_EMERALD = registerOres("powdered_nether_emerald");
    public static final DeferredItem<Item> POWDERED_NETHER_GOLD = registerOres("powdered_nether_gold");
    public static final DeferredItem<Item> POWDERED_NETHER_IRIDIUM = registerOres("powdered_nether_iridium");
    public static final DeferredItem<Item> POWDERED_NETHER_IRON = registerOres("powdered_nether_iron");
    public static final DeferredItem<Item> POWDERED_NETHER_LAPIS = registerOres("powdered_nether_lapis");
    public static final DeferredItem<Item> POWDERED_NETHER_LEAD = registerOres("powdered_nether_lead");
    public static final DeferredItem<Item> POWDERED_NETHER_NICKEL = registerOres("powdered_nether_nickel");
    public static final DeferredItem<Item> POWDERED_NETHER_NIKOLITE = registerOres("powdered_nether_nikolite");
    public static final DeferredItem<Item> POWDERED_NETHER_OSMIUM = registerOres("powdered_nether_osmium");
    public static final DeferredItem<Item> POWDERED_NETHER_PERIDOT = registerOres("powdered_nether_peridot");
    public static final DeferredItem<Item> POWDERED_NETHER_PLATINUM = registerOres("powdered_nether_platinum");
    public static final DeferredItem<Item> POWDERED_NETHER_REDSTONE = registerOres("powdered_nether_redstone");
    public static final DeferredItem<Item> POWDERED_NETHER_SALTPETER = registerOres("powdered_nether_saltpeter");
    public static final DeferredItem<Item> POWDERED_NETHER_SAPPHIRE = registerOres("powdered_nether_sapphire");
    public static final DeferredItem<Item> POWDERED_NETHER_SILVER = registerOres("powdered_nether_silver");
    public static final DeferredItem<Item> POWDERED_NETHER_SULFUR = registerOres("powdered_nether_sulfur");
    public static final DeferredItem<Item> POWDERED_NETHER_TIN = registerOres("powdered_nether_tin");
    public static final DeferredItem<Item> POWDERED_NETHER_TITANIUM = registerOres("powdered_nether_titanium");
    public static final DeferredItem<Item> POWDERED_NETHER_URANIUM = registerOres("powdered_nether_uranium");
    public static final DeferredItem<Item> POWDERED_NICKEL = registerOres("powdered_nickel");
    public static final DeferredItem<Item> POWDERED_NIKOLITE = registerOres("powdered_nikolite");
    public static final DeferredItem<Item> POWDERED_ORDER_INFUSED = registerOres("powdered_order_infused");
    public static final DeferredItem<Item> POWDERED_OSMIUM = registerOres("powdered_osmium");
    public static final DeferredItem<Item> POWDERED_PERIDOT = registerOres("powdered_peridot");
    public static final DeferredItem<Item> POWDERED_PIG_IRON = registerOres("powdered_pig_iron");
    public static final DeferredItem<Item> POWDERED_PITCHBLENDE = registerOres("powdered_pitchblende");
    public static final DeferredItem<Item> POWDERED_PLATINUM = registerOres("powdered_platinum");
    public static final DeferredItem<Item> POWDERED_PYRITE = registerOres("powdered_pyrite");
    public static final DeferredItem<Item> POWDERED_QUANTUM = registerOres("powdered_quantum");
    public static final DeferredItem<Item> POWDERED_RUBY = registerOres("powdered_ruby");
    public static final DeferredItem<Item> POWDERED_RUTILE = registerOres("powdered_rutile");
    public static final DeferredItem<Item> POWDERED_SALTPETER = registerOres("powdered_saltpeter");
    public static final DeferredItem<Item> POWDERED_SAPPHIRE = registerOres("powdered_sapphire");
    public static final DeferredItem<Item> POWDERED_SILICON = registerOres("powdered_silicon");
    public static final DeferredItem<Item> POWDERED_SILVER = registerOres("powdered_silver");
    public static final DeferredItem<Item> POWDERED_SULFUR = registerOres("powdered_sulfur");
    public static final DeferredItem<Item> POWDERED_SUNSTONE = registerOres("powdered_sunstone");
    public static final DeferredItem<Item> POWDERED_TESLATITE = registerOres("powdered_teslatite");
    public static final DeferredItem<Item> POWDERED_THORIUM = registerOres("powdered_thorium");
    public static final DeferredItem<Item> POWDERED_TIN = registerOres("powdered_tin");
    public static final DeferredItem<Item> POWDERED_TITANIUM = registerOres("powdered_titanium");
    public static final DeferredItem<Item> POWDERED_TUNGSTEN = registerOres("powdered_tungsten");
    public static final DeferredItem<Item> POWDERED_URANIUM = registerOres("powdered_uranium");
    public static final DeferredItem<Item> POWDERED_VINTEUM = registerOres("powdered_vinteum");
    public static final DeferredItem<Item> POWDERED_WATER_INFUSED = registerOres("powdered_water_infused");
    public static final DeferredItem<Item> POWDERED_ZINC = registerOres("powdered_zinc");
    public static final DeferredItem<Item> PYRITE_DUST = registerOres("pyrite_dust");
    public static final DeferredItem<Item> PYRITE_FLAKES = registerOres("pyrite_flakes");
    public static final DeferredItem<Item> PYRITE_SLURRY = registerOres("pyrite_slurry");
    public static final DeferredItem<Item> PYRITE_SOLUTION = registerOres("pyrite_solution");
    public static final DeferredItem<Item> QUANTUM_DUST = registerOres("quantum_dust");
    public static final DeferredItem<Item> QUANTUM_FLAKES = registerOres("quantum_flakes");
    public static final DeferredItem<Item> QUANTUM_SLURRY = registerOres("quantum_slurry");
    public static final DeferredItem<Item> QUANTUM_SOLUTION = registerOres("quantum_solution");
    public static final DeferredItem<Item> RUBY = registerOres("ruby");
    public static final DeferredItem<Item> RUBY_FLAKES = registerOres("ruby_flakes");
    public static final DeferredItem<Item> RUBY_SLURRY = registerOres("ruby_slurry");
    public static final DeferredItem<Item> RUBY_SOLUTION = registerOres("ruby_solution");
    public static final DeferredItem<Item> RUTILE_FLAKES = registerOres("rutile_flakes");
    public static final DeferredItem<Item> RUTILE_INGOT = registerOres("rutile_ingot");
    public static final DeferredItem<Item> RUTILE_SLURRY = registerOres("rutile_slurry");
    public static final DeferredItem<Item> RUTILE_SOLUTION = registerOres("rutile_solution");
    public static final DeferredItem<Item> SALTPETER_DUST = registerOres("saltpeter_dust");
    public static final DeferredItem<Item> SALTPETER_FLAKES = registerOres("saltpeter_flakes");
    public static final DeferredItem<Item> SALTPETER_SLURRY = registerOres("saltpeter_slurry");
    public static final DeferredItem<Item> SALTPETER_SOLUTION = registerOres("saltpeter_solution");
    public static final DeferredItem<Item> SAPPHIRE = registerOres("sapphire");
    public static final DeferredItem<Item> SAPPHIRE_FLAKES = registerOres("sapphire_flakes");
    public static final DeferredItem<Item> SAPPHIRE_SLURRY = registerOres("sapphire_slurry");
    public static final DeferredItem<Item> SAPPHIRE_SOLUTION = registerOres("sapphire_solution");
    public static final DeferredItem<Item> SILICON = registerOres("silicon");
    public static final DeferredItem<Item> SILVER_FLAKES = registerOres("silver_flakes");
    public static final DeferredItem<Item> SILVER_INGOT = registerOres("silver_ingot");
    public static final DeferredItem<Item> SILVER_SLURRY = registerOres("silver_slurry");
    public static final DeferredItem<Item> SILVER_SOLUTION = registerOres("silver_solution");
    public static final DeferredItem<Item> SODALITE = registerOres("sodalite");
    public static final DeferredItem<Item> SODALITE_FLAKES = registerOres("sodalite_flakes");
    public static final DeferredItem<Item> SODALITE_SLURRY = registerOres("sodalite_slurry");
    public static final DeferredItem<Item> SODALITE_SOLUTION = registerOres("sodalite_solution");
    public static final DeferredItem<Item> SOLUTION = registerOres("solution");
    public static final DeferredItem<Item> SULFUR_DUST = registerOres("sulfur_dust");
    public static final DeferredItem<Item> SULFUR_FLAKES = registerOres("sulfur_flakes");
    public static final DeferredItem<Item> SULFUR_SLURRY = registerOres("sulfur_slurry");
    public static final DeferredItem<Item> SULFUR_SOLUTION = registerOres("sulfur_solution");
    public static final DeferredItem<Item> SUNSTONE = registerOres("sunstone");
    public static final DeferredItem<Item> SUNSTONE_FLAKES = registerOres("sunstone_flakes");
    public static final DeferredItem<Item> SUNSTONE_SLURRY = registerOres("sunstone_slurry");
    public static final DeferredItem<Item> SUNSTONE_SOLUTION = registerOres("sunstone_solution");
    public static final DeferredItem<Item> TESLATITE_DUST = registerOres("teslatite_dust");
    public static final DeferredItem<Item> TESLATITE_FLAKES = registerOres("teslatite_flakes");
    public static final DeferredItem<Item> TESLATITE_SLURRY = registerOres("teslatite_slurry");
    public static final DeferredItem<Item> TESLATITE_SOLUTION = registerOres("teslatite_solution");
    public static final DeferredItem<Item> THORIUM_DUST = registerOres("thorium_dust");
    public static final DeferredItem<Item> THORIUM_FLAKES = registerOres("thorium_flakes");
    public static final DeferredItem<Item> THORIUM_SLURRY = registerOres("thorium_slurry");
    public static final DeferredItem<Item> THORIUM_SOLUTION = registerOres("thorium_solution");
    public static final DeferredItem<Item> TIN_FLAKES = registerOres("tin_flakes");
    public static final DeferredItem<Item> TIN_INGOT = registerOres("tin_ingot");
    public static final DeferredItem<Item> TIN_SLURRY = registerOres("tin_slurry");
    public static final DeferredItem<Item> TIN_SOLUTION = registerOres("tin_solution");
    public static final DeferredItem<Item> TITANIUM_FLAKES = registerOres("titanium_flakes");
    public static final DeferredItem<Item> TITANIUM_INGOT = registerOres("titanium_ingot");
    public static final DeferredItem<Item> TITANIUM_SLURRY = registerOres("titanium_slurry");
    public static final DeferredItem<Item> TITANIUM_SOLUTION = registerOres("titanium_solution");
    public static final DeferredItem<Item> TUNGSTEN_DUST = registerOres("tungsten_dust");
    public static final DeferredItem<Item> TUNGSTEN_FLAKES = registerOres("tungsten_flakes");
    public static final DeferredItem<Item> TUNGSTEN_SLURRY = registerOres("tungsten_slurry");
    public static final DeferredItem<Item> TUNGSTEN_SOLUTION = registerOres("tungsten_solution");
    public static final DeferredItem<Item> URANIUM_FLAKES = registerOres("uranium_flakes");
    public static final DeferredItem<Item> URANIUM_INGOT = registerOres("uranium_ingot");
    public static final DeferredItem<Item> URANIUM_SLURRY = registerOres("uranium_slurry");
    public static final DeferredItem<Item> URANIUM_SOLUTION = registerOres("uranium_solution");
    public static final DeferredItem<Item> VINTEUM = registerOres("vinteum");
    public static final DeferredItem<Item> VINTEUM_FLAKES = registerOres("vinteum_flakes");
    public static final DeferredItem<Item> VINTEUM_SLURRY = registerOres("vinteum_slurry");
    public static final DeferredItem<Item> VINTEUM_SOLUTION = registerOres("vinteum_solution");
    public static final DeferredItem<Item> WATER_INFUSED_FLAKES = registerOres("water_infused_flakes");
    public static final DeferredItem<Item> WATER_INFUSED_GEM = registerOres("water_infused_gem");
    public static final DeferredItem<Item> WATER_INFUSED_SLURRY = registerOres("water_infused_slurry");
    public static final DeferredItem<Item> WATER_INFUSED_SOLUTION = registerOres("water_infused_solution");
    public static final DeferredItem<Item> ZINC_FLAKES = registerOres("zinc_flakes");
    public static final DeferredItem<Item> ZINC_INGOT = registerOres("zinc_ingot");
    public static final DeferredItem<Item> ZINC_SLURRY = registerOres("zinc_slurry");
    public static final DeferredItem<Item> ZINC_SOLUTION = registerOres("zinc_solution");

    /* --------------------------------- */

    public static Item.Properties defaultProps() {
        return new Item.Properties();
    }

    public static Item.Properties toolProps() {
        return defaultProps()
                .stacksTo(1);
    }

    public static Item.Properties hslaSteelToolProps() {
        return defaultProps()
                .stacksTo(1)
                .rarity(Rarity.RARE);
    }

    public static Item.Properties bedrockToolProps() {
        return defaultProps()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                .fireResistant();
    }

    public static <T extends Item> DeferredItem<T> registerItems(final String name, final Supplier<T> sup) {
        return ITEMS.register(name, sup);
    }

    public static <T extends Item> DeferredItem<T> registerOres(final String name, final Supplier<T> sup) {
        return ORES.register(name, sup);
    }

    public static <T extends Item> DeferredItem<T> registerTools(final String name, final Supplier<T> sup) {
        return TOOLS.register(name, sup);
    }

    public static <T extends Item> DeferredItem<T> registerEnchantedTools(final String name, final Supplier<T> sup) {
        return ENCHANTED_TOOLS.register(name, sup);
    }

    private static DeferredItem<Item> registerTools(final String name) {
        return registerTools(name, () -> new Item(RCItems.toolProps()));
    }

    private static DeferredItem<Item> registerItems(final String name) {
        return registerItems(name, () -> new Item(RCItems.defaultProps()));
    }

    private static DeferredItem<Item> registerOres(final String name) {
        return registerOres(name, () -> new Item(RCItems.defaultProps()));
    }

    private static DeferredItem<Item> registerEnchantedTools(final String name) {
        return registerTools(name, () -> new Item(RCItems.defaultProps()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        TOOLS.register(eventBus);
        ORES.register(eventBus);
        ENCHANTED_TOOLS.register(eventBus);
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
