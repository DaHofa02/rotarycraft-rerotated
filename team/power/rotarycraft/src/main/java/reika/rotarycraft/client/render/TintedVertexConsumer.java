/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Multiplies every vertex colour by a fixed colour - the port's stand-in for GL11.glColor4f before drawing a model. */
public class TintedVertexConsumer implements VertexConsumer {

	private final VertexConsumer parent;
	private final float r, g, b, a;

	public TintedVertexConsumer(VertexConsumer parent, int rgb, float alpha) {
		this.parent = parent;
		r = ((rgb >> 16) & 0xff)/255F;
		g = ((rgb >> 8) & 0xff)/255F;
		b = (rgb & 0xff)/255F;
		a = alpha;
	}

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		parent.addVertex(x, y, z);
		return this;
	}

	@Override
	public VertexConsumer setColor(int red, int green, int blue, int alpha) {
		parent.setColor((int)(red*r), (int)(green*g), (int)(blue*b), (int)(alpha*a));
		return this;
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		parent.setUv(u, v);
		return this;
	}

	@Override
	public VertexConsumer setUv1(int u, int v) {
		parent.setUv1(u, v);
		return this;
	}

	@Override
	public VertexConsumer setUv2(int u, int v) {
		parent.setUv2(u, v);
		return this;
	}

	@Override
	public VertexConsumer setNormal(float x, float y, float z) {
		parent.setNormal(x, y, z);
		return this;
	}
}
