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
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelReservoir;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.ReservoirBlockEntity;

/**
 * Original: Reika.RotaryCraft.Renders.M.RenderReservoir - the floor and the walls towards non-joined sides, a glass cover at 0.99, and
 * the fluid surface at 1/16 + 14/16 of the fill level, each corner averaged with the joined neighbours of the same fluid so that a
 * group of reservoirs shows one smooth surface; luminous fluids are drawn full-bright.
 */
public class ReservoirRenderer extends MachineRenderer<ReservoirBlockEntity> {

	private static final ResourceLocation GLASS = ResourceLocation.withDefaultNamespace("block/glass");

	private final ModelReservoir model;

	public ReservoirRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelReservoir(ctx.bakeLayer(RotaryModelLayers.RESERVOIR));
	}

	@Override
	public void render(ReservoirBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		VertexConsumer vc = consumer(buf, MachineRenderer.tex("reservoirtex.png"));
		if (isInWorld(tile)) {
			pose.mulPose(TechneModel.rotation(90, 0.0F, 1.0F, 0.0F)); //meta 0: var11+90
			for (Direction d : Direction.Plane.HORIZONTAL)
				if (!tile.isConnectedOnSide(d))
					model.renderSide(pose, vc, light, overlay, tile, d);
			model.renderSide(pose, vc, light, overlay, tile, Direction.DOWN);
		}
		else {
			model.renderAll(pose, vc, light, overlay, tile, null, 0, 0);
		}
		pose.popPose();
		if (isInWorld(tile) && tile.isCovered)
			this.renderCover(pose, buf, light);
		this.renderLiquid(tile, pose, buf, light);
	}

	private void renderCover(PoseStack pose, MultiBufferSource buf, int light) {
		TextureAtlasSprite ico = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(GLASS);
		VertexConsumer vc = buf.getBuffer(Sheets.translucentCullBlockSheet());
		Matrix4f m = pose.last().pose();
		float h = 0.99F;
		vertex(vc, m, 0, h, 1, ico.getU0(), ico.getV1(), 0xffffffff, light);
		vertex(vc, m, 1, h, 1, ico.getU1(), ico.getV1(), 0xffffffff, light);
		vertex(vc, m, 1, h, 0, ico.getU1(), ico.getV0(), 0xffffffff, light);
		vertex(vc, m, 0, h, 0, ico.getU0(), ico.getV0(), 0xffffffff, light);
	}

	private static double fill(ReservoirBlockEntity tr) {
		return 0.0625+14D/16D*tr.getFluidAmount()/ReservoirBlockEntity.CAPACITY;
	}

	/** The neighbour at (dx, dz) when it is joined (numpad slot) and holds the same fluid, else null. */
	private static ReservoirBlockEntity neighbour(ReservoirBlockEntity tr, int slot, int dx, int dz, Fluid f) {
		if (!tr.hasNearbyReservoir(slot) || tr.getLevel() == null)
			return null;
		BlockPos p = tr.getBlockPos().offset(dx, 0, dz);
		return tr.getLevel().getBlockEntity(p) instanceof ReservoirBlockEntity o && !o.isEmpty() && o.getFluid().isSame(f) ? o : null;
	}

	private static double avg(double h, ReservoirBlockEntity... others) {
		double sum = h;
		int n = 1;
		for (ReservoirBlockEntity o : others) {
			if (o != null) {
				sum += fill(o);
				n++;
			}
		}
		return sum/n;
	}

	private void renderLiquid(ReservoirBlockEntity tr, PoseStack pose, MultiBufferSource buf, int light) {
		if (tr.isEmpty())
			return;
		Fluid f = tr.getFluid();
		IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(f);
		TextureAtlasSprite ico = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture());
		int clr = isInWorld(tr) ? ext.getTintColor(f.defaultFluidState(), tr.getLevel(), tr.getBlockPos()) : ext.getTintColor();
		if (f.getFluidType().getLightLevel() > 0)
			light = LightTexture.FULL_BRIGHT;
		double h = fill(tr);
		double hmp = h, hpp = h, hpm = h, hmm = h;
		if (isInWorld(tr)) {
			ReservoirBlockEntity tr1 = neighbour(tr, 1, -1, 1, f), tr2 = neighbour(tr, 2, 0, 1, f), tr3 = neighbour(tr, 3, 1, 1, f);
			ReservoirBlockEntity tr4 = neighbour(tr, 4, -1, 0, f), tr6 = neighbour(tr, 6, 1, 0, f);
			ReservoirBlockEntity tr7 = neighbour(tr, 7, -1, -1, f), tr8 = neighbour(tr, 8, 0, -1, f), tr9 = neighbour(tr, 9, 1, -1, f);
			hmp = avg(h, tr1, tr2, tr4);
			hpp = avg(h, tr3, tr2, tr6);
			hpm = avg(h, tr8, tr9, tr6);
			hmm = avg(h, tr7, tr8, tr4);
		}
		VertexConsumer vc = buf.getBuffer(Sheets.translucentCullBlockSheet());
		Matrix4f m = pose.last().pose();
		vertex(vc, m, 0, (float)hmp, 1, ico.getU0(), ico.getV1(), clr, light);
		vertex(vc, m, 1, (float)hpp, 1, ico.getU1(), ico.getV1(), clr, light);
		vertex(vc, m, 1, (float)hpm, 0, ico.getU1(), ico.getV0(), clr, light);
		vertex(vc, m, 0, (float)hmm, 0, ico.getU0(), ico.getV0(), clr, light);
	}

	private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, int clr, int light) {
		vc.addVertex(m, x, y, z).setColor((clr >> 16) & 255, (clr >> 8) & 255, clr & 255, (clr >>> 24) & 255).setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
	}
}
