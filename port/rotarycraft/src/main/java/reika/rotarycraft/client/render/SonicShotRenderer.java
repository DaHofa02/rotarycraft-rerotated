/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

import reika.rotarycraft.content.entity.SonicShotEntity;

/**
 * Original: Reika.RotaryCraft.Entities.RenderSonicShot - a pale blue (127, 222, 255) bell of lines at 0.3 scale, pointing the way it flies:
 * six profile curves around its axis and seven rings.
 */
public class SonicShotRenderer extends EntityRenderer<SonicShotEntity> {

	private static final double[] X = {0, 0, -0.0625, -0.1875, -0.4375, -0.75, -1.5, -2.5};
	private static final int COLOR = 0xFF7FDEFF;

	public SonicShotRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		shadowRadius = 0.15F;
	}

	@Override
	public void render(SonicShotEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
		int[] d = e.getSteps();
		int ry = d[0] == -1 ? 180 : d[2] == 1 ? -90 : d[2] == -1 ? 90 : 0;
		int rz = d[1] == 1 ? 90 : d[1] == -1 ? -90 : 0;
		pose.pushPose();
		pose.scale(0.3F, 0.3F, 0.3F);
		pose.mulPose(Axis.YP.rotationDegrees(ry));
		pose.mulPose(Axis.ZP.rotationDegrees(rz));
		VertexConsumer vc = buf.getBuffer(RenderType.lines());
		for (int i = 0; i < 360; i += 60) {
			pose.pushPose();
			pose.mulPose(Axis.XP.rotationDegrees(i));
			for (int k = 0; k < X.length-1; k++)
				line(vc, pose, (float)X[k], k, 0, (float)X[k+1], k+1, 0);
			pose.popPose();
		}
		for (int i = 1; i < X.length; i++) { //rings of radius i at x = X[i], around the x axis
			for (int a = 0; a < 360; a += 15) {
				double r0 = Math.toRadians(a), r1 = Math.toRadians(a+15);
				line(vc, pose, (float)X[i], (float)(i*Math.cos(r0)), (float)(i*Math.sin(r0)), (float)X[i], (float)(i*Math.cos(r1)), (float)(i*Math.sin(r1)));
			}
		}
		pose.popPose();
	}

	private static void line(VertexConsumer vc, PoseStack pose, float x0, float y0, float z0, float x1, float y1, float z1) {
		PoseStack.Pose p = pose.last();
		float nx = x1-x0, ny = y1-y0, nz = z1-z0;
		float len = (float)Math.sqrt(nx*nx+ny*ny+nz*nz);
		if (len == 0)
			return;
		vc.addVertex(p, x0, y0, z0).setColor(COLOR).setNormal(p, nx/len, ny/len, nz/len);
		vc.addVertex(p, x1, y1, z1).setColor(COLOR).setNormal(p, nx/len, ny/len, nz/len);
	}

	@Override
	public ResourceLocation getTextureLocation(SonicShotEntity e) {
		return TextureAtlas.LOCATION_BLOCKS;
	}
}
