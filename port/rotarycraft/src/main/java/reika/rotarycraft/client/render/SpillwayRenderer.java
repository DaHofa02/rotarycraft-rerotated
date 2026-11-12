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
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelSpillway;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.SpillwayBlockEntity;

/**
 * Original: Reika.RotaryCraft.Renders.M.RenderSpillway - the model (spillwaytex.png) turned to its side and the water surface at
 * 1/32 + 0.71875 of the fill level. The water sheet down the chute and the splash particles are not ported.
 */
public class SpillwayRenderer extends MachineRenderer<SpillwayBlockEntity> {

	private final ModelSpillway model;

	public SpillwayRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelSpillway(ctx.bakeLayer(RotaryModelLayers.SPILLWAY));
	}

	@Override
	public void render(SpillwayBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (isInWorld(tile)) {
			int r = switch (tile.getBlockMetadata()) {
				case 0 -> 180;
				case 2 -> 90;
				case 3 -> 270;
				default -> 0;
			};
			pose.mulPose(TechneModel.rotation(r, 0.0F, 1.0F, 0.0F));
		}
		model.renderAll(pose, consumer(buf, MachineRenderer.tex("spillwaytex.png")), light, overlay, tile, null, 0, 0);
		pose.popPose();
		if (isInWorld(tile))
			this.renderLiquid(tile, pose, buf, light);
	}

	private void renderLiquid(SpillwayBlockEntity tr, PoseStack pose, MultiBufferSource buf, int light) {
		if (tr.getWaterLevel() <= 0)
			return;
		Fluid f = net.minecraft.world.level.material.Fluids.WATER;
		IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(f);
		TextureAtlasSprite ico = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture());
		int clr = isInWorld(tr) ? ext.getTintColor(f.defaultFluidState(), tr.getLevel(), tr.getBlockPos()) : ext.getTintColor();
		float h = (float)(0.03125+0.71875*tr.getWaterLevel()/SpillwayBlockEntity.CAPACITY);
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
