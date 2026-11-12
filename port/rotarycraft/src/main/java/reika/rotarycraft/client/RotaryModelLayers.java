/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client;

import java.util.function.Supplier;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.client.model.*;

/** Model layers of the converted original models. */
public final class RotaryModelLayers {

	public static final ModelLayerLocation SHAFT = layer("shaft");
	public static final ModelLayerLocation SHAFT_V = layer("shaft_v");
	public static final ModelLayerLocation CROSS = layer("cross");
	public static final ModelLayerLocation GEARBOX = layer("gearbox");
	public static final ModelLayerLocation GEARBOX4 = layer("gearbox4");
	public static final ModelLayerLocation GEARBOX8 = layer("gearbox8");
	public static final ModelLayerLocation GEARBOX16 = layer("gearbox16");
	public static final ModelLayerLocation BEVEL = layer("bevel");
	public static final ModelLayerLocation CLUTCH = layer("clutch");
	public static final ModelLayerLocation CLUTCH_V = layer("clutch_v");
	public static final ModelLayerLocation MONITOR = layer("monitor");
	public static final ModelLayerLocation SPLITTER = layer("splitter");
	public static final ModelLayerLocation SPLITTER2 = layer("splitter2");
	public static final ModelLayerLocation FLYWHEEL = layer("flywheel");
	public static final ModelLayerLocation DC = layer("dc");
	public static final ModelLayerLocation STEAM = layer("steam");
	public static final ModelLayerLocation COMBUSTION = layer("combustion");
	public static final ModelLayerLocation AC = layer("ac");
	public static final ModelLayerLocation PERFORMANCE = layer("performance");
	public static final ModelLayerLocation MICRO = layer("micro");
	public static final ModelLayerLocation JET = layer("jet");
	public static final ModelLayerLocation FRICTION = layer("friction");
	public static final ModelLayerLocation COOLING_FIN = layer("cooling_fin");
	public static final ModelLayerLocation FUEL_ENHANCER = layer("fuel_enhancer");
	public static final ModelLayerLocation MIRROR = layer("solar_mirror");
	public static final ModelLayerLocation SOLAR_TOWER = layer("solar_tower");
	public static final ModelLayerLocation PIPE_PUMP = layer("pipe_pump");
	public static final ModelLayerLocation VAN_DE_GRAFF = layer("van_de_graff");
	public static final ModelLayerLocation DRYING_BED = layer("drying_bed");
	public static final ModelLayerLocation WINDER = layer("winder");
	public static final ModelLayerLocation REFRIGERATOR = layer("refrigerator");
	public static final ModelLayerLocation HEAT_RAY = layer("heat_ray");
	public static final ModelLayerLocation COMPACTOR = layer("compactor");
	public static final ModelLayerLocation FILLING_STATION = layer("filling_station");
	public static final ModelLayerLocation BEDROCK_BREAKER = layer("bedrock_breaker");
	public static final ModelLayerLocation EXTRACTOR = layer("extractor");
	public static final ModelLayerLocation GRINDER = layer("grinder");
	public static final ModelLayerLocation CENTRIFUGE = layer("centrifuge");
	public static final ModelLayerLocation FAN = layer("fan");
	public static final ModelLayerLocation MOB_HARVESTER = layer("mob_harvester");
	public static final ModelLayerLocation AGGREGATOR = layer("aggregator");
	public static final ModelLayerLocation FRACTIONATOR = layer("fractionator");
	public static final ModelLayerLocation PUMP = layer("pump");
	public static final ModelLayerLocation SMOKE_DETECTOR = layer("smoke_detector");
	public static final ModelLayerLocation PLAYER_DETECTOR = layer("player_detector");
	public static final ModelLayerLocation MOB_RADAR = layer("mob_radar");
	public static final ModelLayerLocation LAVA_MAKER = layer("lava_maker");
	public static final ModelLayerLocation BEAM_MIRROR = layer("beam_mirror");
	public static final ModelLayerLocation MAGNETIZER = layer("magnetizer");
	public static final ModelLayerLocation LINE_BUILDER = layer("line_builder");
	public static final ModelLayerLocation FLUID_COMPRESSOR = layer("fluid_compressor");
	public static final ModelLayerLocation SPILLWAY = layer("spillway");
	public static final ModelLayerLocation BIG_FURNACE = layer("big_furnace");
	public static final ModelLayerLocation MULTI_CLUTCH = layer("multi_clutch");
	public static final ModelLayerLocation WETTER = layer("wetter");
	public static final ModelLayerLocation HYDRATOR = layer("hydrator");
	public static final ModelLayerLocation CRYSTALLIZER = layer("crystallizer");
	public static final ModelLayerLocation COMPOSTER = layer("composter");
	public static final ModelLayerLocation HEATER = layer("heater");
	public static final ModelLayerLocation RESERVOIR = layer("reservoir");
	public static final ModelLayerLocation PULSE_JET = layer("pulse_jet_furnace");
	public static final ModelLayerLocation BEDROCK_BREAKER_V = layer("bedrock_breaker_v");
	public static final ModelLayerLocation WIND = layer("wind");
	public static final ModelLayerLocation HYDRO = layer("hydro");

	private RotaryModelLayers() {}

	private static ModelLayerLocation layer(String name) {
		return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, name), "main");
	}

	static void register(EntityRenderersEvent.RegisterLayerDefinitions e) {
		reg(e, SHAFT, ModelShaft::createLayer);
		reg(e, SHAFT_V, ModelShaftV::createLayer);
		reg(e, CROSS, ModelCross::createLayer);
		reg(e, GEARBOX, ModelGearbox::createLayer);
		reg(e, GEARBOX4, ModelGearbox4::createLayer);
		reg(e, GEARBOX8, ModelGearbox8::createLayer);
		reg(e, GEARBOX16, ModelGearbox16::createLayer);
		reg(e, BEVEL, ModelBevel::createLayer);
		reg(e, CLUTCH, ModelClutch::createLayer);
		reg(e, CLUTCH_V, ModelVClutch::createLayer);
		reg(e, MONITOR, ModelMonitor::createLayer);
		reg(e, SPLITTER, ModelSplitter::createLayer);
		reg(e, SPLITTER2, ModelSplitter2::createLayer);
		reg(e, FLYWHEEL, ModelFlywheel::createLayer);
		reg(e, DC, ModelDC::createLayer);
		reg(e, STEAM, ModelSteam::createLayer);
		reg(e, COMBUSTION, ModelCombustion::createLayer);
		reg(e, AC, ModelAC::createLayer);
		reg(e, PERFORMANCE, ModelPerformance::createLayer);
		reg(e, MICRO, ModelMicroTurbine::createLayer);
		reg(e, JET, ModelJet::createLayer);
		reg(e, FRICTION, ModelFriction::createLayer);
		reg(e, COOLING_FIN, reika.rotarycraft.client.model.ModelFin::createLayer);
		reg(e, FUEL_ENHANCER, reika.rotarycraft.client.model.ModelFuelConverter::createLayer);
		reg(e, MIRROR, reika.rotarycraft.client.model.ModelMirror::createLayer);
		reg(e, SOLAR_TOWER, reika.rotarycraft.client.model.ModelSolar::createLayer);
		reg(e, PIPE_PUMP, reika.rotarycraft.client.model.ModelPipePump::createLayer);
		reg(e, VAN_DE_GRAFF, reika.rotarycraft.client.model.ModelVanDeGraff::createLayer);
		reg(e, DRYING_BED, reika.rotarycraft.client.model.ModelDryingBed::createLayer);
		reg(e, WINDER, reika.rotarycraft.client.model.ModelWinder::createLayer);
		reg(e, REFRIGERATOR, reika.rotarycraft.client.model.ModelFridge::createLayer);
		reg(e, HEAT_RAY, reika.rotarycraft.client.model.ModelHRay::createLayer);
		reg(e, COMPACTOR, reika.rotarycraft.client.model.ModelCompactor::createLayer);
		reg(e, FILLING_STATION, reika.rotarycraft.client.model.ModelFillingStation::createLayer);
		reg(e, BEDROCK_BREAKER, ModelBedrockBreaker::createLayer);
		reg(e, EXTRACTOR, ModelExtractor::createLayer);
		reg(e, GRINDER, reika.rotarycraft.client.model.ModelGrinder::createLayer);
		reg(e, CENTRIFUGE, reika.rotarycraft.client.model.ModelCentrifuge::createLayer);
		reg(e, FAN, reika.rotarycraft.client.model.ModelFan::createLayer);
		reg(e, MOB_HARVESTER, reika.rotarycraft.client.model.ModelHarvester::createLayer);
		reg(e, AGGREGATOR, reika.rotarycraft.client.model.ModelAggregator::createLayer);
		reg(e, FRACTIONATOR, reika.rotarycraft.client.model.ModelFraction::createLayer);
		reg(e, PUMP, reika.rotarycraft.client.model.ModelPump::createLayer);
		reg(e, SMOKE_DETECTOR, reika.rotarycraft.client.model.ModelSmokeDetector::createLayer);
		reg(e, PLAYER_DETECTOR, reika.rotarycraft.client.model.ModelDetector::createLayer);
		reg(e, MOB_RADAR, reika.rotarycraft.client.model.ModelRadar::createLayer);
		reg(e, LAVA_MAKER, reika.rotarycraft.client.model.ModelLavaMaker::createLayer);
		reg(e, BEAM_MIRROR, reika.rotarycraft.client.model.ModelBeamMirror::createLayer);
		reg(e, MAGNETIZER, reika.rotarycraft.client.model.ModelMagnetizer::createLayer);
		reg(e, LINE_BUILDER, reika.rotarycraft.client.model.ModelRam::createLayer);
		reg(e, FLUID_COMPRESSOR, reika.rotarycraft.client.model.ModelGasCompressor::createLayer);
		reg(e, SPILLWAY, reika.rotarycraft.client.model.ModelSpillway::createLayer);
		reg(e, BIG_FURNACE, reika.rotarycraft.client.model.ModelBigFurnace::createLayer);
		reg(e, MULTI_CLUTCH, reika.rotarycraft.client.model.ModelMultiClutch::createLayer);
		reg(e, WETTER, reika.rotarycraft.client.model.ModelWetter::createLayer);
		reg(e, HYDRATOR, reika.rotarycraft.client.model.ModelReservoir::createLayer);
		reg(e, CRYSTALLIZER, reika.rotarycraft.client.model.ModelCrystallizer::createLayer);
		reg(e, COMPOSTER, reika.rotarycraft.client.model.ModelComposter::createLayer);
		reg(e, HEATER, reika.rotarycraft.client.model.ModelHeater::createLayer);
		reg(e, RESERVOIR, reika.rotarycraft.client.model.ModelReservoir::createLayer);
		reg(e, PULSE_JET, reika.rotarycraft.client.model.ModelPulseFurnace::createLayer);
		reg(e, BEDROCK_BREAKER_V, ModelBedrockBreakerV::createLayer);
		reg(e, WIND, ModelWind::createLayer);
		reg(e, HYDRO, ModelHydro::createLayer);
	}

	private static void reg(EntityRenderersEvent.RegisterLayerDefinitions e, ModelLayerLocation loc, Supplier<LayerDefinition> def) {
		e.registerLayerDefinition(loc, def);
	}
}
