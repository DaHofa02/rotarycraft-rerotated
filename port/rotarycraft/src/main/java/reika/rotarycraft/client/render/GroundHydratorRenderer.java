/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelReservoir;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.GroundHydratorBlockEntity;

/**
 * Original: Reika.RotaryCraft.Renders.M.RenderHydrator - the reservoir model with hydratortex.png (the four walls and the floor in the
 * world) and the water surface at 1/16 + 14/16 of the fill level.
 */
public class GroundHydratorRenderer extends MachineRenderer<GroundHydratorBlockEntity> {

	private final ModelReservoir model;

	public GroundHydratorRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelReservoir(ctx.bakeLayer(RotaryModelLayers.HYDRATOR));
	}

	@Override
	public void render(GroundHydratorBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		VertexConsumer vc = consumer(buf, MachineRenderer.tex("hydratortex.png"));
		if (isInWorld(tile)) {
			pose.mulPose(TechneModel.rotation(90, 0.0F, 1.0F, 0.0F)); //meta 0: var11+90
			for (Direction d : Direction.Plane.HORIZONTAL)
				model.renderSide(pose, vc, light, overlay, tile, d);
			model.renderSide(pose, vc, light, overlay, tile, Direction.DOWN);
		}
		else {
			model.renderAll(pose, vc, light, overlay, tile, null, 0, 0);
		}
		pose.popPose();
		this.renderLiquid(tile, pose, buf, light);
	}

	private void renderLiquid(GroundHydratorBlockEntity tr, PoseStack pose, MultiBufferSource buf, int light) {
		Fluid f = tr.getFluid();
		if (f == null)
			return;
		IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(f);
		TextureAtlasSprite ico = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture());
		int clr = isInWorld(tr) ? ext.getTintColor(f.defaultFluidState(), tr.getLevel(), tr.getBlockPos()) : ext.getTintColor();
		float h = (float)(0.0625+0.875*tr.getWaterLevel()/1000D);
		VertexConsumer vc = buf.getBuffer(Sheets.translucentCullBlockSheet());
		Matrix4f m = pose.last().pose();
		vertex(vc, m, 0, h, 1, ico.getU0(), ico.getV1(), clr, light);
		vertex(vc, m, 1, h, 1, ico.getU1(), ico.getV1(), clr, light);
		vertex(vc, m, 1, h, 0, ico.getU1(), ico.getV0(), clr, light);
		vertex(vc, m, 0, h, 0, ico.getU0(), ico.getV0(), clr, light);
	}

	private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, int clr, int light) {
		vc.addVertex(m, x, y, z).setColor((clr >> 16) & 255, (clr >> 8) & 255, clr & 255, (clr >>> 24) & 255).setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
	}
}
