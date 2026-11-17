/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import java.util.List;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.AABB;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelDisplay;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.DisplayBlockEntity;

/**
 * Original: Reika.RotaryCraft.Renders.M.RenderDisplay - the projector model, and while it runs (coil, free space above) the screen: a
 * translucent 5x3 pane (alpha 96) with a solid 1/32 border and a faint grid (alpha 32), and the message in white, wrapped to 27 character
 * widths, scrolling 12 lines at a time.
 */
public class DisplayRenderer extends MachineRenderer<DisplayBlockEntity> {

	private final ModelDisplay model;

	public DisplayRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelDisplay(ctx.bakeLayer(RotaryModelLayers.DISPLAY));
	}

	@Override
	public void render(DisplayBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		int meta = isInWorld(tile) ? tile.getBlockMetadata() : 0;
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		int var11 = switch (meta) {
			case 1 -> 180;
			case 2 -> 90;
			case 3 -> 270;
			default -> 0;
		};
		pose.mulPose(TechneModel.rotation(var11+90, 0, 1, 0));
		model.renderAll(pose, consumer(buf, tex("displaytex.png")), light, overlay, tile, null, -tile.phi, 0);
		pose.popPose();
		if (isInWorld(tile) && tile.canDisplay() && tile.hasSpace())
			this.renderScreen(tile, meta, pose, buf);
	}

	private void renderScreen(DisplayBlockEntity tile, int meta, PoseStack pose, MultiBufferSource buf) {
		int dir = 0, dx = 0, dz = 0;
		switch (meta) {
			case 0 -> {
				dir = 270;
				dx = 1;
			}
			case 1 -> {
				dir = 90;
				dz = 1;
			}
			case 2 -> {
				dir = 180;
				dz = 1;
				dx = 1;
			}
			default -> dir = 0;
		}
		pose.pushPose();
		pose.translate(dx, 0, dz);
		pose.mulPose(Axis.YP.rotationDegrees(dir));
		pose.translate(0, 0, 0.495);
		int[] c = tile.getScreenColor(), bc = tile.getBorderColor();
		VertexConsumer v = buf.getBuffer(RenderType.debugQuads());
		Matrix4f m = pose.last().pose();
		quad(v, m, -2, 1, 3, 4, 0, c, 96);
		float dd = 0.03125F, z = 0.0005F;
		quad(v, m, -2, 4-dd, 3, 4, z, bc, 255);
		quad(v, m, -2, 1, 3, 1+dd, z, bc, 255);
		quad(v, m, 3-dd, 1, 3, 4, z, bc, 255);
		quad(v, m, -2, 1, -2+dd, 4, z, bc, 255);
		VertexConsumer l = buf.getBuffer(RenderType.lines());
		for (float k = 1.0625F; k < 4; k += 0.0625F)
			line(l, pose, -2, k, 3, k, z, bc);
		for (float k = -1.75F; k < 3; k += 0.25F)
			line(l, pose, k, 4, k, 1, z, bc);
		if (tile.hasList())
			this.renderText(tile, pose, buf);
		pose.popPose();
	}

	private static void quad(VertexConsumer v, Matrix4f m, float x0, float y0, float x1, float y1, float z, int[] c, int a) {
		v.addVertex(m, x0, y0, z).setColor(c[0], c[1], c[2], a);
		v.addVertex(m, x1, y0, z).setColor(c[0], c[1], c[2], a);
		v.addVertex(m, x1, y1, z).setColor(c[0], c[1], c[2], a);
		v.addVertex(m, x0, y1, z).setColor(c[0], c[1], c[2], a);
	}

	private static void line(VertexConsumer l, PoseStack pose, float x0, float y0, float x1, float y1, float z, int[] c) {
		PoseStack.Pose p = pose.last();
		float nx = x1-x0, ny = y1-y0;
		float len = (float)Math.sqrt(nx*nx+ny*ny);
		l.addVertex(p, x0, y0, z).setColor(c[0], c[1], c[2], 32).setNormal(p, nx/len, ny/len, 0);
		l.addVertex(p, x1, y1, z).setColor(c[0], c[1], c[2], 32).setNormal(p, nx/len, ny/len, 0);
	}

	private void renderText(DisplayBlockEntity tile, PoseStack pose, MultiBufferSource buf) {
		Font f = Minecraft.getInstance().font;
		List<FormattedCharSequence> cache = f.split(FormattedText.of(tile.getMessage()), DisplayBlockEntity.DISPLAY_WIDTH*f.lineHeight);
		pose.pushPose();
		pose.scale(1, -1, 1);
		pose.scale(0.02F, 0.02F, 0.02F);
		pose.translate(0, -50-12*12, 25);
		pose.translate(-(100-10/4), 0, 0);
		long core = tile.getLevel().getGameTime();
		float scroll = cache.size() > 12 ? (core*4%(180L*cache.size()))/180F : 0;
		int linescroll = scroll-(int)scroll > 0.5F ? (int)scroll+1 : (int)scroll;
		int len = Math.min(cache.size()-1, 12+linescroll-1);
		for (int i = linescroll; i < len+1; i++)
			f.drawInBatch(cache.get(i), 1, -1+(int)((i-scroll)*12), 0xFFFFFF, false, pose.last().pose(), buf, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen(DisplayBlockEntity te) {
		return true;
	}

	@Override
	public AABB getRenderBoundingBox(DisplayBlockEntity te) {
		return new AABB(te.getBlockPos()).inflate(3, 0, 3).expandTowards(0, 4, 0);
	}
}
