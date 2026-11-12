/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.neoforge.registries.DeferredRegister;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;
import reika.rotarycraft.content.blockentity.ClutchBlockEntity;
import reika.rotarycraft.content.blockentity.DynamometerBlockEntity;
import reika.rotarycraft.content.blockentity.FlywheelBlockEntity;
import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.blockentity.ShaftBlockEntity;
import reika.rotarycraft.content.blockentity.SplitterBlockEntity;
import reika.rotarycraft.content.blockentity.engine.ACEngineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.DCEngineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.GasEngineBlockEntity;
import reika.rotarycraft.content.blockentity.BedrockBreakerBlockEntity;
import reika.rotarycraft.content.blockentity.BlastFurnaceBlockEntity;
import reika.rotarycraft.content.blockentity.EngineControllerBlockEntity;
import reika.rotarycraft.content.blockentity.ExtractorBlockEntity;
import reika.rotarycraft.content.blockentity.FrictionHeaterBlockEntity;
import reika.rotarycraft.content.blockentity.WorktableBlockEntity;
import reika.rotarycraft.content.blockentity.engine.HydroEngineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.JetEngineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.MicroturbineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.PerformanceEngineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.SteamEngineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.WindEngineBlockEntity;

public final class RotaryBlockEntities {

	public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RotaryCraft.MOD_ID);

	public static final Supplier<BlockEntityType<ShaftBlockEntity>> SHAFT = type("shaft", ShaftBlockEntity::new, () -> RotaryBlocks.SHAFT.get());
	public static final Supplier<BlockEntityType<GearboxBlockEntity>> GEARBOX = type("gearbox", GearboxBlockEntity::new, () -> RotaryBlocks.GEARBOX.get());
	public static final Supplier<BlockEntityType<FlywheelBlockEntity>> FLYWHEEL = type("flywheel", FlywheelBlockEntity::new, () -> RotaryBlocks.FLYWHEEL.get());
	public static final Supplier<BlockEntityType<BevelGearBlockEntity>> BEVEL_GEARS = type("bevel_gears", BevelGearBlockEntity::new, () -> RotaryBlocks.BEVEL_GEARS.get());
	public static final Supplier<BlockEntityType<ClutchBlockEntity>> CLUTCH = type("clutch", ClutchBlockEntity::new, () -> RotaryBlocks.CLUTCH.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.MirrorBlockEntity>> MIRROR = type("solar_mirror",
			reika.rotarycraft.content.blockentity.MirrorBlockEntity::new, () -> RotaryBlocks.MIRROR.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.SolarTowerBlockEntity>> SOLAR_TOWER = type("solar_tower",
			reika.rotarycraft.content.blockentity.SolarTowerBlockEntity::new, () -> RotaryBlocks.SOLAR_TOWER.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.VanDeGraffBlockEntity>> VAN_DE_GRAFF = type("van_de_graff",
			reika.rotarycraft.content.blockentity.VanDeGraffBlockEntity::new, () -> RotaryBlocks.VAN_DE_GRAFF.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.DryingBedBlockEntity>> DRYING_BED = type("drying_bed",
			reika.rotarycraft.content.blockentity.DryingBedBlockEntity::new, () -> RotaryBlocks.DRYING_BED.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.WinderBlockEntity>> WINDER = type("winder",
			reika.rotarycraft.content.blockentity.WinderBlockEntity::new, () -> RotaryBlocks.WINDER.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.FillingStationBlockEntity>> FILLING_STATION = type("filling_station",
			reika.rotarycraft.content.blockentity.FillingStationBlockEntity::new, () -> RotaryBlocks.FILLING_STATION.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.CompactorBlockEntity>> COMPACTOR = type("compactor",
			reika.rotarycraft.content.blockentity.CompactorBlockEntity::new, () -> RotaryBlocks.COMPACTOR.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.HeatRayBlockEntity>> HEAT_RAY = type("heat_ray",
			reika.rotarycraft.content.blockentity.HeatRayBlockEntity::new, () -> RotaryBlocks.HEAT_RAY.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.RefrigeratorBlockEntity>> REFRIGERATOR = type("refrigerator",
			reika.rotarycraft.content.blockentity.RefrigeratorBlockEntity::new, () -> RotaryBlocks.REFRIGERATOR.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.PipePumpBlockEntity>> PIPE_PUMP = type("pipe_pump",
			reika.rotarycraft.content.blockentity.PipePumpBlockEntity::new, () -> RotaryBlocks.PIPE_PUMP.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.FuelEnhancerBlockEntity>> FUEL_ENHANCER = type("fuel_enhancer",
			reika.rotarycraft.content.blockentity.FuelEnhancerBlockEntity::new, () -> RotaryBlocks.FUEL_ENHANCER.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.CoolingFinBlockEntity>> COOLING_FIN = type("cooling_fin",
			reika.rotarycraft.content.blockentity.CoolingFinBlockEntity::new, () -> RotaryBlocks.COOLING_FIN.get());
	public static final Supplier<BlockEntityType<DynamometerBlockEntity>> DYNAMOMETER = type("dynamometer", DynamometerBlockEntity::new, () -> RotaryBlocks.DYNAMOMETER.get());
	public static final Supplier<BlockEntityType<SplitterBlockEntity>> SHAFT_JUNCTION = type("shaft_junction", SplitterBlockEntity::new, () -> RotaryBlocks.SHAFT_JUNCTION.get());

	public static final Supplier<BlockEntityType<DCEngineBlockEntity>> DC_ENGINE = type("dc_engine", DCEngineBlockEntity::new, () -> RotaryBlocks.DC_ENGINE.get());
	public static final Supplier<BlockEntityType<WindEngineBlockEntity>> WIND_ENGINE = type("wind_engine", WindEngineBlockEntity::new, () -> RotaryBlocks.WIND_ENGINE.get());
	public static final Supplier<BlockEntityType<SteamEngineBlockEntity>> STEAM_ENGINE = type("steam_engine", SteamEngineBlockEntity::new, () -> RotaryBlocks.STEAM_ENGINE.get());
	public static final Supplier<BlockEntityType<GasEngineBlockEntity>> GAS_ENGINE = type("gas_engine", GasEngineBlockEntity::new, () -> RotaryBlocks.GAS_ENGINE.get());
	public static final Supplier<BlockEntityType<ACEngineBlockEntity>> AC_ENGINE = type("ac_engine", ACEngineBlockEntity::new, () -> RotaryBlocks.AC_ENGINE.get());
	public static final Supplier<BlockEntityType<PerformanceEngineBlockEntity>> SPORT_ENGINE = type("sport_engine", PerformanceEngineBlockEntity::new, () -> RotaryBlocks.SPORT_ENGINE.get());
	public static final Supplier<BlockEntityType<MicroturbineBlockEntity>> MICRO_ENGINE = type("micro_engine", MicroturbineBlockEntity::new, () -> RotaryBlocks.MICRO_ENGINE.get());

	public static final Supplier<BlockEntityType<HydroEngineBlockEntity>> HYDRO_ENGINE = type("hydro_engine", HydroEngineBlockEntity::new, () -> RotaryBlocks.HYDRO_ENGINE.get());
	public static final Supplier<BlockEntityType<JetEngineBlockEntity>> JET_ENGINE = type("jet_engine", JetEngineBlockEntity::new, () -> RotaryBlocks.JET_ENGINE.get());

	public static final Supplier<BlockEntityType<EngineControllerBlockEntity>> ECU = type("ecu", EngineControllerBlockEntity::new, () -> RotaryBlocks.ECU.get());

	public static final Supplier<BlockEntityType<BlastFurnaceBlockEntity>> BLAST_FURNACE = type("blast_furnace", BlastFurnaceBlockEntity::new, () -> RotaryBlocks.BLAST_FURNACE.get());
	public static final Supplier<BlockEntityType<WorktableBlockEntity>> WORKTABLE = type("worktable", WorktableBlockEntity::new, () -> RotaryBlocks.WORKTABLE.get());

	public static final Supplier<BlockEntityType<ExtractorBlockEntity>> EXTRACTOR = type("extractor", ExtractorBlockEntity::new, () -> RotaryBlocks.EXTRACTOR.get());
	public static final Supplier<BlockEntityType<BedrockBreakerBlockEntity>> BEDROCK_BREAKER = type("bedrock_breaker", BedrockBreakerBlockEntity::new, () -> RotaryBlocks.BEDROCK_BREAKER.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.BorerBlockEntity>> BORER = type("borer", reika.rotarycraft.content.blockentity.BorerBlockEntity::new, () -> RotaryBlocks.BORER.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.GrinderBlockEntity>> GRINDER = type("grinder", reika.rotarycraft.content.blockentity.GrinderBlockEntity::new, () -> RotaryBlocks.GRINDER.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.FermenterBlockEntity>> FERMENTER = type("fermenter", reika.rotarycraft.content.blockentity.FermenterBlockEntity::new, () -> RotaryBlocks.FERMENTER.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.FanBlockEntity>> FAN = type("fan", reika.rotarycraft.content.blockentity.FanBlockEntity::new, () -> RotaryBlocks.FAN.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.MobHarvesterBlockEntity>> MOB_HARVESTER = type("mob_harvester", reika.rotarycraft.content.blockentity.MobHarvesterBlockEntity::new, () -> RotaryBlocks.MOB_HARVESTER.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.AggregatorBlockEntity>> AGGREGATOR = type("aggregator", reika.rotarycraft.content.blockentity.AggregatorBlockEntity::new, () -> RotaryBlocks.AGGREGATOR.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.FlooderBlockEntity>> SPILLER = type("spiller", reika.rotarycraft.content.blockentity.FlooderBlockEntity::new, () -> RotaryBlocks.SPILLER.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.BlockFillerBlockEntity>> FILLER = type("filler", reika.rotarycraft.content.blockentity.BlockFillerBlockEntity::new, () -> RotaryBlocks.FILLER.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.SmokeDetectorBlockEntity>> SMOKE_DETECTOR = type("smoke_detector", reika.rotarycraft.content.blockentity.SmokeDetectorBlockEntity::new, () -> RotaryBlocks.SMOKE_DETECTOR.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.PlayerDetectorBlockEntity>> PLAYER_DETECTOR = type("player_detector", reika.rotarycraft.content.blockentity.PlayerDetectorBlockEntity::new, () -> RotaryBlocks.PLAYER_DETECTOR.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.MobRadarBlockEntity>> MOB_RADAR = type("mob_radar", reika.rotarycraft.content.blockentity.MobRadarBlockEntity::new, () -> RotaryBlocks.MOB_RADAR.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.LavaMakerBlockEntity>> LAVA_MAKER = type("lava_maker", reika.rotarycraft.content.blockentity.LavaMakerBlockEntity::new, () -> RotaryBlocks.LAVA_MAKER.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.HeaterBlockEntity>> HEATER = type("heater", reika.rotarycraft.content.blockentity.HeaterBlockEntity::new, () -> RotaryBlocks.HEATER.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.ComposterBlockEntity>> COMPOSTER = type("composter", reika.rotarycraft.content.blockentity.ComposterBlockEntity::new, () -> RotaryBlocks.COMPOSTER.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.CrystallizerBlockEntity>> CRYSTALLIZER = type("crystallizer", reika.rotarycraft.content.blockentity.CrystallizerBlockEntity::new, () -> RotaryBlocks.CRYSTALLIZER.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.LampBlockEntity>> LAMP = type("lamp", reika.rotarycraft.content.blockentity.LampBlockEntity::new, () -> RotaryBlocks.LAMP.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.GroundHydratorBlockEntity>> HYDRATOR = type("hydrator", reika.rotarycraft.content.blockentity.GroundHydratorBlockEntity::new, () -> RotaryBlocks.HYDRATOR.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.WetterBlockEntity>> WETTER = type("wetter", reika.rotarycraft.content.blockentity.WetterBlockEntity::new, () -> RotaryBlocks.WETTER.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.CentrifugeBlockEntity>> CENTRIFUGE = type("centrifuge", reika.rotarycraft.content.blockentity.CentrifugeBlockEntity::new, () -> RotaryBlocks.CENTRIFUGE.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.FractionatorBlockEntity>> FRACTIONATOR = type("fractionator", reika.rotarycraft.content.blockentity.FractionatorBlockEntity::new, () -> RotaryBlocks.FRACTIONATOR.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.PumpBlockEntity>> PUMP = type("pump", reika.rotarycraft.content.blockentity.PumpBlockEntity::new, () -> RotaryBlocks.PUMP.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.ReservoirBlockEntity>> RESERVOIR = type("reservoir", reika.rotarycraft.content.blockentity.ReservoirBlockEntity::new, () -> RotaryBlocks.RESERVOIR.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.blockentity.PulseJetFurnaceBlockEntity>> PULSE_JET = type("pulse_jet_furnace", reika.rotarycraft.content.blockentity.PulseJetFurnaceBlockEntity::new, () -> RotaryBlocks.PULSE_JET.get());
	public static final Supplier<BlockEntityType<FrictionHeaterBlockEntity>> FRICTION_HEATER = type("friction_heater", FrictionHeaterBlockEntity::new, () -> RotaryBlocks.FRICTION_HEATER.get());

	public static final Supplier<BlockEntityType<reika.rotarycraft.content.piping.HoseBlockEntity>> HOSE = type("hose", reika.rotarycraft.content.piping.HoseBlockEntity::new, () -> RotaryBlocks.HOSE.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.piping.PipeBlockEntity>> PIPE = type("pipe", reika.rotarycraft.content.piping.PipeBlockEntity::new, () -> RotaryBlocks.PIPE.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.piping.FuelLineBlockEntity>> FUEL_LINE = type("fuel_line", reika.rotarycraft.content.piping.FuelLineBlockEntity::new, () -> RotaryBlocks.FUEL_LINE.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.piping.BedrockPipeBlockEntity>> BEDROCK_PIPE = type("bedrock_pipe", reika.rotarycraft.content.piping.BedrockPipeBlockEntity::new, () -> RotaryBlocks.BEDROCK_PIPE.get());

	public static final Supplier<BlockEntityType<reika.rotarycraft.content.piping.FlowPipeBlockEntity>> VALVE = type("valve", reika.rotarycraft.content.piping.FlowPipeBlockEntity::valve, () -> RotaryBlocks.VALVE.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.piping.FlowPipeBlockEntity>> BYPASS = type("bypass", reika.rotarycraft.content.piping.FlowPipeBlockEntity::bypass, () -> RotaryBlocks.BYPASS.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.piping.FlowPipeBlockEntity>> SEPARATION = type("separation", reika.rotarycraft.content.piping.FlowPipeBlockEntity::separation, () -> RotaryBlocks.SEPARATION.get());
	public static final Supplier<BlockEntityType<reika.rotarycraft.content.piping.FlowPipeBlockEntity>> SUCTION = type("suction", reika.rotarycraft.content.piping.FlowPipeBlockEntity::suction, () -> RotaryBlocks.SUCTION.get());

	@SuppressWarnings("unchecked")
	public static java.util.List<Supplier<? extends BlockEntityType<? extends EngineBlockEntity>>> engines() {
		return java.util.List.of(DC_ENGINE, WIND_ENGINE, STEAM_ENGINE, GAS_ENGINE, AC_ENGINE, SPORT_ENGINE, MICRO_ENGINE, HYDRO_ENGINE, JET_ENGINE);
	}

	private RotaryBlockEntities() {}

	@SuppressWarnings("DataFlowIssue")
	private static <T extends net.minecraft.world.level.block.entity.BlockEntity> Supplier<BlockEntityType<T>> type(String name, BlockEntityType.BlockEntitySupplier<T> factory, Supplier<Block> block) {
		return TYPES.register(name, () -> BlockEntityType.Builder.of(factory, block.get()).build(null));
	}
}
