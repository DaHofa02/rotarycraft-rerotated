/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelDetector;
import reika.rotarycraft.content.blockentity.PlayerDetectorBlockEntity;

/** Original: Reika.RotaryCraft.Renders.M.RenderDetector - a static model. */
public class PlayerDetectorRenderer extends MachineRenderer<PlayerDetectorBlockEntity> {

	private final ModelDetector model;

	public PlayerDetectorRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelDetector(ctx.bakeLayer(RotaryModelLayers.PLAYER_DETECTOR));
	}

	@Override
	public void render(PlayerDetectorBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		model.renderAll(pose, consumer(buf, MachineRenderer.tex("detectortex.png")), light, overlay, tile, null, 0, 0);
		pose.popPose();
	}
}
