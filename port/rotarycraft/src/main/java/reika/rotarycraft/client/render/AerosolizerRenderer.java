/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelAerosolizer;
import reika.rotarycraft.content.blockentity.AerosolizerBlockEntity;

/** Original: Reika.RotaryCraft.Renders.MI.RenderAerosolizer. */
public class AerosolizerRenderer extends MachineRenderer<AerosolizerBlockEntity> {

	private final ModelAerosolizer model;

	public AerosolizerRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelAerosolizer(ctx.bakeLayer(RotaryModelLayers.AEROSOLIZER));
	}

	@Override
	public void render(AerosolizerBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		model.renderAll(pose, consumer(buf, tex("aerotex.png")), light, overlay, tile, null, 0, 0);
		pose.popPose();
	}
}
