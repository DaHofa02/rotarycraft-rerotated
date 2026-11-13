/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.registry;

import java.util.function.Function;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.block.BlastFurnaceBlock;
import reika.rotarycraft.content.block.BedrockBreakerBlock;
import reika.rotarycraft.content.block.BedrockSliceBlock;
import reika.rotarycraft.content.block.EcuBlock;
import reika.rotarycraft.content.block.ExtractorBlock;
import reika.rotarycraft.content.block.FrictionHeaterBlock;
import reika.rotarycraft.content.block.WorktableBlock;
import reika.rotarycraft.content.block.EngineBlock;
import reika.rotarycraft.content.block.FlammableBlock;
import reika.rotarycraft.content.block.FlywheelBlock;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.block.MachineBlock;
import reika.rotarycraft.content.block.MachineBlock.Placement;
import reika.rotarycraft.content.block.ShaftBlock;
import reika.rotarycraft.content.block.PipingBlock;
import reika.rotarycraft.content.item.MachineBlockItem;
import reika.rotarycraft.lib.LegacyResistance;

/** Original: MachineRegistry entries, one block per machine instead of metadata-shared blocks. */
public final class RotaryBlocks {

	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RotaryCraft.MOD_ID);

	/**
	 * Original BlockBasicMachine/BlockRotaryCraftMachine: hardness 4, setResistance(15) (= blast resistance 9, see LegacyResistance),
	 * Material.iron (metal sound, needs a tool).
	 */
	public static BlockBehaviour.Properties machineProperties() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4F, LegacyResistance.of(15F)).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion();
	}

	public static final DeferredBlock<ShaftBlock> SHAFT = machine("shaft", ShaftBlock::new);
	public static final DeferredBlock<GearboxBlock> GEARBOX = machine("gearbox", GearboxBlock::new);
	public static final DeferredBlock<FlywheelBlock> FLYWHEEL = machine("flywheel", FlywheelBlock::new);
	public static final DeferredBlock<MachineBlock> BEVEL_GEARS = machine("bevel_gears", p -> new MachineBlock(p, RotaryBlockEntities.BEVEL_GEARS, Placement.NONE, true));
	public static final DeferredBlock<MachineBlock> CLUTCH = machine("clutch", p -> new MachineBlock(p, RotaryBlockEntities.CLUTCH, Placement.SIX_SIDED, true));
	/** Original MachineRegistry.FUELENHANCER (BlockMMachine, no orientation). */
	public static final DeferredBlock<MachineBlock> FUEL_ENHANCER = machine("fuel_enhancer", p -> new MachineBlock(p, RotaryBlockEntities.FUEL_ENHANCER, Placement.NONE, false));
	/** Original MachineRegistry.COOLINGFIN (side placed, 6 orientations, BlockDMMachine). */
	public static final DeferredBlock<MachineBlock> COOLING_FIN = machine("cooling_fin", p -> new MachineBlock(p, RotaryBlockEntities.COOLING_FIN, Placement.SIDE_ATTACHED, false));
	/** Original MachineRegistry.MIRROR and SOLARTOWER (BlockSolar, no orientation). */
	public static final DeferredBlock<reika.rotarycraft.content.block.MirrorBlock> MIRROR = machine("solar_mirror", reika.rotarycraft.content.block.MirrorBlock::new);
	public static final DeferredBlock<MachineBlock> SOLAR_TOWER = machine("solar_tower", p -> new MachineBlock(p, RotaryBlockEntities.SOLAR_TOWER, Placement.NONE, false));
	/** Original MachineRegistry.PIPEPUMP (BlockDMMachine, 6 orientations). */
	public static final DeferredBlock<MachineBlock> PIPE_PUMP = machine("pipe_pump", p -> new MachineBlock(p, RotaryBlockEntities.PIPE_PUMP, Placement.SIX_SIDED, false));
	/** Original MachineRegistry.VANDEGRAFF (BlockMMachine, no orientation). */
	public static final DeferredBlock<MachineBlock> VAN_DE_GRAFF = machine("van_de_graff", p -> new MachineBlock(p, RotaryBlockEntities.VAN_DE_GRAFF, Placement.NONE, false));
	/** Original MachineRegistry.DRYING (BlockMIMachine). */
	public static final DeferredBlock<reika.rotarycraft.content.block.DryingBedBlock> DRYING_BED = machine("drying_bed", reika.rotarycraft.content.block.DryingBedBlock::new);
	/** Original MachineRegistry.WINDER (BlockDMIMachine, 4 orientations, GUI). */
	public static final DeferredBlock<reika.rotarycraft.content.block.WinderBlock> WINDER = machine("winder", reika.rotarycraft.content.block.WinderBlock::new);
	/** Original MachineRegistry.FILLINGSTATION (BlockDMIMachine, 4 orientations, GUI). */
	public static final DeferredBlock<reika.rotarycraft.content.block.FillingStationBlock> FILLING_STATION = machine("filling_station", reika.rotarycraft.content.block.FillingStationBlock::new);
	/** Original MachineRegistry.COMPACTOR (BlockDMIMachine, 4 orientations, GUI). */
	public static final DeferredBlock<reika.rotarycraft.content.block.CompactorBlock> COMPACTOR = machine("compactor", reika.rotarycraft.content.block.CompactorBlock::new);
	/** Original MachineRegistry.HEATRAY (BlockDMMachine, 4 orientations, z-flipped). */
	public static final DeferredBlock<reika.rotarycraft.content.block.HeatRayBlock> HEAT_RAY = machine("heat_ray", reika.rotarycraft.content.block.HeatRayBlock::new);
	/** Original MachineRegistry.REFRIGERATOR (BlockDMIMachine, 4 orientations, GUI). */
	public static final DeferredBlock<reika.rotarycraft.content.block.RefrigeratorBlock> REFRIGERATOR = machine("refrigerator", reika.rotarycraft.content.block.RefrigeratorBlock::new);
	public static final DeferredBlock<MachineBlock> DYNAMOMETER = machine("dynamometer", p -> new MachineBlock(p, RotaryBlockEntities.DYNAMOMETER, Placement.FOUR_SIDED, true));
	public static final DeferredBlock<MachineBlock> SHAFT_JUNCTION = machine("shaft_junction", p -> new MachineBlock(p, RotaryBlockEntities.SHAFT_JUNCTION, Placement.FOUR_SIDED, false));

	public static final DeferredBlock<EngineBlock> DC_ENGINE = machine("dc_engine", p -> new EngineBlock(p, RotaryBlockEntities.DC_ENGINE, EngineType.DC));
	public static final DeferredBlock<EngineBlock> WIND_ENGINE = machine("wind_engine", p -> new EngineBlock(p, RotaryBlockEntities.WIND_ENGINE, EngineType.WIND));
	public static final DeferredBlock<EngineBlock> STEAM_ENGINE = machine("steam_engine", p -> new EngineBlock(p, RotaryBlockEntities.STEAM_ENGINE, EngineType.STEAM));
	public static final DeferredBlock<EngineBlock> GAS_ENGINE = machine("gas_engine", p -> new EngineBlock(p, RotaryBlockEntities.GAS_ENGINE, EngineType.GAS));
	public static final DeferredBlock<EngineBlock> AC_ENGINE = machine("ac_engine", p -> new EngineBlock(p, RotaryBlockEntities.AC_ENGINE, EngineType.AC));
	public static final DeferredBlock<EngineBlock> SPORT_ENGINE = machine("sport_engine", p -> new EngineBlock(p, RotaryBlockEntities.SPORT_ENGINE, EngineType.SPORT));
	public static final DeferredBlock<EngineBlock> MICRO_ENGINE = machine("micro_engine", p -> new EngineBlock(p, RotaryBlockEntities.MICRO_ENGINE, EngineType.MICRO));

	public static final DeferredBlock<EngineBlock> HYDRO_ENGINE = machine("hydro_engine", p -> new EngineBlock(p, RotaryBlockEntities.HYDRO_ENGINE, EngineType.HYDRO));
	public static final DeferredBlock<EngineBlock> JET_ENGINE = machine("jet_engine", p -> new EngineBlock(p, RotaryBlockEntities.JET_ENGINE, EngineType.JET));

	/** Original: MachineRegistry.ECU (BlockMachine). */
	public static final DeferredBlock<EcuBlock> ECU = machine("ecu", EcuBlock::new);

	/** Original: MachineRegistry.BLASTFURNACE / WORKTABLE (BlockIMachine). */
	public static final DeferredBlock<BlastFurnaceBlock> BLAST_FURNACE = machine("blast_furnace", BlastFurnaceBlock::new);
	public static final DeferredBlock<WorktableBlock> WORKTABLE = machine("worktable", WorktableBlock::new);
	/** Original: MachineRegistry.FRICTION (BlockDMMachine). */
	public static final DeferredBlock<FrictionHeaterBlock> FRICTION_HEATER = machine("friction_heater", FrictionHeaterBlock::new);
	/** Original: MachineRegistry.FERMENTER (BlockIMachine) and CENTRIFUGE (BlockMIMachine). */
	public static final DeferredBlock<reika.rotarycraft.content.block.FermenterBlock> FERMENTER = machine("fermenter", reika.rotarycraft.content.block.FermenterBlock::new);
	/** Original: MachineRegistry.FAN (BlockDMMachine, 6 orientations, z-flipped). */
	public static final DeferredBlock<reika.rotarycraft.content.block.FanBlock> FAN = machine("fan", reika.rotarycraft.content.block.FanBlock::new);
	/** Original: MachineRegistry.MOBHARVESTER (BlockMMachine, no orientation). */
	public static final DeferredBlock<MachineBlock> MOB_HARVESTER = machine("mob_harvester", p -> new MachineBlock(p, RotaryBlockEntities.MOB_HARVESTER, Placement.NONE, false));
	/** Original: MachineRegistry.AGGREGATOR (BlockMMachine, no orientation). */
	public static final DeferredBlock<MachineBlock> AGGREGATOR = machine("aggregator", p -> new MachineBlock(p, RotaryBlockEntities.AGGREGATOR, Placement.NONE, false));
	/** Original: MachineRegistry.SPILLER (BlockMachine, a plain cube: steel with the spiller grate below). */
	public static final DeferredBlock<MachineBlock> SPILLER = machine("spiller", p -> new MachineBlock(p, RotaryBlockEntities.SPILLER, Placement.NONE, false));
	/** Original: MachineRegistry.FILLER (BlockMachine, a plain cube: steel with the filler grate below). */
	public static final DeferredBlock<MachineBlock> FILLER = machine("filler", p -> new MachineBlock(p, RotaryBlockEntities.FILLER, Placement.NONE, false));
	/** Original: MachineRegistry.SMOKEDETECTOR (BlockMIMachine). */
	public static final DeferredBlock<reika.rotarycraft.content.block.SmokeDetectorBlock> SMOKE_DETECTOR = machine("smoke_detector", reika.rotarycraft.content.block.SmokeDetectorBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.PlayerDetectorBlock> PLAYER_DETECTOR = machine("player_detector", reika.rotarycraft.content.block.PlayerDetectorBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.MobRadarBlock> MOB_RADAR = machine("mob_radar", reika.rotarycraft.content.block.MobRadarBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.LavaMakerBlock> LAVA_MAKER = machine("lava_maker", reika.rotarycraft.content.block.LavaMakerBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.HeaterBlock> HEATER = machine("heater", reika.rotarycraft.content.block.HeaterBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.ComposterBlock> COMPOSTER = machine("composter", reika.rotarycraft.content.block.ComposterBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.CrystallizerBlock> CRYSTALLIZER = machine("crystallizer", reika.rotarycraft.content.block.CrystallizerBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.LampBlock> LAMP = machine("lamp", reika.rotarycraft.content.block.LampBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.GroundHydratorBlock> HYDRATOR = machine("hydrator", reika.rotarycraft.content.block.GroundHydratorBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.WetterBlock> WETTER = machine("wetter", reika.rotarycraft.content.block.WetterBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.ParticleEmitterBlock> PARTICLE_EMITTER = machine("particle_emitter", reika.rotarycraft.content.block.ParticleEmitterBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.MultiClutchBlock> MULTI_CLUTCH = machine("multi_clutch", reika.rotarycraft.content.block.MultiClutchBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.BigFurnaceBlock> BIG_FURNACE = machine("big_furnace", reika.rotarycraft.content.block.BigFurnaceBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.SpillwayBlock> SPILLWAY = machine("spillway", reika.rotarycraft.content.block.SpillwayBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.FluidCompressorBlock> FLUID_COMPRESSOR = machine("fluid_compressor", reika.rotarycraft.content.block.FluidCompressorBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.LineBuilderBlock> LINE_BUILDER = machine("line_builder", reika.rotarycraft.content.block.LineBuilderBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.MagnetizerBlock> MAGNETIZER = machine("magnetizer", reika.rotarycraft.content.block.MagnetizerBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.BeamMirrorBlock> BEAM_MIRROR = machine("beam_mirror", reika.rotarycraft.content.block.BeamMirrorBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.ItemRefresherBlock> ITEM_REFRESHER = machine("item_refresher", reika.rotarycraft.content.block.ItemRefresherBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.GrindstoneBlock> GRINDSTONE = machine("grindstone", reika.rotarycraft.content.block.GrindstoneBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.BucketFillerBlock> BUCKET_FILLER = machine("bucket_filler", reika.rotarycraft.content.block.BucketFillerBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.SortingBlock> SORTING = machine("sorting", reika.rotarycraft.content.block.SortingBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.CentrifugeBlock> CENTRIFUGE = machine("centrifuge", reika.rotarycraft.content.block.CentrifugeBlock::new);
	/** Original: MachineRegistry.FRACTIONATOR (BlockMIMachine). */
	public static final DeferredBlock<reika.rotarycraft.content.block.FractionatorBlock> FRACTIONATOR = machine("fractionator", reika.rotarycraft.content.block.FractionatorBlock::new);
	/** Original: MachineRegistry.PUMP (BlockDMMachine). */
	public static final DeferredBlock<reika.rotarycraft.content.block.PumpBlock> PUMP = machine("pump", reika.rotarycraft.content.block.PumpBlock::new);
	/** Original: MachineRegistry.RESERVOIR (BlockMachine). */
	public static final DeferredBlock<reika.rotarycraft.content.block.ReservoirBlock> RESERVOIR = machine("reservoir", reika.rotarycraft.content.block.ReservoirBlock::new);
	/** Original: MachineRegistry.PULSEJET (BlockMIMachine) and BlockRegistry.BLASTGLASS. */
	public static final DeferredBlock<reika.rotarycraft.content.block.PulseJetFurnaceBlock> PULSE_JET = machine("pulse_jet_furnace", reika.rotarycraft.content.block.PulseJetFurnaceBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.BlastGlassBlock> BLAST_GLASS = BLOCKS.register("blast_glass", () -> new reika.rotarycraft.content.block.BlastGlassBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(10F, 6000F).sound(SoundType.GLASS).requiresCorrectToolForDrops().noOcclusion()
			.isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));
	public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem> BLAST_GLASS_ITEM = RotaryItems.ITEMS.registerSimpleBlockItem("blast_glass", BLAST_GLASS);

	/**
	 * Original: BlockRegistry.DECO (BlockDeco, metadata 0-5, RotaryNames.blockNames): HSLA steel, anthracite, lonsdaleite, borer shield,
	 * bedrock ingot and coal coke storage blocks. Material.iron (any pickaxe, metal sound), hardness 4, setResistance(10) = blast
	 * resistance 6 (the bedrock ingot block: 6000, and the wither and the dragon cannot break it - tags wither_immune/dragon_immune).
	 * Anthracite and coke burn (FlammableBlock). ReactorCraft uses the steel and bedrock ingot blocks as radiation shields
	 * (c:storage_blocks/steel, c:storage_blocks/bedrock) and the steel block in its recipes.
	 * Not ported: BlockDeco.onBlockExploded put the bedrock ingot block back after an explosion; with resistance 6000 no explosion short
	 * of power ~1800 reaches it anyway.
	 */
	public static final DeferredBlock<Block> STEEL_BLOCK = deco("steel_block", decoProperties(LegacyResistance.of(10F)));
	public static final DeferredBlock<FlammableBlock> ANTHRACITE_BLOCK = flammableDeco("anthracite_block", 30, 4);
	public static final DeferredBlock<Block> LONSDALEITE_BLOCK = deco("lonsdaleite_block", decoProperties(LegacyResistance.of(10F)));
	public static final DeferredBlock<Block> SHIELD_BLOCK = deco("shield_block", decoProperties(LegacyResistance.of(10F)));
	public static final DeferredBlock<Block> BEDROCK_INGOT_BLOCK = deco("bedrock_ingot_block", decoProperties(6000F));
	public static final DeferredBlock<FlammableBlock> COKE_BLOCK = flammableDeco("coke_block", 20, 3);
	/** Original: MachineRegistry.GRINDER (BlockDMIMachine). */
	public static final DeferredBlock<reika.rotarycraft.content.block.GrinderBlock> GRINDER = machine("grinder", reika.rotarycraft.content.block.GrinderBlock::new);
	/** Original: MachineRegistry.EXTRACTOR (BlockMIMachine). */
	public static final DeferredBlock<ExtractorBlock> EXTRACTOR = machine("extractor", ExtractorBlock::new);
	/** Original: MachineRegistry.BEDROCKBREAKER (BlockDMMachine) and BlockRegistry.BEDROCKSLICE (no item). */
	public static final DeferredBlock<BedrockBreakerBlock> BEDROCK_BREAKER = machine("bedrock_breaker", BedrockBreakerBlock::new);
	public static final DeferredBlock<BedrockSliceBlock> BEDROCK_SLICE = BLOCKS.register("bedrock_slice", () -> new BedrockSliceBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(-1F, 3600000F).noLootTable().noOcclusion().isValidSpawn((s, l, p, e) -> false)
			.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));
	/** Original: MachineRegistry.BORER (BlockDMachine) and BlockRegistry.MININGPIPE (Material.iron, hardness 0, resistance 0, no item). */
	public static final DeferredBlock<reika.rotarycraft.content.block.BorerBlock> BORER = machine("borer", reika.rotarycraft.content.block.BorerBlock::new);
	public static final DeferredBlock<reika.rotarycraft.content.block.MiningPipeBlock> MINING_PIPE = BLOCKS.register("mining_pipe", () -> new reika.rotarycraft.content.block.MiningPipeBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0F, 0F).sound(SoundType.METAL).noLootTable().noOcclusion().isValidSpawn((s, l, p, e) -> false)));

	/** Original: BlockRegistry.CANOLA (BlockCanola, Material.plants, grass sound, no item: planted from the seeds). */
	public static final DeferredBlock<reika.rotarycraft.content.block.CanolaBlock> CANOLA = BLOCKS.register("canola", () -> new reika.rotarycraft.content.block.CanolaBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak().randomTicks().sound(SoundType.GRASS)
			.pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

	/** Original: BlockPiping (MachineRegistry.HOSE, PIPE, FUELLINE, BEDPIPE): Material.iron (metal sound, also the hose), hardness from the config, setResistance(1). */
	public static final DeferredBlock<PipingBlock> HOSE = pipe("hose", RotaryBlockEntities.HOSE, true);
	public static final DeferredBlock<PipingBlock> PIPE = pipe("pipe", RotaryBlockEntities.PIPE, false);
	public static final DeferredBlock<PipingBlock> FUEL_LINE = pipe("fuel_line", RotaryBlockEntities.FUEL_LINE, false);
	public static final DeferredBlock<PipingBlock> BEDROCK_PIPE = pipe("bedrock_pipe", RotaryBlockEntities.BEDROCK_PIPE, false);
	public static final DeferredBlock<PipingBlock> VALVE = pipe("valve", RotaryBlockEntities.VALVE, false);
	public static final DeferredBlock<PipingBlock> BYPASS = pipe("bypass", RotaryBlockEntities.BYPASS, false);
	public static final DeferredBlock<PipingBlock> SEPARATION = pipe("separation", RotaryBlockEntities.SEPARATION, false);
	public static final DeferredBlock<PipingBlock> SUCTION = pipe("suction", RotaryBlockEntities.SUCTION, false);

	private RotaryBlocks() {}

	private static DeferredBlock<PipingBlock> pipe(String name, java.util.function.Supplier<? extends net.minecraft.world.level.block.entity.BlockEntityType<? extends reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity>> type,
			boolean flammable) {
		return machine(name, p -> new PipingBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0F, LegacyResistance.of(1F)).sound(SoundType.METAL).noOcclusion()
				.dynamicShape(), type, flammable));
	}

	private static BlockBehaviour.Properties decoProperties(float blastResistance) {
		return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4F, blastResistance).sound(SoundType.METAL).requiresCorrectToolForDrops();
	}

	private static DeferredBlock<Block> deco(String name, BlockBehaviour.Properties props) {
		DeferredBlock<Block> block = BLOCKS.registerSimpleBlock(name, props);
		RotaryItems.ITEMS.registerSimpleBlockItem(name, block);
		return block;
	}

	private static DeferredBlock<FlammableBlock> flammableDeco(String name, int flammability, int spreadSpeed) {
		DeferredBlock<FlammableBlock> block = BLOCKS.register(name, () -> new FlammableBlock(decoProperties(LegacyResistance.of(10F)), flammability, spreadSpeed));
		RotaryItems.ITEMS.registerSimpleBlockItem(name, block);
		return block;
	}

	private static <B extends Block> DeferredBlock<B> machine(String name, Function<BlockBehaviour.Properties, B> factory) {
		DeferredBlock<B> block = BLOCKS.register(name, () -> factory.apply(machineProperties()));
		RotaryItems.ITEMS.register(name, () -> new MachineBlockItem(block.get(), new Item.Properties()));
		return block;
	}
}
