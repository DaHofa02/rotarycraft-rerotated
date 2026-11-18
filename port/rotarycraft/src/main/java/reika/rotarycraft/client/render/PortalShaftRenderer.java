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
import reika.rotarycraft.client.model.ModelPortalShaft;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.PortalShaftBlockEntity;

/** Own model and texture: the axle lies along the shaft's axis (meta 0/1 x, 2/3 z, 4/5 y). */
public class PortalShaftRenderer extends MachineRenderer<PortalShaftBlockEntity> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/machine/own/portal_shaft.png");

	private final ModelPortalShaft model;

	public PortalShaftRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelPortalShaft(ctx.bakeLayer(RotaryModelLayers.PORTAL_SHAFT));
	}

	@Override
	public void render(PortalShaftBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		int meta = tile.hasLevel() ? tile.getBlockMetadata() : 2;
		if (meta < 2) {
			pose.mulPose(TechneModel.rotation(90, 0, 1, 0));
		}
		else if (meta >= 4) {
			pose.translate(0, 1, 0);
			pose.mulPose(TechneModel.rotation(90, 1, 0, 0));
			pose.translate(0, -1, 0);
		}
		model.renderAll(pose, consumer(buf, TEX), light, overlay, tile, null, tile.phi, 0);
		pose.popPose();
	}
}
