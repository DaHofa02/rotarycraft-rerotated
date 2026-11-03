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
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelFillingStation;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.FillingStationBlockEntity;

/**
 * Original: Reika.RotaryCraft.Renders.DMI.RenderFillingStation - the model turned by its metadata, the tank's fluid surface (inset on
 * the front side, rising with the level) and the item being filled, drawn flat in the window.
 */
public class FillingStationRenderer extends MachineRenderer<FillingStationBlockEntity> {

	private final ModelFillingStation model;

	public FillingStationRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelFillingStation(ctx.bakeLayer(RotaryModelLayers.FILLING_STATION));
	}

	private static int angle(int meta) {
		return switch (meta) {
			case 0 -> 180;
			case 2 -> 270;
			case 3 -> 90;
			default -> 0;
		};
	}

	@Override
	public void render(FillingStationBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (isInWorld(tile))
			pose.mulPose(TechneModel.rotation(angle(tile.getBlockMetadata())-90, 0, 1, 0));
		model.renderAll(pose, consumer(buf, tex("fillingtex.png")), light, overlay, tile, null, -tile.phi, 0);
		pose.popPose();
		if (!isInWorld(tile))
			return;
		this.renderLiquid(tile, pose, buf, light);
		this.renderItem(tile, pose, buf, light, overlay);
	}

	private void renderLiquid(FillingStationBlockEntity tr, PoseStack pose, MultiBufferSource buf, int light) {
		Fluid f = tr.getFluid();
		if (f == null)
			return;
		double dx = 0, dz = 0, ddx = 0, ddz = 0;
		switch (tr.getBlockMetadata()) {
			case 0 -> dx = 0.25;
			case 1 -> ddx = 0.25;
			case 2 -> dz = 0.25;
			case 3 -> ddz = 0.25;
			default -> {}
		}
		IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(f);
		TextureAtlasSprite ico = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture());
		int clr = ext.getTintColor(f.defaultFluidState(), tr.getLevel(), tr.getBlockPos());
		if (f.getFluidType().getLightLevel() > 0)
			light = LightTexture.FULL_BRIGHT;
		float h = (float)(0.0625+14D/16D*tr.getTank().getFluidAmount()/FillingStationBlockEntity.CAPACITY);
		VertexConsumer vc = buf.getBuffer(Sheets.translucentCullBlockSheet());
		Matrix4f m = pose.last().pose();
		vertex(vc, m, (float)dx, h, (float)(1-ddz), ico.getU0(), ico.getV1(), clr, light);
		vertex(vc, m, (float)(1-ddx), h, (float)(1-ddz), ico.getU1(), ico.getV1(), clr, light);
		vertex(vc, m, (float)(1-ddx), h, (float)dz, ico.getU1(), ico.getV0(), clr, light);
		vertex(vc, m, (float)dx, h, (float)dz, ico.getU0(), ico.getV0(), clr, light);
	}

	private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, int clr, int light) {
		vc.addVertex(m, x, y, z).setColor((clr >> 16) & 255, (clr >> 8) & 255, clr & 255, 255).setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
	}

	/** Original renderItem: the item's sprite on the plane z = ±0.125 of the turned block frame. */
	private void renderItem(FillingStationBlockEntity fs, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		ItemStack is = fs.getItemForRender();
		if (is.isEmpty())
			return;
		double in = 0.125, xoff = 0, zoff = 0;
		switch (fs.getBlockMetadata()) {
			case 1 -> {
				xoff = 1;
				zoff = -1;
			}
			case 2 -> in = -in;
			case 3 -> {
				xoff = 1;
				zoff = 1;
				in = -in;
			}
			default -> {}
		}
		pose.pushPose();
		pose.mulPose(TechneModel.rotation(angle(fs.getBlockMetadata())-90, 0, 1, 0));
		pose.translate(xoff, 0, zoff);
		pose.translate(-0.5, 0.5, in);
		pose.scale(1, 1, 0.01F);
		Minecraft.getInstance().getItemRenderer().renderStatic(is, ItemDisplayContext.FIXED, light, overlay, pose, buf, fs.getLevel(), 0);
		pose.popPose();
	}
}
