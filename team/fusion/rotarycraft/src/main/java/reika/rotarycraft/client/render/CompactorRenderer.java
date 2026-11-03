/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelCompactor;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.CompactorBlockEntity;

/** Original: Reika.RotaryCraft.Renders.DMI.RenderCompactor - turned by its metadata, the press plates moving with phi (1 in the hand). */
public class CompactorRenderer extends MachineRenderer<CompactorBlockEntity> {

	private final ModelCompactor model;

	public CompactorRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelCompactor(ctx.bakeLayer(RotaryModelLayers.COMPACTOR));
	}

	@Override
	public void render(CompactorBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (isInWorld(tile)) {
			int var11 = switch (tile.getBlockMetadata()) {
				case 0 -> 180;
				case 2 -> 270;
				case 3 -> 90;
				default -> 0;
			};
			pose.mulPose(TechneModel.rotation(var11, 0, 1, 0));
		}
		float p = isInWorld(tile) ? tile.phi : 1;
		if (p < 0.5F)
			p = 1;
		model.renderAll(pose, consumer(buf, tex("compactortex.png")), light, overlay, tile, null, p, 0);
		pose.popPose();
	}
}
