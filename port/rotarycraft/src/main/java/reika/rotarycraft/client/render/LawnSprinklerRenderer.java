/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelLawnSprinkler;
import reika.rotarycraft.content.blockentity.LawnSprinklerBlockEntity;

/** Original: Reika.RotaryCraft.Renders.M.RenderLawnSprinkler. */
public class LawnSprinklerRenderer extends MachineRenderer<LawnSprinklerBlockEntity> {

	private final ModelLawnSprinkler model;

	public LawnSprinklerRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelLawnSprinkler(ctx.bakeLayer(RotaryModelLayers.LAWN_SPRINKLER));
	}

	@Override
	public void render(LawnSprinklerBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		model.renderAll(pose, consumer(buf, tex("lawnsprinklertex.png")), light, overlay, tile, null, -tile.phi, 0);
		pose.popPose();
	}
}
