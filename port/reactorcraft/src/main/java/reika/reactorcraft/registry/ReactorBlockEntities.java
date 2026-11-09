/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.neoforge.registries.DeferredRegister;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.blockentity.HeavyPumpBlockEntity;

/** Original: ReactorTiles (the machines ported so far). */
public final class ReactorBlockEntities {

	public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ReactorCraft.MOD_ID);

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<HeavyPumpBlockEntity>> HEAVY_PUMP = TYPES.register("fluid_extractor",
			() -> BlockEntityType.Builder.of(HeavyPumpBlockEntity::new, ReactorBlocks.FLUID_EXTRACTOR.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.content.blockentity.UProcessorBlockEntity>> PROCESSOR = TYPES.register("processor",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.content.blockentity.UProcessorBlockEntity::new, ReactorBlocks.PROCESSOR.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.content.blockentity.SynthesizerBlockEntity>> SYNTHESIZER = TYPES.register("synthesizer",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.content.blockentity.SynthesizerBlockEntity::new, ReactorBlocks.SYNTHESIZER.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.content.blockentity.IsoCentrifugeBlockEntity>> ISO_CENTRIFUGE = TYPES.register("isocentrifuge",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.content.blockentity.IsoCentrifugeBlockEntity::new, ReactorBlocks.ISO_CENTRIFUGE.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.reactor.FuelRodBlockEntity>> FUEL_ROD = TYPES.register("fuel_rod",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.reactor.FuelRodBlockEntity::new, ReactorBlocks.FUEL_ROD.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.reactor.WaterCellBlockEntity>> WATER_CELL = TYPES.register("coolant_cell",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.reactor.WaterCellBlockEntity::new, ReactorBlocks.WATER_CELL.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.reactor.ControlRodBlockEntity>> CONTROL_ROD = TYPES.register("control_rod",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.reactor.ControlRodBlockEntity::new, ReactorBlocks.CONTROL_ROD.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.reactor.CpuBlockEntity>> CPU = TYPES.register("cpu",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.reactor.CpuBlockEntity::new, ReactorBlocks.CPU.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.reactor.ReactorBoilerBlockEntity>> BOILER = TYPES.register("reactor_boiler",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.reactor.ReactorBoilerBlockEntity::new, ReactorBlocks.BOILER.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.powergen.SteamLineBlockEntity>> STEAM_LINE = TYPES.register("steam_line",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.powergen.SteamLineBlockEntity::new, ReactorBlocks.STEAM_LINE.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.powergen.SteamGrateBlockEntity>> STEAM_GRATE = TYPES.register("steam_grate",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.powergen.SteamGrateBlockEntity::new, ReactorBlocks.STEAM_GRATE.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.powergen.TurbineCoreBlockEntity>> TURBINE_CORE = TYPES.register("turbine_core",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.powergen.TurbineCoreBlockEntity::new, ReactorBlocks.TURBINE_CORE.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.powergen.CondenserBlockEntity>> CONDENSER = TYPES.register("condenser",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.powergen.CondenserBlockEntity::new, ReactorBlocks.CONDENSER.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.powergen.SteamDiffuserBlockEntity>> STEAM_DIFFUSER = TYPES.register("steam_diffuser",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.powergen.SteamDiffuserBlockEntity::new, ReactorBlocks.STEAM_DIFFUSER.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.waste.WasteContainerBlockEntity>> WASTE_CONTAINER = TYPES.register("waste_container",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.waste.WasteContainerBlockEntity::new, ReactorBlocks.WASTE_CONTAINER.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.waste.WasteStorageBlockEntity>> WASTE_STORAGE = TYPES.register("waste_storage",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.waste.WasteStorageBlockEntity::new, ReactorBlocks.WASTE_STORAGE.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.waste.WasteDecayerBlockEntity>> WASTE_DECAYER = TYPES.register("waste_decayer",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.waste.WasteDecayerBlockEntity::new, ReactorBlocks.WASTE_DECAYER.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.reactor.BreederCoreBlockEntity>> BREEDER_CORE = TYPES.register("breeder_core",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.reactor.BreederCoreBlockEntity::new, ReactorBlocks.BREEDER_CORE.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.reactor.SodiumHeaterBlockEntity>> SODIUM_HEATER = TYPES.register("sodium_heater",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.reactor.SodiumHeaterBlockEntity::new, ReactorBlocks.SODIUM_HEATER.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.powergen.HeatExchangerBlockEntity>> HEAT_EXCHANGER = TYPES.register("heat_exchanger",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.powergen.HeatExchangerBlockEntity::new, ReactorBlocks.HEAT_EXCHANGER.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.reactor.PebbleBedBlockEntity>> PEBBLE_BED = TYPES.register("pebble_bed",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.reactor.PebbleBedBlockEntity::new, ReactorBlocks.PEBBLE_BED.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.reactor.CO2HeaterBlockEntity>> CO2_HEATER = TYPES.register("co2_heater",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.reactor.CO2HeaterBlockEntity::new, ReactorBlocks.CO2_HEATER.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.reactor.ThoriumCoreBlockEntity>> THORIUM_CORE = TYPES.register("thorium_core",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.reactor.ThoriumCoreBlockEntity::new, ReactorBlocks.THORIUM_CORE.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.reactor.FuelDumpBlockEntity>> FUEL_DUMP = TYPES.register("fuel_dump",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.reactor.FuelDumpBlockEntity::new, ReactorBlocks.FUEL_DUMP.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.waste.WastePipeBlockEntity>> WASTE_PIPE = TYPES.register("waste_pipe",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.waste.WastePipeBlockEntity::new, ReactorBlocks.WASTE_PIPE.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.powergen.HiPTurbineBlockEntity>> HIP_TURBINE = TYPES.register("hip_turbine",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.powergen.HiPTurbineBlockEntity::new, ReactorBlocks.HIP_TURBINE.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.powergen.SteamInjectorBlockEntity>> STEAM_INJECTOR = TYPES.register("steam_injector",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.powergen.SteamInjectorBlockEntity::new, ReactorBlocks.STEAM_INJECTOR.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.powergen.ReactorGeneratorBlockEntity>> GENERATOR = TYPES.register("reactor_generator",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.powergen.ReactorGeneratorBlockEntity::new, ReactorBlocks.GENERATOR.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.powergen.ReactorFlywheelBlockEntity>> FLYWHEEL = TYPES.register("turbine_flywheel",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.powergen.ReactorFlywheelBlockEntity::new, ReactorBlocks.FLYWHEEL.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.reactor.TritizerBlockEntity>> TRITIZER = TYPES.register("tritizer",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.reactor.TritizerBlockEntity::new, ReactorBlocks.TRITIZER.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.powergen.SolarExchangerBlockEntity>> SOLAR_EXCHANGER = TYPES.register("solar_exchanger",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.powergen.SolarExchangerBlockEntity::new, ReactorBlocks.SOLAR_EXCHANGER.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.powergen.SolarTopBlockEntity>> SOLAR_TOP = TYPES.register("solar_top",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.powergen.SolarTopBlockEntity::new, ReactorBlocks.SOLAR_TOP.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.content.blockentity.ElectrolyzerBlockEntity>> ELECTROLYZER = TYPES.register("electrolyzer",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.content.blockentity.ElectrolyzerBlockEntity::new, ReactorBlocks.ELECTROLYZER.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.fusion.ToroidMagnetBlockEntity>> TOROID_MAGNET = TYPES.register("toroid_magnet",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.fusion.ToroidMagnetBlockEntity::new, ReactorBlocks.TOROID_MAGNET.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.fusion.SolenoidMagnetBlockEntity>> SOLENOID = TYPES.register("solenoid",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.fusion.SolenoidMagnetBlockEntity::new, ReactorBlocks.SOLENOID.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.fusion.FusionInjectorBlockEntity>> FUSION_INJECTOR = TYPES.register("fusion_injector",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.fusion.FusionInjectorBlockEntity::new, ReactorBlocks.FUSION_INJECTOR.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.fusion.FusionHeaterBlockEntity>> FUSION_HEATER = TYPES.register("fusion_heater",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.fusion.FusionHeaterBlockEntity::new, ReactorBlocks.FUSION_HEATER.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.fusion.NeutronAbsorberBlockEntity>> NEUTRON_ABSORBER = TYPES.register("neutron_absorber",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.fusion.NeutronAbsorberBlockEntity::new, ReactorBlocks.NEUTRON_ABSORBER.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.reactor.NeutronReflectorBlockEntity>> NEUTRON_REFLECTOR = TYPES.register("neutron_reflector",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.reactor.NeutronReflectorBlockEntity::new, ReactorBlocks.NEUTRON_REFLECTOR.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.powergen.TurbineMeterBlockEntity>> TURBINE_METER = TYPES.register("turbine_meter",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.powergen.TurbineMeterBlockEntity::new, ReactorBlocks.TURBINE_METER.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.fusion.GasCollectorBlockEntity>> GAS_COLLECTOR = TYPES.register("gas_collector",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.fusion.GasCollectorBlockEntity::new, ReactorBlocks.GAS_COLLECTOR.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.fusion.FusionMarkerBlockEntity>> FUSION_MARKER = TYPES.register("fusion_marker",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.fusion.FusionMarkerBlockEntity::new, ReactorBlocks.FUSION_MARKER.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final Supplier<BlockEntityType<reika.reactorcraft.fusion.MagneticPipeBlockEntity>> MAGNETIC_PIPE = TYPES.register("magnetic_pipe",
			() -> BlockEntityType.Builder.of(reika.reactorcraft.fusion.MagneticPipeBlockEntity::new, ReactorBlocks.MAGNETIC_PIPE.get()).build(null));

	private ReactorBlockEntities() {}
}
