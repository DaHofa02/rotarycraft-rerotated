package xyz.dahofa.rotarycraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import xyz.dahofa.rotarycraft.api.lib.Names;
import xyz.dahofa.rotarycraft.common.entity.renderer.GrinderBlockEntityRenderer;
import xyz.dahofa.rotarycraft.common.registry.*;
import xyz.dahofa.rotarycraft.common.screen.GrinderScreen;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Names.MOD_ID)
public class RotaryCraftRerotated {
    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public RotaryCraftRerotated(ModContainer container, IEventBus modBus) {
        IEventBus forgeBus = NeoForge.EVENT_BUS;
        registerAllDeferredRegistryObjects(modBus);
    }

    private void registerAllDeferredRegistryObjects(IEventBus modBus) {

        RCItems.register(modBus);
        RCBlocks.register(modBus);
        RCArmorMaterials.register(modBus);

        RCBlockEntities.register(modBus);
        RCMenuTypes.register(modBus);
        RCRecipes.register(modBus);

        RCCreativeModeTab.register(modBus);
    }

    @EventBusSubscriber(modid = Names.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientOnlyEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(RCMenuTypes.GRINDER_MENU.get(), GrinderScreen::new);
        }

        @SubscribeEvent
        public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(RCBlockEntities.GRINDER_BE.get(), GrinderBlockEntityRenderer::new);
        }
    }

}
