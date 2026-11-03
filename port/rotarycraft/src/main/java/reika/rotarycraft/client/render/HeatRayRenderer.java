/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelHRay;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.HeatRayBlockEntity;

/** Original: Reika.RotaryCraft.Renders.DM.RenderHRay - the heat ray turned by its metadata (no visible beam, as in the original). */
public class HeatRayRenderer extends MachineRenderer<HeatRayBlockEntity> {

	private final ModelHRay model;

	public HeatRayRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelHRay(ctx.bakeLayer(RotaryModelLayers.HEAT_RAY));
	}

	@Override
	public void render(HeatRayBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (isInWorld(tile)) {
			int var11 = switch (tile.getBlockMetadata()) {
				case 1 -> 180;
				case 2 -> 270;
				case 3 -> 90;
				default -> 0;
			};
			pose.mulPose(TechneModel.rotation(var11+90, 0, 1, 0));
		}
		model.renderAll(pose, consumer(buf, tex("hray.png")), light, overlay, tile, null, 0, 0);
		pose.popPose();
	}
}
