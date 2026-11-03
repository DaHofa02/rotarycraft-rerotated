/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelFuelConverter;
import reika.rotarycraft.content.blockentity.FuelEnhancerBlockEntity;

/**
 * Original: Reika.RotaryCraft.Renders.M.RenderFuelConverter - the model turning with the input speed, and both fluids as boxes in
 * their glass chambers (input above at 10/16, output below at 1/16; a third of the height when full, filled by level).
 */
public class FuelEnhancerRenderer extends MachineRenderer<FuelEnhancerBlockEntity> {

	private final ModelFuelConverter model;

	public FuelEnhancerRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelFuelConverter(ctx.bakeLayer(RotaryModelLayers.FUEL_ENHANCER));
	}

	@Override
	public void render(FuelEnhancerBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		model.renderAll(pose, consumer(buf, tex("fuelconverttex.png")), light, overlay, tile, null, -tile.phi, 0);
		pose.popPose();
		if (isInWorld(tile)) {
			this.renderFuel(tile, tile.getInputTank(), true, pose, buf, light);
			this.renderFuel(tile, tile.getOutputTank(), false, pose, buf, light);
		}
	}

	private void renderFuel(FuelEnhancerBlockEntity tile, FluidTank tank, boolean in, PoseStack pose, MultiBufferSource buf, int light) {
		if (tank.isEmpty())
			return;
		Fluid f = tank.getFluid().getFluid();
		float frac = Math.min(1, tank.getFluidAmount()/(float)FuelEnhancerBlockEntity.CAPACITY);
		float y0 = (float)tile.getLiquidModelOffset(in)+0.001F;
		float h = frac*0.95F/3F;
		float m = 0.005F;
		var ext = IClientFluidTypeExtensions.of(f);
		TextureAtlasSprite ico = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture());
		box(buf.getBuffer(RenderType.translucent()), pose.last().pose(), ico, ext.getTintColor(), light, m, y0, m, 1-m, y0+h, 1-m);
	}

	private static void box(VertexConsumer vc, Matrix4f mat, TextureAtlasSprite s, int clr, int light, float x0, float y0, float z0, float x1, float y1, float z1) {
		float u0 = s.getU0(), u1 = s.getU1(), v0 = s.getV0(), v1 = s.getV1();
		float[][] q = {
				{x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0}, {x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1},
				{x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0}, {x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1},
				{x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0}, {x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1}};
		float[][] uv = {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
		for (float[] f4 : q)
			for (int i = 0; i < 4; i++)
				vc.addVertex(mat, f4[i*3], f4[i*3+1], f4[i*3+2]).setColor(clr).setUv(uv[i][0], uv[i][1]).setLight(light).setNormal(0, 1, 0);
	}
}
