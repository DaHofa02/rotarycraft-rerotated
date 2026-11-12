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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelWetter;
import reika.rotarycraft.content.blockentity.WetterBlockEntity;

/**
 * Original: Reika.RotaryCraft.Renders.MI.RenderWetter - the model (wettertex.png) turning with phi, the item in the slot floating in
 * the middle, and the liquid. The liquid is drawn as its surface at 1/8 + 0.805 * the fill level (the original drew a filled box).
 */
public class WetterRenderer extends MachineRenderer<WetterBlockEntity> {

	private final ModelWetter model;

	public WetterRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelWetter(ctx.bakeLayer(RotaryModelLayers.WETTER));
	}

	@Override
	public void render(WetterBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		model.renderAll(pose, consumer(buf, MachineRenderer.tex("wettertex.png")), light, overlay, tile, null, tile.phi, 0);
		pose.popPose();
		if (!isInWorld(tile))
			return;
		ItemStack is = tile.getInventory().getStackInSlot(0);
		if (!is.isEmpty()) {
			pose.pushPose();
			pose.translate(0.5, 0.3125, 0.5);
			pose.scale(1.5F, 1.5F, 1.5F);
			Minecraft.getInstance().getItemRenderer().renderStatic(is, ItemDisplayContext.GROUND, light, overlay, pose, buf, tile.getLevel(), 0);
			pose.popPose();
		}
		this.renderLiquid(tile, pose, buf, light);
	}

	private void renderLiquid(WetterBlockEntity tr, PoseStack pose, MultiBufferSource buf, int light) {
		if (tr.getTank().isEmpty())
			return;
		Fluid f = tr.getTank().getFluid().getFluid();
		IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(f);
		TextureAtlasSprite ico = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture());
		int clr = isInWorld(tr) ? ext.getTintColor(f.defaultFluidState(), tr.getLevel(), tr.getBlockPos()) : ext.getTintColor();
		float h = (float)(0.126+0.805*tr.getTank().getFluidAmount()/WetterBlockEntity.CAPACITY);
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
