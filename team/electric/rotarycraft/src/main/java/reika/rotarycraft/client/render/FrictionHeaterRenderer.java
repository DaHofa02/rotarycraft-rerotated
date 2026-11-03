/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelFriction;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.FrictionHeaterBlockEntity;

/**
 * Original: Reika.RotaryCraft.Renders.DM.RenderFriction - the model with a texture that glows more the hotter it is (400/700/1000/
 * 1300/1600 C), and a glowing furnace front (hotfurnace_0-2 above 500/750/1000 C) while it drives a vanilla furnace. The heat-ripple
 * shader is not ported.
 */
public class FrictionHeaterRenderer extends MachineRenderer<FrictionHeaterBlockEntity> {

	private final ModelFriction model;

	public FrictionHeaterRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelFriction(ctx.bakeLayer(RotaryModelLayers.FRICTION));
	}

	private static String texture(FrictionHeaterBlockEntity tile) {
		int t = tile.getTemperature();
		String s = "";
		if (isInWorld(tile)) {
			if (t >= 1600)
				s = "-5";
			else if (t >= 1300)
				s = "-4";
			else if (t >= 1000)
				s = "-3";
			else if (t >= 700)
				s = "-2";
			else if (t >= 400)
				s = "-1";
		}
		return "friction/frictiontex"+s+".png";
	}

	@Override
	public void render(FrictionHeaterBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		int var9 = isInWorld(tile) ? tile.getBlockMetadata() : 3;
		this.setupGL(tile, pose);
		int var11 = switch(var9) {
			case 1 -> 180;
			case 2 -> 90;
			case 3 -> 270;
			default -> 0;
		};
		pose.mulPose(TechneModel.rotation((float)var11+90, 0.0F, 1.0F, 0.0F));
		model.renderAll(pose, consumer(buf, MachineRenderer.tex(texture(tile))), light, overlay, tile, null, -tile.phi, 0);
		this.closeGL(tile, pose);
		if (isInWorld(tile))
			this.renderHotSide(tile, pose, buf, light);
	}

	private void renderHotSide(FrictionHeaterBlockEntity tile, PoseStack pose, MultiBufferSource buf, int light) {
		if (!tile.hasFurnace())
			return;
		int t = tile.getTemperature();
		String tex;
		if (t > 1000)
			tex = "hotfurnace_2.png";
		else if (t > 750)
			tex = "hotfurnace_1.png";
		else if (t > 500)
			tex = "hotfurnace_0.png";
		else
			return;
		VertexConsumer v = buf.getBuffer(RenderType.entityTranslucent(ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/misc/"+tex)));
		PoseStack.Pose p = pose.last();
		Matrix4f m = p.pose();
		float[][] q = switch(tile.getBlockMetadata()) {
			case 0 -> new float[][] {{0.001F, 0, 1, 0, 1}, {0.001F, 0, 0, 1, 1}, {0.001F, 1, 0, 1, 0}, {0.001F, 1, 1, 0, 0}};
			case 1 -> new float[][] {{0.999F, 0, 0, 0, 1}, {0.999F, 0, 1, 1, 1}, {0.999F, 1, 1, 1, 0}, {0.999F, 1, 0, 0, 0}};
			case 2 -> new float[][] {{0, 0, 0.001F, 0, 1}, {1, 0, 0.001F, 1, 1}, {1, 1, 0.001F, 1, 0}, {0, 1, 0.001F, 0, 0}};
			default -> new float[][] {{0, 0, 0.999F, 0, 0}, {1, 0, 0.999F, 1, 0}, {1, 1, 0.999F, 1, 1}, {0, 1, 0.999F, 0, 1}};
		};
		for (float[] c : q)
			v.addVertex(m, c[0], c[1], c[2]).setColor(255, 255, 255, 255).setUv(c[3], c[4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 1, 0);
	}
}
