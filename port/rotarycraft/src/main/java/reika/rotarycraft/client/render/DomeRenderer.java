/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

import org.joml.Matrix4f;

import reika.rotarycraft.content.blockentity.DomeBlockEntity;

/** Own rendering: the dome as a faint translucent sphere (latitude/longitude quads) in the field's colour. */
public class DomeRenderer<T extends DomeBlockEntity> implements BlockEntityRenderer<T> {

	private static final int SEG = 24;
	private final int argb;

	public DomeRenderer(BlockEntityRendererProvider.Context ctx, int argb) {
		this.argb = argb;
	}

	@Override
	public void render(T te, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		int r = te.getRange();
		if (r <= 0)
			return;
		VertexConsumer vc = buf.getBuffer(RenderType.lightning());
		Matrix4f m = pose.last().pose();
		int a = argb >>> 24, red = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
		for (int i = 0; i < SEG; i++) {
			double t0 = Math.PI*i/SEG, t1 = Math.PI*(i+1)/SEG;
			for (int k = 0; k < SEG; k++) {
				double p0 = 2*Math.PI*k/SEG, p1 = 2*Math.PI*(k+1)/SEG;
				v(vc, m, r, t0, p0, red, g, b, a);
				v(vc, m, r, t1, p0, red, g, b, a);
				v(vc, m, r, t1, p1, red, g, b, a);
				v(vc, m, r, t0, p1, red, g, b, a);
			}
		}
	}

	private static void v(VertexConsumer vc, Matrix4f m, double r, double theta, double phi, int red, int g, int b, int a) {
		float x = (float)(0.5+r*Math.sin(theta)*Math.cos(phi)), y = (float)(0.5+r*Math.cos(theta)), z = (float)(0.5+r*Math.sin(theta)*Math.sin(phi));
		vc.addVertex(m, x, y, z).setColor(red, g, b, a);
	}

	@Override
	public boolean shouldRenderOffScreen(T te) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 160;
	}

	@Override
	public AABB getRenderBoundingBox(T te) {
		return AABB.INFINITE;
	}
}
