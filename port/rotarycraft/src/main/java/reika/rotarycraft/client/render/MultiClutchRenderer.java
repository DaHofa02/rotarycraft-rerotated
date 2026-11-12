/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelMultiClutch;
import reika.rotarycraft.content.blockentity.MultiClutchBlockEntity;

/** Original: Reika.RotaryCraft.Renders.RenderMultiClutch (not turned with its metadata). The coloured side faces are not ported. */
public class MultiClutchRenderer extends MachineRenderer<MultiClutchBlockEntity> {

	private final ModelMultiClutch model;

	public MultiClutchRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelMultiClutch(ctx.bakeLayer(RotaryModelLayers.MULTI_CLUTCH));
	}

	@Override
	public void render(MultiClutchBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		model.renderAll(pose, consumer(buf, tex("transmission/multiclutchtex.png")), light, overlay, tile, null, tile.phi, 0);
		pose.popPose();
	}
}
