/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

import reika.rotarycraft.client.model.ModelBelt;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.BeltHubBlockEntity;

/**
 * Original: Reika.RotaryCraft.Renders.DM.RenderBelt - the hub model turned by its metadata (0-5, the splitting metadata 6-11 look the
 * same), plus drawBelt2: the belt in the hub's colour wrapped round the wheel and, from the receiving end, the two strands to the other
 * hub (untextured, unlit, both faces).
 */
public class BeltHubRenderer<T extends BeltHubBlockEntity> extends MachineRenderer<T> {

	private final ModelBelt model;

	public BeltHubRenderer(BlockEntityRendererProvider.Context ctx, ModelLayerLocation layer) {
		model = new ModelBelt(ctx.bakeLayer(layer));
	}

	@Override
	public void render(T tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (isInWorld(tile)) {
			int meta = tile.getBlockMetadata()%6;
			int var11 = switch (meta) {
				case 1 -> 180;
				case 2, 4 -> 270;
				case 3, 5 -> 90;
				default -> 0;
			};
			if (meta <= 3)
				pose.mulPose(TechneModel.rotation(var11+90, 0, 1, 0));
			else {
				pose.mulPose(TechneModel.rotation(var11, 1, 0, 0));
				pose.translate(0, -1, 1);
				if (meta == 5)
					pose.translate(0, 0, -2);
			}
		}
		model.renderAll(pose, consumer(buf, tex("transmission/belttex.png")), light, overlay, tile, null, tile.phi, 0);
		pose.popPose();
		if (isInWorld(tile) && tile.hasValidConnection())
			this.drawBelt2(tile, pose, buf);
	}

	private void drawBelt2(T tile, PoseStack pose, MultiBufferSource buf) {
		int meta = tile.getBlockMetadata();
		boolean vertical = meta == 4 || meta == 5 || meta == 10 || meta == 11;
		Direction dir = tile.getBeltDirection();
		int dist = tile.getDistanceToTarget();
		double rx = 0, ry = 0, rz = 0;
		switch (dir) {
			case WEST -> {
				if (vertical)
					rx = 90;
				ry = 180;
			}
			case EAST -> {
				if (vertical)
					rx = 90;
			}
			case NORTH -> {
				if (vertical) {
					rz = 90;
					rx = -90;
				}
				else
					ry = 90;
			}
			case SOUTH -> {
				if (vertical) {
					rz = 90;
					rx = 90;
				}
				else
					ry = -90;
			}
			case UP -> {
				if (meta != 0 && meta != 1)
					rz = 90;
				else {
					ry = 90;
					rz = 90;
				}
			}
			case DOWN -> {
				if (meta != 0 && meta != 1)
					rz = -90;
				else {
					ry = 90;
					rz = -90;
				}
			}
		}
		pose.pushPose();
		pose.translate(0.5, 0.5, 0.5);
		pose.mulPose(Axis.XP.rotationDegrees((float)rx));
		pose.mulPose(Axis.YP.rotationDegrees((float)ry));
		pose.mulPose(Axis.ZP.rotationDegrees((float)rz));
		pose.translate(-0.5, -0.5, -0.5);
		VertexConsumer v = buf.getBuffer(Types.BELT);
		Matrix4f m = pose.last().pose();
		int c = tile.getBeltColor();
		int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
		float far = 0.625F+dist;
		float[][] q = {
				{0.125F, 0.375F, 0.375F}, {0.125F, 0.625F, 0.375F}, {0.125F, 0.625F, 0.625F}, {0.125F, 0.375F, 0.625F},
				{0.125F, 0.625F, 0.625F}, {0.375F, 0.875F, 0.625F}, {0.375F, 0.875F, 0.375F}, {0.125F, 0.625F, 0.375F},
				{0.125F, 0.375F, 0.625F}, {0.375F, 0.125F, 0.625F}, {0.375F, 0.125F, 0.375F}, {0.125F, 0.375F, 0.375F},
				{0.375F, 0.875F, 0.375F}, {far, 0.875F, 0.375F}, {far, 0.875F, 0.625F}, {0.375F, 0.875F, 0.625F},
				{0.375F, 0.125F, 0.375F}, {far, 0.125F, 0.375F}, {far, 0.125F, 0.625F}, {0.375F, 0.125F, 0.625F}};
		int n = tile.isReceivingEnd ? q.length : 12;
		for (int i = 0; i < n; i++)
			v.addVertex(m, q[i][0], q[i][1], q[i][2]).setColor(r, g, b, 255);
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen(T te) {
		return true;
	}

	@Override
	public AABB getRenderBoundingBox(T te) {
		BlockPos o = te.getConnection();
		AABB box = new AABB(te.getBlockPos()).inflate(1);
		return o == null ? box : box.minmax(new AABB(o));
	}

	/** Opaque untextured quads without culling or lighting (original: texture, lighting and face culling off). */
	private static final class Types extends RenderType {

		static final RenderType BELT = create("rotarycraft_belt", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 1536, false, false,
				CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setCullState(NO_CULL).createCompositeState(false));

		private Types(String n, VertexFormat f, VertexFormat.Mode md, int s, boolean c, boolean so, Runnable a, Runnable b) {
			super(n, f, md, s, c, so, a, b);
		}
	}
}
