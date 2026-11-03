/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.client;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

import reika.reactorcraft.ReactorConfig;
import reika.reactorcraft.content.entity.NeutronEntity;

/** Original: RenderNeutron - a small untextured blue square facing the camera (brighter when fast), with option "Visible Neutrons". */
public class NeutronRenderer extends EntityRenderer<NeutronEntity> {

	public NeutronRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		shadowRadius = 0.15F;
		shadowStrength = 0.75F;
	}

	@Override
	public void render(NeutronEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
		if (!ReactorConfig.VISIBLENEUTRONS.get())
			return;
		pose.pushPose();
		pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
		float s = 0.3F;
		pose.scale(s, s, s);
		//original setColorOpaque(0, 0, fast ? 0x22aaff : 0x0000aa): each channel clamped to 255, so pure blue (fast) or dark blue
		int r = 0, g = 0, b = e.getNeutronSpeed() == NeutronEntity.NeutronSpeed.FAST ? 255 : 0xaa;
		VertexConsumer vc = buf.getBuffer(RenderType.debugQuads());
		Matrix4f m = pose.last().pose();
		float a = 0.25F, ox = 0.5F, oy = 0.25F;
		vc.addVertex(m, -ox, -oy, 0).setColor(r, g, b, 255);
		vc.addVertex(m, a-ox, -oy, 0).setColor(r, g, b, 255);
		vc.addVertex(m, a-ox, a-oy, 0).setColor(r, g, b, 255);
		vc.addVertex(m, -ox, a-oy, 0).setColor(r, g, b, 255);
		pose.popPose();
		super.render(e, yaw, partial, pose, buf, light);
	}

	@Override
	public ResourceLocation getTextureLocation(NeutronEntity e) {
		return TextureAtlas.LOCATION_BLOCKS;
	}
}
