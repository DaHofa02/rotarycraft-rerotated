/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelCctv;
import reika.rotarycraft.content.blockentity.CctvBlockEntity;

/** Own model and texture: the camera head points where the CCTV looks (Minecraft yaw, so the model's -z faces south at yaw 0). */
public class CctvRenderer extends MachineRenderer<CctvBlockEntity> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/machine/own/cctv.png");

	private final ModelCctv model;

	public CctvRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelCctv(ctx.bakeLayer(RotaryModelLayers.CCTV));
	}

	@Override
	public void render(CctvBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		model.renderAll(pose, consumer(buf, TEX), light, overlay, tile, null, 180-tile.yaw, -tile.theta);
		pose.popPose();
	}
}
