/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.client;

import java.util.Set;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import reika.reactorcraft.content.item.IronFinderItem;
import reika.reactorcraft.registry.ReactorItems;

/**
 * Original: Reika.ReactorCraft.Auxiliary.IronFinderOverlay (RenderGameOverlayEvent, HELMET layer) - while the iron finder is held, every
 * magnetic ore within 8 blocks is shown on the HUD with its top texture: behind the player on the left or right edge as an arrow, in
 * front of the player as a box at its direction (the original's horizontal and vertical screen formulas, unchanged).
 */
public final class IronFinderOverlay {

	private IronFinderOverlay() {}

	/** Original ReikaRenderHelper.getRealFOV. */
	private static int getRealFOV() {
		float base = Minecraft.getInstance().options.fov().get();
		float diff = ((-40+70-base)/40F)*15F;
		return (int)(base+diff);
	}

	public static void render(GuiGraphics g, DeltaTracker dt) {
		Minecraft mc = Minecraft.getInstance();
		Player ep = mc.player;
		if (ep == null || mc.options.hideGui || !ep.getMainHandItem().is(ReactorItems.IRON_FINDER.get()))
			return;
		int h = g.guiHeight()/2;
		float yaw = ep.getYHeadRot()%360;
		float pitch = ep.getXRot()+90;
		if (yaw < 0)
			yaw += 360;
		int fov = getRealFOV();
		double dfv = mc.options.fov().get()/70F;
		Set<BlockPos> map = IronFinderItem.getOreNearby(ep, 8);
		for (BlockPos c : map) {
			double dx = c.getX()+0.5-ep.getX();
			double dy = c.getY()+0.5-ep.getY();
			double dz = c.getZ()+0.5-ep.getZ();
			BlockState s = ep.level().getBlockState(c);
			if (s.isAir())
				continue;
			TextureAtlasSprite ico = topIcon(mc, s);

			double dl = Math.sqrt(dx*dx+dz*dz);
			double arel = -Math.toDegrees(Math.atan2(dx, dz));
			double prel = 90-Math.toDegrees(Math.atan2(dy, dl));
			if (arel < 0)
				arel += 360;
			double phi = arel-yaw;
			double theta = prel-pitch;
			if (phi < 0)
				phi += 360;
			int cy = h+(int)(h*2*Math.sin(Math.toRadians(theta)))+(int)(dfv*20);
			if (phi >= 180 && 360-fov > phi) {
				int cx = 10;
				triangle(g, cx+10, cy+10, cx+10, cy-10, cx, cy);
				g.blit(cx+10, cy-8, 0, 16, 16, ico);
			}
			else if (phi < 180 && phi > fov) {
				int cx = g.guiWidth()-10;
				triangle(g, cx, cy, cx-10, cy-10, cx-10, cy+10);
				g.blit(cx-26, cy-8, 0, 16, 16, ico);
			}
			else {
				double w = g.guiWidth()/2D;
				w *= dfv;
				int cx = (int)(w+w*Math.sin(Math.toRadians(phi)));
				g.fill(cx-8, cy-8, cx+8, cy+8, 0x20FFFFFF);
				outline(g, new float[] {cx-8, cy+8, cx+8, cy+8, cx+8, cy-8, cx-8, cy-8});
				g.blit(cx-8, cy-8, 0, 16, 16, ico);
			}
		}
	}

	/** Original b.getIcon(1, meta): the top face texture. */
	private static TextureAtlasSprite topIcon(Minecraft mc, BlockState s) {
		BakedModel m = mc.getBlockRenderer().getBlockModel(s);
		var quads = m.getQuads(s, Direction.UP, RandomSource.create(42));
		return quads.isEmpty() ? m.getParticleIcon() : quads.get(0).getSprite();
	}

	/** Original: a translucent white triangle (alpha 96) with an opaque white outline. */
	private static void triangle(GuiGraphics g, float x1, float y1, float x2, float y2, float x3, float y3) {
		Matrix4f m = g.pose().last().pose();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		b.addVertex(m, x1, y1, 0).setColor(0x60FFFFFF);
		b.addVertex(m, x2, y2, 0).setColor(0x60FFFFFF);
		b.addVertex(m, x3, y3, 0).setColor(0x60FFFFFF);
		BufferUploader.drawWithShader(b.buildOrThrow());
		outline(g, new float[] {x3, y3, x2, y2, x1, y1});
	}

	private static void outline(GuiGraphics g, float[] pts) {
		Matrix4f m = g.pose().last().pose();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i <= pts.length/2; i++) {
			int k = (i%(pts.length/2))*2;
			b.addVertex(m, pts[k], pts[k+1], 0).setColor(0xFFFFFFFF);
		}
		BufferUploader.drawWithShader(b.buildOrThrow());
		RenderSystem.disableBlend();
	}
}
