/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.entity.RadiationEntity;
import reika.reactorcraft.content.item.RadiationGogglesItem;

/**
 * Original: RenderRadiation - a radiation cloud is drawn only for a player wearing radiation goggles: a camera-facing square of
 * radiation2.png, min(20, range) blocks wide, darkened (grey 127) and added onto the scene (black is invisible). The original also
 * turned the depth test off; here the depth test stays on.
 */
public class RadiationRenderer extends EntityRenderer<RadiationEntity> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "textures/entity/radiation2.png");

	public RadiationRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public void render(RadiationEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
		var cam = Minecraft.getInstance().player;
		if (cam == null || !RadiationGogglesItem.isWearingGoggles(cam))
			return;
		pose.pushPose();
		pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
		float size = Math.min(20, e.getRange());
		pose.scale(size, size, 1);
		VertexConsumer vc = buf.getBuffer(RenderType.eyes(TEX)); //additive: black stays invisible as with the original blend
		var m = pose.last();
		int c = 127;
		vertex(vc, m, -0.5F, -0.5F, 0, 1, c);
		vertex(vc, m, 0.5F, -0.5F, 1, 1, c);
		vertex(vc, m, 0.5F, 0.5F, 1, 0, c);
		vertex(vc, m, -0.5F, 0.5F, 0, 0, c);
		pose.popPose();
	}

	private static void vertex(VertexConsumer vc, PoseStack.Pose m, float x, float y, float u, float v, int c) {
		vc.addVertex(m, x, y, 0).setColor(c, c, c, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(m, 0, 1, 0);
	}

	@Override
	public ResourceLocation getTextureLocation(RadiationEntity e) {
		return TEX;
	}
}
