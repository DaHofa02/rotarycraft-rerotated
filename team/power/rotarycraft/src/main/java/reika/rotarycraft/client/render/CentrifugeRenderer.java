/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelCentrifuge;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.CentrifugeBlockEntity;

/** Original: Reika.RotaryCraft.Renders.MI.RenderCentrifuge - the drum spins with phi; flipped machines hang upside down. */
public class CentrifugeRenderer extends MachineRenderer<CentrifugeBlockEntity> {

	private final ModelCentrifuge model;

	public CentrifugeRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelCentrifuge(ctx.bakeLayer(RotaryModelLayers.CENTRIFUGE));
	}

	@Override
	public void render(CentrifugeBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (tile.isFlipped) {
			pose.mulPose(TechneModel.rotation(180, 1, 0, 0));
			pose.translate(0, -2, 0);
		}
		model.renderAll(pose, consumer(buf, MachineRenderer.tex("centrifugetex.png")), light, overlay, tile, null, tile.phi, 0);
		pose.popPose();
	}
}
