/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelHeater;
import reika.rotarycraft.content.blockentity.HeaterBlockEntity;

/** Original: Reika.RotaryCraft.Renders.MI.RenderHeater - the heater, glowing in five steps from 200 to 900 deg C. */
public class HeaterRenderer extends MachineRenderer<HeaterBlockEntity> {

	private final ModelHeater model;

	public HeaterRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelHeater(ctx.bakeLayer(RotaryModelLayers.HEATER));
	}

	private static String texture(int temperature) {
		if (temperature >= 900)
			return "heater/heatertex900c.png";
		if (temperature >= 800)
			return "heater/heatertex800c.png";
		if (temperature >= 600)
			return "heater/heatertex600c.png";
		if (temperature >= 400)
			return "heater/heatertex400c.png";
		if (temperature >= 200)
			return "heater/heatertex200c.png";
		return "heater/heatertex.png";
	}

	@Override
	public void render(HeaterBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		String tex = isInWorld(tile) ? texture(tile.getTemperature()) : "heater/heatertex.png";
		model.renderAll(pose, consumer(buf, tex(tex)), light, overlay, tile, List.of(), 0, 0);
		pose.popPose();
	}
}
