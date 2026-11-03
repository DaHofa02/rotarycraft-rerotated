/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

import reika.rotarycraft.content.entity.DischargeEntity;

/**
 * Original: Reika.RotaryCraft.Entities.RenderDischarge - a jagged line of 20 randomly jittered points (0.1 blocks) from the generator to
 * the target, in the colour of the current.
 */
public class DischargeRenderer extends EntityRenderer<DischargeEntity> {

	public DischargeRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		shadowRadius = 0;
	}

	@Override
	public void render(DischargeEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
		int c = 0xFF000000 | e.getColor();
		float x = (float)(e.targetX-e.getX());
		float y = (float)(e.targetY-e.getY());
		float z = (float)(e.targetZ-e.getZ());
		Random r = new Random();
		int s = 20;
		float[][] pts = new float[s+2][];
		pts[0] = new float[] {0, 0, 0};
		for (int i = 0; i < s; i++) {
			float[] a = {x*i/s, y*i/s, z*i/s};
			for (int k = 0; k < 3; k++)
				a[k] += (float)((r.nextDouble()-0.5)*0.1+r.nextDouble()*0.1);
			pts[i+1] = a;
		}
		pts[s+1] = new float[] {x, y, z};
		VertexConsumer vc = buf.getBuffer(RenderType.lines());
		var m = pose.last();
		for (int i = 0; i < pts.length-1; i++) {
			float[] a = pts[i], b = pts[i+1];
			float nx = b[0]-a[0], ny = b[1]-a[1], nz = b[2]-a[2];
			float len = (float)Math.sqrt(nx*nx+ny*ny+nz*nz);
			if (len == 0)
				continue;
			vc.addVertex(m, a[0], a[1], a[2]).setColor(c).setNormal(m, nx/len, ny/len, nz/len);
			vc.addVertex(m, b[0], b[1], b[2]).setColor(c).setNormal(m, nx/len, ny/len, nz/len);
		}
	}

	@Override
	public boolean shouldRender(DischargeEntity e, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) {
		return true;
	}

	@Override
	public ResourceLocation getTextureLocation(DischargeEntity e) {
		return TextureAtlas.LOCATION_BLOCKS;
	}
}
