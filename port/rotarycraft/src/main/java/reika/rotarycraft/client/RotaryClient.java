/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidType;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.client.render.BevelRenderer;
import reika.rotarycraft.client.render.ClutchRenderer;
import reika.rotarycraft.client.render.DynamometerRenderer;
import reika.rotarycraft.client.render.BedrockBreakerRenderer;
import reika.rotarycraft.client.render.EngineRenderer;
import reika.rotarycraft.client.render.ExtractorRenderer;
import reika.rotarycraft.client.render.FrictionHeaterRenderer;
import reika.rotarycraft.client.render.IORenderer;
import reika.rotarycraft.client.render.FlywheelRenderer;
import reika.rotarycraft.client.render.GearboxRenderer;
import reika.rotarycraft.client.render.ShaftRenderer;
import reika.rotarycraft.client.render.SplitterRenderer;
import reika.rotarycraft.client.screen.BevelScreen;
import reika.rotarycraft.client.screen.BlastFurnaceScreen;
import reika.rotarycraft.client.screen.EngineScreens;
import reika.rotarycraft.client.screen.ExtractorScreen;
import reika.rotarycraft.client.screen.WorktableScreen;
import reika.rotarycraft.client.screen.GearboxScreen;
import reika.rotarycraft.client.screen.SplitterScreen;
import reika.rotarycraft.content.menu.MachineMenu.Layout;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryMenus;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryFluids;

/** Original: Reika.RotaryCraft.ClientProxy (renderer registration). */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RotaryClient {

	private RotaryClient() {}

	/** Original ItemIntegratedGearbox.getItemSpriteIndex: the sprite follows the fill (item model overrides on "rotarycraft:fill"). */
	@SubscribeEvent
	public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e) {
		e.enqueueWork(() -> {
			for (var item : reika.rotarycraft.registry.RotaryItems.GEAR_UPGRADES)
				net.minecraft.client.renderer.item.ItemProperties.register(item.get(), net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "fill"),
						(is, level, entity, seed) -> reika.rotarycraft.content.item.IntegratedGearboxItem.fillState(is));
		});
	}

	@SubscribeEvent
	public static void extensions(RegisterClientExtensionsEvent e) {
		MachineItemRenderer[] renderer = new MachineItemRenderer[1];
		IClientItemExtensions items = new IClientItemExtensions() {
			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (renderer[0] == null)
					renderer[0] = new MachineItemRenderer();
				return renderer[0];
			}
		};
		for (var block : RotaryBlocks.BLOCKS.getEntries())
			if (block.get().asItem() != net.minecraft.world.item.Items.AIR)
				e.registerItem(items, block.get().asItem());

		fluid(e, RotaryFluids.LUBRICANT_TYPE.get(), "lubricant_anim");
		fluid(e, RotaryFluids.ETHANOL_TYPE.get(), "ethanol_anim");
		fluid(e, RotaryFluids.JET_FUEL_TYPE.get(), "jetfuel_anim");
		fluid(e, RotaryFluids.LIQUID_NITROGEN_TYPE.get(), "nitrogen_anim");
	}

	/** Original: EnhancedFluid.setIcons with the animated fluid texture for still and flowing. */
	private static void fluid(RegisterClientExtensionsEvent e, FluidType type, String texture) {
		ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "block/fluid/"+texture);
		e.registerFluidType(new IClientFluidTypeExtensions() {
			@Override
			public ResourceLocation getStillTexture() {
				return tex;
			}

			@Override
			public ResourceLocation getFlowingTexture() {
				return tex;
			}
		}, type);
	}

	@SubscribeEvent
	public static void screens(RegisterMenuScreensEvent e) {
		e.register(RotaryMenus.get(Layout.STEAM), EngineScreens.Steam::new);
		e.register(RotaryMenus.get(Layout.ETHANOL), EngineScreens.Ethanol::new);
		e.register(RotaryMenus.get(Layout.PERFORMANCE), EngineScreens.Performance::new);
		e.register(RotaryMenus.get(Layout.JET), EngineScreens.Jet::new);
		e.register(RotaryMenus.BLAST_FURNACE.get(), BlastFurnaceScreen::new);
		e.register(RotaryMenus.WORKTABLE.get(), WorktableScreen::new);
		e.register(RotaryMenus.EXTRACTOR.get(), ExtractorScreen::new);
		e.register(RotaryMenus.BORER.get(), reika.rotarycraft.client.screen.BorerScreen::new);
		e.register(RotaryMenus.GRINDER.get(), reika.rotarycraft.client.screen.GrinderScreen::new);
		e.register(RotaryMenus.FERMENTER.get(), reika.rotarycraft.client.screen.FermenterScreen::new);
		e.register(RotaryMenus.DRYING_BED.get(), reika.rotarycraft.client.screen.DryingBedScreen::new);
		e.register(RotaryMenus.WINDER.get(), reika.rotarycraft.client.screen.WinderScreen::new);
		e.register(RotaryMenus.REFRIGERATOR.get(), reika.rotarycraft.client.screen.RefrigeratorScreen::new);
		e.register(RotaryMenus.COMPACTOR.get(), reika.rotarycraft.client.screen.CompactorScreen::new);
		e.register(RotaryMenus.FILLING_STATION.get(), reika.rotarycraft.client.screen.FillingStationScreen::new);
		e.register(RotaryMenus.CENTRIFUGE.get(), reika.rotarycraft.client.screen.CentrifugeScreen::new);
		e.register(RotaryMenus.FRACTIONATOR.get(), reika.rotarycraft.client.screen.FractionatorScreen::new);
		e.register(RotaryMenus.PULSE_JET.get(), reika.rotarycraft.client.screen.PulseJetFurnaceScreen::new);
		e.register(RotaryMenus.get(Layout.ONE_SLOT), EngineScreens.OneSlot::new);
		e.register(RotaryMenus.get(Layout.GEARBOX), GearboxScreen::new);
		e.register(RotaryMenus.get(Layout.SPLITTER), SplitterScreen::new);
		e.register(RotaryMenus.get(Layout.BEVEL), BevelScreen::new);
	}

	@SubscribeEvent
	public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
		RotaryModelLayers.register(e);
	}

	@SubscribeEvent
	public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
		e.registerBlockEntityRenderer(RotaryBlockEntities.SHAFT.get(), ctx -> new IORenderer<>(new ShaftRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.GEARBOX.get(), ctx -> new IORenderer<>(new GearboxRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.BEVEL_GEARS.get(), ctx -> new IORenderer<>(new BevelRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.CLUTCH.get(), ctx -> new IORenderer<>(new ClutchRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.DYNAMOMETER.get(), ctx -> new IORenderer<>(new DynamometerRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.SHAFT_JUNCTION.get(), ctx -> new IORenderer<>(new SplitterRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.FLYWHEEL.get(), ctx -> new IORenderer<>(new FlywheelRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.FRICTION_HEATER.get(), ctx -> new IORenderer<>(new FrictionHeaterRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.COOLING_FIN.get(), ctx -> new reika.rotarycraft.client.render.CoolingFinRenderer(ctx));
		e.registerBlockEntityRenderer(RotaryBlockEntities.FUEL_ENHANCER.get(), ctx -> new IORenderer<>(new reika.rotarycraft.client.render.FuelEnhancerRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.MIRROR.get(), ctx -> new reika.rotarycraft.client.render.MirrorRenderer(ctx));
		e.registerBlockEntityRenderer(RotaryBlockEntities.WINDER.get(), ctx -> new IORenderer<>(new reika.rotarycraft.client.render.WinderRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.FILLING_STATION.get(), ctx -> new IORenderer<>(new reika.rotarycraft.client.render.FillingStationRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.COMPACTOR.get(), ctx -> new IORenderer<>(new reika.rotarycraft.client.render.CompactorRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.HEAT_RAY.get(), ctx -> new IORenderer<>(new reika.rotarycraft.client.render.HeatRayRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.REFRIGERATOR.get(), ctx -> new IORenderer<>(new reika.rotarycraft.client.render.RefrigeratorRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.DRYING_BED.get(), reika.rotarycraft.client.render.DryingBedRenderer::new);
		e.registerBlockEntityRenderer(RotaryBlockEntities.VAN_DE_GRAFF.get(), ctx -> new IORenderer<>(new reika.rotarycraft.client.render.VanDeGraffRenderer(ctx)));
		e.registerEntityRenderer(reika.rotarycraft.registry.RotaryEntities.DISCHARGE.get(), reika.rotarycraft.client.render.DischargeRenderer::new);
		e.registerBlockEntityRenderer(RotaryBlockEntities.PIPE_PUMP.get(), ctx -> new IORenderer<>(new reika.rotarycraft.client.render.PipePumpRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.SOLAR_TOWER.get(), ctx -> new IORenderer<>(new reika.rotarycraft.client.render.SolarTowerRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.EXTRACTOR.get(), ctx -> new IORenderer<>(new ExtractorRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.GRINDER.get(), ctx -> new IORenderer<>(new reika.rotarycraft.client.render.GrinderRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.CENTRIFUGE.get(), ctx -> new IORenderer<>(new reika.rotarycraft.client.render.CentrifugeRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.FAN.get(), ctx -> new IORenderer<>(new reika.rotarycraft.client.render.FanRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.MOB_HARVESTER.get(), ctx -> new IORenderer<>(new reika.rotarycraft.client.render.MobHarvesterRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.FRACTIONATOR.get(), ctx -> new IORenderer<>(new reika.rotarycraft.client.render.FractionatorRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.PUMP.get(), ctx -> new IORenderer<>(new reika.rotarycraft.client.render.PumpRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.PULSE_JET.get(), ctx -> new IORenderer<>(new reika.rotarycraft.client.render.PulseJetFurnaceRenderer(ctx)));
		e.registerBlockEntityRenderer(RotaryBlockEntities.RESERVOIR.get(), reika.rotarycraft.client.render.ReservoirRenderer::new);
		e.registerBlockEntityRenderer(RotaryBlockEntities.BEDROCK_BREAKER.get(), ctx -> new IORenderer<>(new BedrockBreakerRenderer(ctx)));
		//Original PipeRenderer: no I/O overlay
		e.registerBlockEntityRenderer(RotaryBlockEntities.HOSE.get(), reika.rotarycraft.client.render.PipeFluidRenderer::new);
		e.registerBlockEntityRenderer(RotaryBlockEntities.PIPE.get(), reika.rotarycraft.client.render.PipeFluidRenderer::new);
		e.registerBlockEntityRenderer(RotaryBlockEntities.FUEL_LINE.get(), reika.rotarycraft.client.render.PipeFluidRenderer::new);
		e.registerBlockEntityRenderer(RotaryBlockEntities.BEDROCK_PIPE.get(), reika.rotarycraft.client.render.PipeFluidRenderer::new);
		for (var t : java.util.List.of(RotaryBlockEntities.VALVE, RotaryBlockEntities.BYPASS, RotaryBlockEntities.SEPARATION, RotaryBlockEntities.SUCTION))
			e.registerBlockEntityRenderer(t.get(), reika.rotarycraft.client.render.PipeFluidRenderer::new);
		for (var type : RotaryBlockEntities.engines())
			e.registerBlockEntityRenderer(type.get(), ctx -> new IORenderer<>(new EngineRenderer(ctx)));
	}
}
