/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelExtractor;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.ExtractorBlockEntity;

/** Original: Reika.RotaryCraft.Renders.MI.RenderExtractor - opaque parts in render pass 0, the translucent tank (Shape5) in pass 1. */
public class ExtractorRenderer extends MachineRenderer<ExtractorBlockEntity> {

	private final ModelExtractor model;

	public ExtractorRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelExtractor(ctx.bakeLayer(RotaryModelLayers.EXTRACTOR));
	}

	@Override
	public void render(ExtractorBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (isInWorld(tile)) {
			int meta = tile.getBlockMetadata();
			int var11 = switch(meta) {
				case 1 -> 180;
				case 2 -> 270;
				case 3 -> 90;
				default -> 0;
			};
			if (meta <= 3)
				pose.mulPose(TechneModel.rotation((float)var11+90, 0.0F, 1.0F, 0.0F));
			else {
				pose.mulPose(TechneModel.rotation(var11, 1F, 0F, 0.0F));
				pose.translate(0F, -1F, 1F);
				if (meta == 5)
					pose.translate(0F, 0F, -2F);
			}
		}
		var t = MachineRenderer.tex("extractortex.png");
		model.renderAll(pose, consumer(buf, t), light, overlay, tile, List.of(false), 0, 0);
		model.renderAll(pose, buf.getBuffer(RenderType.entityTranslucent(t)), light, overlay, tile, List.of(true), 0, 0);
		pose.popPose();
	}
}
