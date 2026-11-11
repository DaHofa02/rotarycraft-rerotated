/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelComposter;
import reika.rotarycraft.content.blockentity.ComposterBlockEntity;

/** Original: Reika.RotaryCraft.Renders.MI.RenderComposter. */
public class ComposterRenderer extends MachineRenderer<ComposterBlockEntity> {

	private final ModelComposter model;

	public ComposterRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelComposter(ctx.bakeLayer(RotaryModelLayers.COMPOSTER));
	}

	@Override
	public void render(ComposterBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		model.renderAll(pose, consumer(buf, tex("compostertex.png")), light, overlay, tile, List.of(), 0, 0);
		pose.popPose();
	}
}
