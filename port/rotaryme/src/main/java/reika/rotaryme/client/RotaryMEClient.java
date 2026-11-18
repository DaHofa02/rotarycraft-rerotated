/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import appeng.api.client.AEKeyRenderHandler;
import appeng.api.client.AEKeyRendering;

import reika.rotaryme.KineticKey;
import reika.rotaryme.KineticKeyType;
import reika.rotaryme.RotaryME;

/** Client side: the output GUI, and how kinetic energy looks in ME terminals (the addon's own energy icon). */
@EventBusSubscriber(modid = RotaryME.MOD_ID, value = Dist.CLIENT)
public final class RotaryMEClient {

	private RotaryMEClient() {}

	@SubscribeEvent
	public static void screens(RegisterMenuScreensEvent e) {
		e.register(RotaryME.KINETIC_INPUT_MENU.get(), KineticInputScreen::new);
		e.register(RotaryME.BUNDLED_BUS_MENU.get(), BundledBusScreen::new);
		e.register(RotaryME.KINETIC_OUTPUT_MENU.get(), KineticOutputScreen::new);
	}

	/** The kinetic blocks draw nothing themselves (full-block models); the renderer only shows their in/out boxes after a screwdriver click. */
	@SubscribeEvent
	public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e) {
		e.registerBlockEntityRenderer(RotaryME.KINETIC_INPUT_BE.get(), ctx -> new reika.rotarycraft.client.render.IORenderer<>((te, partial, pose, buf, light, overlay) -> {}));
		e.registerBlockEntityRenderer(RotaryME.KINETIC_OUTPUT_BE.get(), ctx -> new reika.rotarycraft.client.render.IORenderer<>((te, partial, pose, buf, light, overlay) -> {}));
	}

	@SubscribeEvent
	public static void setup(FMLClientSetupEvent e) {
		e.enqueueWork(() -> AEKeyRendering.register(KineticKeyType.INSTANCE, KineticKey.class, new AEKeyRenderHandler<KineticKey>() {
			@Override
			public void drawInGui(Minecraft mc, GuiGraphics g, int x, int y, KineticKey key) {
				g.renderItem(new ItemStack(RotaryME.ENERGY_ICON.get()), x, y);
			}

			@Override
			public void drawOnBlockFace(PoseStack pose, MultiBufferSource buf, KineticKey key, float scale, int light, Level level) {}

			@Override
			public Component getDisplayName(KineticKey key) {
				return key.getDisplayName();
			}
		}));
	}
}
