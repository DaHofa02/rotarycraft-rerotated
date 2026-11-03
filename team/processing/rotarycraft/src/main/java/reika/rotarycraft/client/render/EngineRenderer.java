/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import java.util.List;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelAC;
import reika.rotarycraft.client.model.ModelCombustion;
import reika.rotarycraft.client.model.ModelDC;
import reika.rotarycraft.client.model.ModelHydro;
import reika.rotarycraft.client.model.ModelJet;
import reika.rotarycraft.client.model.ModelMicroTurbine;
import reika.rotarycraft.client.model.ModelPerformance;
import reika.rotarycraft.client.model.ModelSteam;
import reika.rotarycraft.client.model.ModelWind;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.HydroEngineBlockEntity;
import reika.rotarycraft.content.blockentity.engine.JetEngineBlockEntity;
import reika.rotarycraft.lib.math.BlackbodyColor;

/**
 * Original: Reika.RotaryCraft.Renders.RenderSEngine. Not ported: the gas turbine's heat-ripple distortion shader in front of the exhaust
 * (HeatRippleRenderer) and the redstone-upgrade frame (comes with the I/O and frame overlays).
 */
public class EngineRenderer extends MachineRenderer<EngineBlockEntity> {

	private final ModelDC dc;
	private final ModelSteam steam;
	private final ModelCombustion comb;
	private final ModelAC ac;
	private final ModelPerformance perf;
	private final ModelMicroTurbine micro;
	private final ModelWind wind;
	private final ModelHydro hydro;
	private final ModelJet jet;

	public EngineRenderer(BlockEntityRendererProvider.Context ctx) {
		dc = new ModelDC(ctx.bakeLayer(RotaryModelLayers.DC));
		steam = new ModelSteam(ctx.bakeLayer(RotaryModelLayers.STEAM));
		comb = new ModelCombustion(ctx.bakeLayer(RotaryModelLayers.COMBUSTION));
		ac = new ModelAC(ctx.bakeLayer(RotaryModelLayers.AC));
		perf = new ModelPerformance(ctx.bakeLayer(RotaryModelLayers.PERFORMANCE));
		micro = new ModelMicroTurbine(ctx.bakeLayer(RotaryModelLayers.MICRO));
		wind = new ModelWind(ctx.bakeLayer(RotaryModelLayers.WIND));
		hydro = new ModelHydro(ctx.bakeLayer(RotaryModelLayers.HYDRO));
		jet = new ModelJet(ctx.bakeLayer(RotaryModelLayers.JET));
	}

	private static String texture(EngineBlockEntity tile) {
		return "engine/"+switch(tile.getEngineType()) {
			case DC -> "dc.png";
			case WIND -> "windtex.png";
			case STEAM -> "steamtex.png";
			case GAS -> "combtex.png";
			case AC -> "actex.png";
			case SPORT -> "perftex.png";
			case HYDRO -> tile instanceof HydroEngineBlockEntity h && h.isBedrock() ? "bedhydrotex.png" : "hydrotex.png";
			case MICRO -> "microtex.png";
			case JET -> isInWorld(tile) && tile instanceof JetEngineBlockEntity j && j.canAfterBurn() ? "jettex_b.png" : "jettex.png";
		};
	}

	@Override
	public void render(EngineBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		var vc = consumer(buf, MachineRenderer.tex(texture(tile)));
		this.setupGL(tile, pose);
		if (isInWorld(tile)) {
			int var11 = switch(tile.getBlockMetadata()) {
				case 1 -> 180;
				case 2 -> 90;
				case 3 -> 270;
				default -> 0;
			};
			if (tile.getEngineType().isJetFueled())
				var11 += 90;
			pose.mulPose(TechneModel.rotation(var11, 0.0F, 1.0F, 0.0F));
		}
		else {
			pose.mulPose(TechneModel.rotation(-90, 0.0F, 1.0F, 0.0F));
			switch(tile.getEngineType()) {
				case DC -> dc.renderAll(pose, vc, light, overlay, tile, null, 0, 0);
				case WIND -> {
					pose.mulPose(TechneModel.rotation(90, 0.0F, 1.0F, 0.0F));
					double s = 0.7;
					double d = 0.375;
					pose.scale((float)s, (float)s, (float)s);
					pose.translate(0, d, 0);
					pose.translate(0.2, 0, 0);
					wind.renderAll(pose, vc, light, overlay, tile, null, 0, 0);
				}
				case STEAM -> steam.renderAll(pose, vc, light, overlay, tile, null, 0, 0);
				case GAS -> comb.renderAll(pose, vc, light, overlay, tile, null, 0, 0);
				case AC -> ac.renderAll(pose, vc, light, overlay, tile, null, 0, 0);
				case SPORT -> perf.renderAll(pose, vc, light, overlay, tile, null, Float.MIN_NORMAL, 0);
				case HYDRO -> {
					HydroEngineBlockEntity eng = (HydroEngineBlockEntity)tile;
					double s = 0.7;
					pose.translate(0, 0.375, 0);
					pose.scale((float)s, (float)s, (float)s);
					hydro.renderAll(pose, vc, light, overlay, tile, List.of(eng.failed, eng.isBedrock()), 0, 0);
				}
				case MICRO -> {
					pose.mulPose(TechneModel.rotation(90, 0.0F, 1.0F, 0.0F));
					micro.renderAll(pose, vc, light, overlay, tile, null, 0, 0);
				}
				case JET -> {
					pose.mulPose(TechneModel.rotation(90, 0.0F, 1.0F, 0.0F));
					jet.renderAll(pose, vc, light, overlay, tile, null, 0, 0);
				}
			}
			this.closeGL(tile, pose);
			return;
		}
		switch (tile.getEngineType()) {
			case DC -> dc.renderAll(pose, vc, light, overlay, tile, null, -tile.phi, 0);
			case WIND -> {
				pose.mulPose(TechneModel.rotation(-90, 0.0F, 1.0F, 0.0F));
				wind.renderAll(pose, vc, light, overlay, tile, null, -tile.phi, 0);
			}
			case STEAM -> steam.renderAll(pose, vc, light, overlay, tile, null, -tile.phi, 0);
			case GAS -> comb.renderAll(pose, vc, light, overlay, tile, null, -tile.phi, 0);
			case AC -> ac.renderAll(pose, vc, light, overlay, tile, null, -tile.phi, 0);
			case SPORT -> perf.renderAll(pose, vc, light, overlay, tile, null, -tile.phi, 0);
			case MICRO -> micro.renderAll(pose, vc, light, overlay, tile, null, -tile.phi, 0);
			case HYDRO -> {
				HydroEngineBlockEntity eng = (HydroEngineBlockEntity)tile;
				hydro.renderAll(pose, vc, light, overlay, tile, List.of(eng.failed, eng.isBedrock()), eng.isReversed() ? tile.phi : -tile.phi, 0);
			}
			case JET -> {
				jet.renderAll(pose, vc, light, overlay, tile, null, -tile.phi, 0);
				this.renderJetGlow(tile, pose, buf, light, overlay);
			}
		}
		this.closeGL(tile, pose);
	}

	/**
	 * Original RenderSEngine JET glow: the model drawn twice more, slightly scaled, with jettex_glow_mask2 tinted by the blackbody
	 * colour of the exhaust temperature and blended additively, plus a filled nozzle polygon at 65 % brightness.
	 */
	private void renderJetGlow(EngineBlockEntity tile, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		int temp = Math.max(tile.temperature-600, (tile.temperature-1000)*5/2);
		int c = BlackbodyColor.getColorForTemperature(temp);
		if (c == 0)
			return;
		int full = LightTexture.FULL_BRIGHT;
		VertexConsumer glow = new TintedVertexConsumer(buf.getBuffer(RenderType.eyes(MachineRenderer.tex("engine/jettex_glow_mask2.png"))), c, 1);
		double s = 1.005;
		pose.pushPose();
		pose.translate(0, 1-s, (1-s)/4);
		pose.scale((float)s, (float)s, (float)s);
		jet.renderAll(pose, glow, full, overlay, tile, null, -tile.phi, 0);
		pose.popPose();
		pose.pushPose();
		pose.translate(0, s-1, -(1-s)/4);
		pose.scale((float)(1/s), (float)(1/s), (float)(1/s));
		jet.renderAll(pose, glow, full, overlay, tile, null, -tile.phi, 0);
		pose.popPose();

		int c2 = BlackbodyColor.mixColors(c, 0, 0.65F); //ReikaColorAPI.getColorWithBrightnessMultiplier(c, 0.65F)
		VertexConsumer fan = buf.getBuffer(RenderType.lightning());
		float[][] v = {{0.5F, 0.5F}, {0.3F, 0.8F}, {0.7F, 0.8F}, {0.8F, 0.5F}, {0.7F, 0.2F}, {0.3F, 0.2F}, {0.2F, 0.5F}, {0.3F, 0.8F}};
		Matrix4f m = pose.last().pose();
		int r = (c2 >> 16) & 0xff, g = (c2 >> 8) & 0xff, b = c2 & 0xff;
		for (int i = 1; i < v.length-1; i++) { //GL_TRIANGLE_FAN as degenerate quads
			fan.addVertex(m, v[0][0]-0.5F, v[0][1]+0.5625F, -0.45F).setColor(r, g, b, 255);
			fan.addVertex(m, v[i][0]-0.5F, v[i][1]+0.5625F, -0.45F).setColor(r, g, b, 255);
			fan.addVertex(m, v[i+1][0]-0.5F, v[i+1][1]+0.5625F, -0.45F).setColor(r, g, b, 255);
			fan.addVertex(m, v[i+1][0]-0.5F, v[i+1][1]+0.5625F, -0.45F).setColor(r, g, b, 255);
		}
	}
}
