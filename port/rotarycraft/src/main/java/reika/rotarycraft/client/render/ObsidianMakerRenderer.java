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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelObsidian;
import reika.rotarycraft.content.blockentity.ObsidianMakerBlockEntity;

/**
 * Original: Reika.RotaryCraft.Renders.MI.RenderObsidian - the model (obsidiantex.png) and inside it the water or lava surface at 0.6875
 * of its fill level, or, with both, the mix (drawn here with the obsidian block texture). The model's red glow when hot is not ported.
 */
public class ObsidianMakerRenderer extends MachineRenderer<ObsidianMakerBlockEntity> {

	private final ModelObsidian model;

	public ObsidianMakerRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelObsidian(ctx.bakeLayer(RotaryModelLayers.OBSIDIAN_MAKER));
	}

	@Override
	public void render(ObsidianMakerBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		model.renderAll(pose, consumer(buf, MachineRenderer.tex("obsidiantex.png")), light, overlay, tile, null, 0, 0);
		pose.popPose();
		if (isInWorld(tile))
			this.renderLiquid(tile, pose, buf, light);
	}

	private void renderLiquid(ObsidianMakerBlockEntity tr, PoseStack pose, MultiBufferSource buf, int light) {
		boolean w = tr.getWater() > 0, l = tr.getLava() > 0;
		if (!w && !l)
			return;
		TextureAtlasSprite ico;
		int clr = 0xffffffff;
		float h;
		if (w && l) {
			ico = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.withDefaultNamespace("block/obsidian"));
			h = 0.6875F;
		}
		else {
			Fluid f = w ? net.minecraft.world.level.material.Fluids.WATER : net.minecraft.world.level.material.Fluids.LAVA;
			IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(f);
			ico = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture());
			clr = ext.getTintColor(f.defaultFluidState(), tr.getLevel(), tr.getBlockPos());
			h = 0.6875F*(w ? tr.getWater() : tr.getLava())/ObsidianMakerBlockEntity.CAPACITY;
			if (l)
				light = LightTexture.FULL_BRIGHT;
		}
		h += 0.0625F;
		VertexConsumer vc = buf.getBuffer(Sheets.translucentCullBlockSheet());
		Matrix4f m = pose.last().pose();
		vertex(vc, m, 0.0625F, h, 0.9375F, ico.getU0(), ico.getV1(), clr, light);
		vertex(vc, m, 0.9375F, h, 0.9375F, ico.getU1(), ico.getV1(), clr, light);
		vertex(vc, m, 0.9375F, h, 0.0625F, ico.getU1(), ico.getV0(), clr, light);
		vertex(vc, m, 0.0625F, h, 0.0625F, ico.getU0(), ico.getV0(), clr, light);
	}

	private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, int clr, int light) {
		vc.addVertex(m, x, y, z).setColor((clr >> 16) & 255, (clr >> 8) & 255, clr & 255, (clr >>> 24) & 255).setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
	}
}
