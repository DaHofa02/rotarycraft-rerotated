/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import reika.rotarycraft.content.piping.PipingBlockEntity;

/**
 * Original: PipeBodyRenderer.renderLiquid (render pass 1). The fluid fills the pipe's core, 0.01 inside the frame, and runs out
 * to the block edge on every connected side; luminous fluids are drawn full-bright. The pipe body itself is the block model.
 */
public class PipeFluidRenderer<T extends net.minecraft.world.level.block.entity.BlockEntity & reika.rotarycraft.content.piping.FluidPipeView> implements BlockEntityRenderer<T> {

	private static final float size = 0.75F/2F;
	private static final float in = 0.5F+size-0.01F;
	private static final float in2 = 0.5F-size+0.01F;

	public PipeFluidRenderer(BlockEntityRendererProvider.Context ctx) {}

	@Override
	public void render(T te, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		Fluid f = te.getFluidType();
		if (f == null || te.getFluidLevel() <= 0 || te.getLevel() == null)
			return;
		FluidState fs = f.defaultFluidState();
		IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fs);
		TextureAtlasSprite ico = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fs, te.getLevel(), te.getBlockPos()));
		int clr = ext.getTintColor(fs, te.getLevel(), te.getBlockPos());
		if (f.getFluidType().getLightLevel() > 0)
			light = LightTexture.FULL_BRIGHT;
		VertexConsumer vc = buf.getBuffer(Sheets.translucentCullBlockSheet());
		Matrix4f m = pose.last().pose();
		for (Direction dir : Direction.values()) {
			if (!te.isConnectedDirectly(dir)) {
				face(vc, m, ico, clr, light, dir, in2, in, in2, in, dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? in : in2);
			}
			else {
				float a = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? in : 0;
				float b = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : in2;
				for (Direction wall : Direction.values()) {
					if (wall.getAxis() == dir.getAxis())
						continue;
					float plane = wall.getAxisDirection() == Direction.AxisDirection.POSITIVE ? in : in2;
					//the wall spans [a,b] along dir's axis and [in2,in] along the remaining axis
					Direction.Axis other = third(dir.getAxis(), wall.getAxis());
					float[] lo = new float[3];
					float[] hi = new float[3];
					lo[dir.getAxis().ordinal()] = a;
					hi[dir.getAxis().ordinal()] = b;
					lo[other.ordinal()] = in2;
					hi[other.ordinal()] = in;
					wall(vc, m, ico, clr, light, wall, lo, hi, plane);
				}
			}
		}
	}

	private static Direction.Axis third(Direction.Axis a, Direction.Axis b) {
		for (Direction.Axis ax : Direction.Axis.values())
			if (ax != a && ax != b)
				return ax;
		return a;
	}

	/** A square face of the core on side {@code dir}, at {@code plane}. */
	private static void face(VertexConsumer vc, Matrix4f m, TextureAtlasSprite ico, int clr, int light, Direction dir, float min1, float max1, float min2, float max2, float plane) {
		float[] lo = {min1, min1, min1};
		float[] hi = {max1, max1, max1};
		wall(vc, m, ico, clr, light, dir, lo, hi, plane);
	}

	/** A quad facing {@code dir}, in the plane {@code plane} of dir's axis, spanning lo..hi on the other two axes. */
	private static void wall(VertexConsumer vc, Matrix4f m, TextureAtlasSprite ico, int clr, int light, Direction dir, float[] lo, float[] hi, float plane) {
		Direction.Axis ax = dir.getAxis();
		int u = ax == Direction.Axis.X ? 2 : 0; //first in-plane axis: z for X faces, x otherwise
		int v = ax == Direction.Axis.Y ? 2 : 1; //second in-plane axis: z for Y faces, y otherwise
		float[][] c = new float[4][3];
		float[][] uv = {{lo[u], lo[v]}, {hi[u], lo[v]}, {hi[u], hi[v]}, {lo[u], hi[v]}};
		for (int i = 0; i < 4; i++) {
			c[i][ax.ordinal()] = plane;
			c[i][u] = uv[i][0];
			c[i][v] = uv[i][1];
		}
		//wind counter-clockwise when seen from the side the quad faces (0123 faces -Y, -X and +Z)
		boolean flip = (dir.getAxisDirection() == Direction.AxisDirection.POSITIVE) == (ax == Direction.Axis.Z);
		int[] order = flip ? new int[]{0, 1, 2, 3} : new int[]{3, 2, 1, 0};
		int r = (clr >> 16) & 255, g = (clr >> 8) & 255, b = clr & 255, a = (clr >>> 24) & 255;
		for (int i : order) {
			vc.addVertex(m, c[i][0], c[i][1], c[i][2]).setColor(r, g, b, a)
					.setUv(ico.getU(uv[i][0]), ico.getV(uv[i][1])).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
					.setNormal(dir.getStepX(), dir.getStepY(), dir.getStepZ());
		}
	}
}
