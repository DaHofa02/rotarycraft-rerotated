/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.api.power.ShaftPowerEmitter;
import reika.rotarycraft.api.power.ShaftPowerReceiver;
import reika.rotarycraft.content.blockentity.IOMachineBlockEntity;

/**
 * Original: Reika.RotaryCraft.Auxiliary.IORenderer, called by every machine renderer in world render pass 1. After a screwdriver or
 * Angular Transducer click (iotick = 512, falling by 8 per tick) the machine's output side is outlined red and its input sides green
 * (colour-blind mode: blue and magenta). Here it wraps the machine's renderer. Not ported: the I/O goggles (not ported yet) and the
 * special cases of machines that do not exist yet (distribution clutch, winder).
 */
public class IORenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

	private static final double expand = 0.5;

	private final BlockEntityRenderer<T> parent;

	public IORenderer(BlockEntityRenderer<T> parent) {
		this.parent = parent;
	}

	@Override
	public void render(T te, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		parent.render(te, partial, pose, buf, light, overlay);
		if (te.getLevel() != null)
			renderIO(te, pose, buf);
	}

	@Override
	public AABB getRenderBoundingBox(T te) {
		return parent.getRenderBoundingBox(te).minmax(new AABB(te.getBlockPos()).inflate(1.0625));
	}

	@Override
	public boolean shouldRenderOffScreen(T te) {
		return parent.shouldRenderOffScreen(te);
	}

	@Override
	public int getViewDistance() {
		return parent.getViewDistance();
	}

	@Override
	public boolean shouldRender(T te, Vec3 cam) {
		return parent.shouldRender(te, cam);
	}

	public static void renderOut(PoseStack pose, MultiBufferSource buf, double x, double y, double z, int a) {
		int[] color = {255, 0, 0, a};
		if (RotaryConfig.COLORBLIND.get())
			color[0] = 0;
		renderBox(pose, buf, x, y, z, color);
	}

	public static void renderIn(PoseStack pose, MultiBufferSource buf, double x, double y, double z, int a) {
		int[] color = {0, 255, 0, a};
		if (RotaryConfig.COLORBLIND.get()) {
			color[0] = 255;
			color[2] = 255;
		}
		renderBox(pose, buf, x, y, z, color);
	}

	private static void out(PoseStack pose, MultiBufferSource buf, Direction d, int a) {
		if (d != null)
			renderOut(pose, buf, d.getStepX(), d.getStepY(), d.getStepZ(), a);
	}

	private static void in(PoseStack pose, MultiBufferSource buf, Direction d, BlockPos po, int a) {
		if (d != null)
			renderIn(pose, buf, d.getStepX()+po.getX(), d.getStepY()+po.getY(), d.getStepZ()+po.getZ(), a);
	}

	public static void renderIO(BlockEntity teb, PoseStack pose, MultiBufferSource buf) {
		if (teb instanceof IOMachineBlockEntity te) {
			if (te.iotick <= 0)
				return;
			int a = te.iotick;
			BlockPos po = te.getPointingOffset();
			if (te.isOmniSided) {
				//Original: one box per side where the machine's block bounds reach the block face; all ported machines are full blocks.
				for (Direction d : Direction.values())
					renderIn(pose, buf, po.getX()+d.getStepX(), po.getY()+d.getStepY(), po.getZ()+d.getStepZ(), a);
				return;
			}
			out(pose, buf, te.getWriteDirection(), a);
			out(pose, buf, te.getWriteDirection2(), a);
			in(pose, buf, te.getReadDirection(), po, a);
			in(pose, buf, te.getReadDirection2(), po, a);
			in(pose, buf, te.getReadDirection3(), po, a);
			in(pose, buf, te.getReadDirection4(), po, a);
		}
		else {
			if (teb instanceof ShaftPowerReceiver sr) {
				int io = sr.getIORenderAlpha();
				if (io <= 0)
					return;
				for (Direction dir : Direction.values()) {
					if (sr.canReadFrom(dir))
						renderIn(pose, buf, dir.getStepX(), dir.getStepY(), dir.getStepZ(), io);
				}
			}
			if (teb instanceof ShaftPowerEmitter se) {
				int io = se.getIORenderAlpha();
				if (io <= 0)
					return;
				for (Direction dir : Direction.values()) {
					if (se.canWriteTo(dir))
						renderOut(pose, buf, dir.getStepX(), dir.getStepY(), dir.getStepZ(), io);
				}
			}
		}
	}

	private static void line(VertexConsumer vc, PoseStack.Pose p, double x1, double y1, double z1, double x2, double y2, double z2, int[] c) {
		float nx = (float)(x2-x1), ny = (float)(y2-y1), nz = (float)(z2-z1);
		float l = (float)Math.sqrt(nx*nx+ny*ny+nz*nz);
		if (l > 0) {
			nx /= l;
			ny /= l;
			nz /= l;
		}
		vc.addVertex(p, (float)x1, (float)y1, (float)z1).setColor(c[0], c[1], c[2], c[3]).setNormal(p, nx, ny, nz);
		vc.addVertex(p, (float)x2, (float)y2, (float)z2).setColor(c[0], c[1], c[2], c[3]).setNormal(p, nx, ny, nz);
	}

	private static void renderBox(PoseStack pose, MultiBufferSource buf, double x, double y, double z, int[] color) {
		if (color[3] > 255)
			color[3] = 255;
		double e = 0.0625*expand;
		double x0 = x-e, x1 = x+1+e, y0 = y-e, y1 = y+1+e, z0 = z-e, z1 = z+1+e;
		PoseStack.Pose p = pose.last();

		VertexConsumer l = buf.getBuffer(RenderType.lines());
		//top and bottom loops
		for (double yy : new double[] {y1, y0}) {
			line(l, p, x1, yy, z0, x0, yy, z0, color);
			line(l, p, x0, yy, z0, x0, yy, z1, color);
			line(l, p, x0, yy, z1, x1, yy, z1, color);
			line(l, p, x1, yy, z1, x1, yy, z0, color);
		}
		//vertical edges
		line(l, p, x0, y0, z0, x0, y1, z0, color);
		line(l, p, x1, y0, z0, x1, y1, z0, color);
		line(l, p, x1, y0, z1, x1, y1, z1, color);
		line(l, p, x0, y0, z1, x0, y1, z1, color);

		VertexConsumer q = buf.getBuffer(Types.IO_FILL);
		Matrix4f m = p.pose();
		int r = color[0], g = color[1], b = color[2], a = (int)(color[3]*0.375);
		double[][] quads = {
				{x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1},
				{x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1},
				{x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1},
				{x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1},
				{x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0},
				{x1, y1, z0, x0, y1, z0, x0, y1, z1, x1, y1, z1},
		};
		for (double[] f : quads) {
			for (int i = 0; i < 12; i += 3)
				q.addVertex(m, (float)f[i], (float)f[i+1], (float)f[i+2]).setColor(r, g, b, a);
		}
	}

	/** Translucent untextured quads with back-face culling and no depth writes (original: blend on, cull face on, no texture). */
	private static final class Types extends RenderType {

		static final RenderType IO_FILL = create("rotarycraft_io_fill", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 1536, false, true,
				CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(CULL)
				.setWriteMaskState(COLOR_WRITE).setLayeringState(VIEW_OFFSET_Z_LAYERING).createCompositeState(false));

		private Types(String n, VertexFormat f, VertexFormat.Mode m, int s, boolean c, boolean so, Runnable a, Runnable b) {
			super(n, f, m, s, c, so, a, b);
		}
	}
}
