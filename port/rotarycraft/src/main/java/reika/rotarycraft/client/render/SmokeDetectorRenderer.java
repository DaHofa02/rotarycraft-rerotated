/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelSmokeDetector;
import reika.rotarycraft.content.blockentity.SmokeDetectorBlockEntity;

/** Original: Reika.RotaryCraft.Renders.MI.RenderSmokeDetector - a static model, drawn twice as large as an item. */
public class SmokeDetectorRenderer extends MachineRenderer<SmokeDetectorBlockEntity> {

	private final ModelSmokeDetector model;

	public SmokeDetectorRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelSmokeDetector(ctx.bakeLayer(RotaryModelLayers.SMOKE_DETECTOR));
	}

	@Override
	public void render(SmokeDetectorBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (!isInWorld(tile)) {
			pose.scale(2, 2, 2);
			pose.mulPose(Axis.YP.rotationDegrees(-90));
		}
		model.renderAll(pose, consumer(buf, MachineRenderer.tex("smokedetectortex.png")), light, overlay, tile, null, 0, 0);
		pose.popPose();
	}
}
