/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelIodide;
import reika.rotarycraft.content.blockentity.WeatherControllerBlockEntity;

/** Original: Reika.RotaryCraft.Renders.MI.RenderWeatherController - the silver iodide launcher. */
public class WeatherControllerRenderer extends MachineRenderer<WeatherControllerBlockEntity> {

	private final ModelIodide model;

	public WeatherControllerRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelIodide(ctx.bakeLayer(RotaryModelLayers.WEATHER_CONTROLLER));
	}

	@Override
	public void render(WeatherControllerBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		model.renderAll(pose, consumer(buf, tex("iotex.png")), light, overlay, tile, null, 0, 0);
		pose.popPose();
	}
}
