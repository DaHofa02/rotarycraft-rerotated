/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;


import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelAggregator;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.AggregatorBlockEntity;

/** Original: Reika.RotaryCraft.Renders.M.RenderAggregator - the rotor turns with phi. The water level shown inside the original is not drawn. */
public class AggregatorRenderer extends MachineRenderer<AggregatorBlockEntity> {

	private final ModelAggregator model;

	public AggregatorRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelAggregator(ctx.bakeLayer(RotaryModelLayers.AGGREGATOR));
	}

	@Override
	public void render(AggregatorBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		model.renderAll(pose, consumer(buf, MachineRenderer.tex("aggregatortex.png")), light, overlay, tile, null, -tile.phi, 0);
		pose.popPose();
	}
}
