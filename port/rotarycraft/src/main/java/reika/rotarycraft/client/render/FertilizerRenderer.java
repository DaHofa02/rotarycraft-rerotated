/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelFertilizer;
import reika.rotarycraft.content.blockentity.FertilizerBlockEntity;

/** Original: Reika.RotaryCraft.Renders.MI.RenderFertilizer (fertilizertex.png; the enchantment glint is not ported). */
public class FertilizerRenderer extends MachineRenderer<FertilizerBlockEntity> {

	private final ModelFertilizer model;

	public FertilizerRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelFertilizer(ctx.bakeLayer(RotaryModelLayers.FERTILIZER));
	}

	@Override
	public void render(FertilizerBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		model.renderAll(pose, consumer(buf, tex("fertilizertex.png")), light, overlay, tile, null, -tile.phi, 0);
		pose.popPose();
	}
}
