/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import java.util.List;

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
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelLavaMaker;
import reika.rotarycraft.content.blockentity.LavaMakerBlockEntity;

/**
 * Original: Reika.RotaryCraft.Renders.MI.RenderRockMelter - the melter with its turning grinder, the rock pile while there are items in
 * it, and the liquid surface between 3/16 and 15/16 high by fill.
 */
public class LavaMakerRenderer extends MachineRenderer<LavaMakerBlockEntity> {

	private final ModelLavaMaker model;

	public LavaMakerRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelLavaMaker(ctx.bakeLayer(RotaryModelLayers.LAVA_MAKER));
	}

	@Override
	public void render(LavaMakerBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		boolean world = isInWorld(tile);
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (world)
			pose.mulPose(Axis.YP.rotationDegrees(90));
		model.renderAll(pose, consumer(buf, tex("lavamakertex.png")), light, overlay, tile, List.of(world && tile.hasStone()), world ? -tile.phi : 0, 0);
		pose.popPose();
		if (world)
			this.renderFluid(tile, pose, buf, light);
	}

	private void renderFluid(LavaMakerBlockEntity tile, PoseStack pose, MultiBufferSource buf, int light) {
		if (tile.getTank().isEmpty())
			return;
		Fluid f = tile.getTank().getFluid().getFluid();
		IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(f);
		TextureAtlasSprite ico = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture());
		int clr = ext.getTintColor(f.defaultFluidState(), tile.getLevel(), tile.getBlockPos());
		if (f.getFluidType().getLightLevel() > 0)
			light = LightTexture.FULL_BRIGHT;
		float h = 0.1875F+0.75F*tile.getTank().getFluidAmount()/LavaMakerBlockEntity.CAPACITY;
		VertexConsumer vc = buf.getBuffer(Sheets.translucentCullBlockSheet());
		Matrix4f m = pose.last().pose();
		vertex(vc, m, 0, h, 1, ico.getU0(), ico.getV0(), clr, light);
		vertex(vc, m, 1, h, 1, ico.getU1(), ico.getV0(), clr, light);
		vertex(vc, m, 1, h, 0, ico.getU1(), ico.getV1(), clr, light);
		vertex(vc, m, 0, h, 0, ico.getU0(), ico.getV1(), clr, light);
	}

	private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, int clr, int light) {
		vc.addVertex(m, x, y, z).setColor((clr >> 16) & 255, (clr >> 8) & 255, clr & 255, (clr >>> 24) & 255).setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
	}
}
