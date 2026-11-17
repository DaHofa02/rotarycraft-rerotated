/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import java.awt.Color;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelCave;
import reika.rotarycraft.content.blockentity.CaveScannerBlockEntity;

/**
 * Original: Reika.RotaryCraft.Renders.M.RenderCaveFinder - the scanner model, and while it runs every mapped corner as a small dot
 * (the original: 2-pixel GL points) coloured by height (HSB hue ((|y| - 12) mod 64) / 64), drawn through walls, plus the white outline
 * of the scanned cube. The original kept the points in a display list; here the dot list is rebuilt when the map changes.
 */
public class CaveScannerRenderer extends MachineRenderer<CaveScannerBlockEntity> {

	private static final float D = 0.0625F;

	private final ModelCave model;
	private float[] cache = new float[0];
	private int cacheSize;
	private CaveScannerBlockEntity cacheFor;
	private int cacheVersion = -1;

	public CaveScannerRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelCave(ctx.bakeLayer(RotaryModelLayers.CAVE_SCANNER));
	}

	@Override
	public void render(CaveScannerBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		model.renderAll(pose, consumer(buf, tex("cavetex.png")), light, overlay, tile, null, 0, 0);
		pose.popPose();
		if (isInWorld(tile) && tile.on)
			this.renderPoints(tile, pose, buf);
	}

	private void rebuild(CaveScannerBlockEntity te) {
		int r = te.getRange();
		BlockPos s = te.getSource(), o = te.getBlockPos();
		float[] out = new float[(2*r+1)*(2*r+1)*(2*r+1)*6];
		int n = 0;
		for (int i = -r; i <= r; i++)
			for (int j = -r; j <= r; j++)
				for (int k = -r; k <= r; k++)
					if (te.hasPointAt(i+r, j+r, k+r)) {
						int y = s.getY()+j;
						int c = Color.HSBtoRGB((float)((Math.abs(y)-12)%64)/64F, 1, 1);
						out[n++] = s.getX()+i-o.getX();
						out[n++] = y-o.getY();
						out[n++] = s.getZ()+k-o.getZ();
						out[n++] = ((c >> 16) & 255)/255F;
						out[n++] = ((c >> 8) & 255)/255F;
						out[n++] = (c & 255)/255F;
					}
		cache = out;
		cacheSize = n;
		cacheFor = te;
		cacheVersion = te.version;
	}

	private void renderPoints(CaveScannerBlockEntity te, PoseStack pose, MultiBufferSource buf) {
		if (cacheFor != te || cacheVersion != te.version)
			this.rebuild(te);
		VertexConsumer v = buf.getBuffer(Types.POINTS);
		Matrix4f m = pose.last().pose();
		for (int i = 0; i < cacheSize; i += 6) {
			float x = cache[i], y = cache[i+1], z = cache[i+2];
			float r = cache[i+3], g = cache[i+4], b = cache[i+5];
			v.addVertex(m, x-D, y, z).setColor(r, g, b, 1);
			v.addVertex(m, x+D, y, z).setColor(r, g, b, 1);
			v.addVertex(m, x+D, y+2*D, z).setColor(r, g, b, 1);
			v.addVertex(m, x-D, y+2*D, z).setColor(r, g, b, 1);
			v.addVertex(m, x, y, z-D).setColor(r, g, b, 1);
			v.addVertex(m, x, y, z+D).setColor(r, g, b, 1);
			v.addVertex(m, x, y+2*D, z+D).setColor(r, g, b, 1);
			v.addVertex(m, x, y+2*D, z-D).setColor(r, g, b, 1);
		}
		int rg = te.getRange();
		BlockPos s = te.getSource().subtract(te.getBlockPos());
		AABB box = new AABB(s.getX()-rg, s.getY()-rg, s.getZ()-rg, s.getX()+1+rg, s.getY()+1+rg, s.getZ()+1+rg);
		LevelRenderer.renderLineBox(pose, buf.getBuffer(RenderType.lines()), box, 1, 1, 1, 1);
	}

	@Override
	public boolean shouldRenderOffScreen(CaveScannerBlockEntity te) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 256;
	}

	@Override
	public AABB getRenderBoundingBox(CaveScannerBlockEntity te) {
		return new AABB(te.getSource()).inflate(te.getRange()+1).minmax(new AABB(te.getBlockPos()));
	}

	/** Untextured unlit dots without culling and without depth test (original: GL_DEPTH_TEST off). */
	private static final class Types extends RenderType {

		static final RenderType POINTS = create("rotarycraft_cave_points", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 262144, false, false,
				CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setCullState(NO_CULL).setDepthTestState(NO_DEPTH_TEST).createCompositeState(false));

		private Types(String n, VertexFormat f, VertexFormat.Mode md, int s, boolean c, boolean so, Runnable a, Runnable b) {
			super(n, f, md, s, c, so, a, b);
		}
	}
}
