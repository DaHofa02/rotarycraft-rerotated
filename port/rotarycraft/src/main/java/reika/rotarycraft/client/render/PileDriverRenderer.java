/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelPileDriver;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.PileDriverBlockEntity;

/** Original: Reika.RotaryCraft.Renders.DMM.RenderPileDriver - the frame turned by its axis, the flywheel turning while powered. */
public class PileDriverRenderer extends MachineRenderer<PileDriverBlockEntity> {

	private final ModelPileDriver model;

	public PileDriverRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelPileDriver(ctx.bakeLayer(RotaryModelLayers.PILE_DRIVER));
	}

	@Override
	public void render(PileDriverBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (isInWorld(tile))
			pose.mulPose(TechneModel.rotation(tile.getBlockMetadata() == 0 ? 90 : 0, 0, 1, 0));
		model.renderAll(pose, consumer(buf, tex("piletex.png")), light, overlay, tile, List.of(tile.getPower() > 0), tile.phi, 0);
		pose.popPose();
	}
}
