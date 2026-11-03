/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
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
import reika.rotarycraft.client.model.ModelDryingBed;
import reika.rotarycraft.content.blockentity.DryingBedBlockEntity;

/**
 * Original: Reika.RotaryCraft.Renders.MI.RenderDryingBed - the basin model, the liquid surface (between 13/16 and 15/16 high by fill,
 * inset by 1/16; full light for glowing liquids) and the dried product lying flat in it, sinking as the batch progresses.
 */
public class DryingBedRenderer extends MachineRenderer<DryingBedBlockEntity> {

	private final ModelDryingBed model;

	public DryingBedRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelDryingBed(ctx.bakeLayer(RotaryModelLayers.DRYING_BED));
	}

	@Override
	public void render(DryingBedBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		model.renderAll(pose, consumer(buf, tex("dryingbedtex.png")), light, overlay, tile, null, 0, 0);
		pose.popPose();
		if (!isInWorld(tile))
			return;
		ItemStack is = tile.getInventory().getStackInSlot(0);
		if (!is.isEmpty()) {
			pose.pushPose();
			pose.translate(0.5, 0.925-tile.progress*0.00005, 0.5);
			pose.mulPose(Axis.XP.rotationDegrees(90));
			pose.scale(0.8F, 0.8F, 0.8F);
			Minecraft.getInstance().getItemRenderer().renderStatic(is, ItemDisplayContext.FIXED, light, overlay, pose, buf, tile.getLevel(), 0);
			pose.popPose();
		}
		this.renderFluid(tile, pose, buf, light);
	}

	private void renderFluid(DryingBedBlockEntity tile, PoseStack pose, MultiBufferSource buf, int light) {
		if (tile.getTank().isEmpty())
			return;
		Fluid f = tile.getTank().getFluid().getFluid();
		IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(f);
		TextureAtlasSprite ico = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture());
		int clr = ext.getTintColor(f.defaultFluidState(), tile.getLevel(), tile.getBlockPos());
		if (f.getFluidType().getLightLevel() > 0)
			light = LightTexture.FULL_BRIGHT;
		int l = tile.getTank().getFluidAmount();
		float h = l > 0 ? 0.8125F+l*0.125F/DryingBedBlockEntity.CAPACITY : 0.5F;
		float a = 0.0625F, b = 1-0.0625F;
		VertexConsumer vc = buf.getBuffer(Sheets.translucentCullBlockSheet());
		Matrix4f m = pose.last().pose();
		vertex(vc, m, a, h, b, ico.getU0(), ico.getV0(), clr, light);
		vertex(vc, m, b, h, b, ico.getU1(), ico.getV0(), clr, light);
		vertex(vc, m, b, h, a, ico.getU1(), ico.getV1(), clr, light);
		vertex(vc, m, a, h, a, ico.getU0(), ico.getV1(), clr, light);
	}

	private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, int clr, int light) {
		vc.addVertex(m, x, y, z).setColor((clr >> 16) & 255, (clr >> 8) & 255, clr & 255, (clr >>> 24) & 255).setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
	}
}
