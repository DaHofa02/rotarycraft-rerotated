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
import reika.rotarycraft.content.block.FlywheelBlock;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.block.MachineBlock;
import reika.rotarycraft.content.block.MachineBlock.Placement;
import reika.rotarycraft.content.block.ShaftBlock;
import reika.rotarycraft.content.block.PipingBlock;
import reika.rotarycraft.content.item.MachineBlockItem;

/** Original: MachineRegistry entries, one block per machine instead of metadata-shared blocks. */
public final class RotaryBlocks {

	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RotaryCraft.MOD_ID);

	/** Original BlockBasicMachine/BlockRotaryCraftMachine: hardness 4, resistance 15, Material.iron (metal sound, needs a tool). */
	public static BlockBehaviour.Properties machineProperties() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4F, 15F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion();
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
	/** Original: MachineRegistry.GRINDER (BlockDMIMachine). */
	public static final DeferredBlock<reika.rotarycraft.content.block.GrinderBlock> GRINDER = machine("grinder", reika.rotarycraft.content.block.GrinderBlock::new);
	/** Original: MachineRegistry.EXTRACTOR (BlockMIMachine). */
	public static final DeferredBlock<ExtractorBlock> EXTRACTOR = machine("extractor", ExtractorBlock::new);
	/** Original: MachineRegistry.BEDROCKBREAKER (BlockDMMachine) and BlockRegistry.BEDROCKSLICE (no item). */
	public static final DeferredBlock<BedrockBreakerBlock> BEDROCK_BREAKER = machine("bedrock_breaker", BedrockBreakerBlock::new);
	public static final DeferredBlock<BedrockSliceBlock> BEDROCK_SLICE = BLOCKS.register("bedrock_slice", () -> new BedrockSliceBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(-1F, 3600000F).noLootTable().noOcclusion().isValidSpawn((s, l, p, e) -> false)
			.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));

	/** Original: BlockRegistry.CANOLA (BlockCanola, Material.plants, grass sound, no item: planted from the seeds). */
	public static final DeferredBlock<reika.rotarycraft.content.block.CanolaBlock> CANOLA = BLOCKS.register("canola", () -> new reika.rotarycraft.content.block.CanolaBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak().randomTicks().sound(SoundType.GRASS)
			.pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

	/** Original: BlockPiping (MachineRegistry.HOSE, PIPE, FUELLINE, BEDPIPE): Material.iron (metal sound, also the hose), hardness from the config, resistance 1. */
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
		return machine(name, p -> new PipingBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0F, 1F).sound(SoundType.METAL).noOcclusion()
				.dynamicShape(), type, flammable));
	}

	private static <B extends Block> DeferredBlock<B> machine(String name, Function<BlockBehaviour.Properties, B> factory) {
		DeferredBlock<B> block = BLOCKS.register(name, () -> factory.apply(machineProperties()));
		RotaryItems.ITEMS.register(name, () -> new MachineBlockItem(block.get(), new Item.Properties()));
		return block;
	}
}
