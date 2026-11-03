/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelPulseFurnace;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.PulseJetFurnaceBlockEntity;

/** Original: Reika.RotaryCraft.Renders.MI.RenderPulseFurnace - the texture glows hotter with the temperature (5 stages). */
public class PulseJetFurnaceRenderer extends MachineRenderer<PulseJetFurnaceBlockEntity> {

	private final ModelPulseFurnace model;

	public PulseJetFurnaceRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelPulseFurnace(ctx.bakeLayer(RotaryModelLayers.PULSE_JET));
	}

	private static String texture(PulseJetFurnaceBlockEntity tile) {
		int temp = tile.temperature;
		int maxtemp = PulseJetFurnaceBlockEntity.MAXTEMP;
		if (temp < maxtemp/10)
			return "pulsejet/pulsetex.png";
		if (temp < maxtemp/5)
			return "pulsejet/pulsetexhot-1.png";
		if (temp < maxtemp/2)
			return "pulsejet/pulsetexhot0.png";
		if (temp < maxtemp/1.2)
			return "pulsejet/pulsetexhot2.png";
		return "pulsejet/pulsetexhot3.png";
	}

	@Override
	public void render(PulseJetFurnaceBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (isInWorld(tile))
			pose.mulPose(TechneModel.rotation(180, 0.0F, 1.0F, 0.0F)); //meta 0
		model.renderAll(pose, consumer(buf, MachineRenderer.tex(texture(tile))), light, overlay, tile, null, 0, 0);
		pose.popPose();
	}
}
