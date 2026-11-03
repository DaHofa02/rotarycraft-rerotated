/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelSolar;
import reika.rotarycraft.content.blockentity.SolarTowerBlockEntity;

/** Original: Reika.RotaryCraft.Renders.RenderSolar - the tower segment model, unrotated. */
public class SolarTowerRenderer extends MachineRenderer<SolarTowerBlockEntity> {

	private final ModelSolar model;

	public SolarTowerRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelSolar(ctx.bakeLayer(RotaryModelLayers.SOLAR_TOWER));
	}

	@Override
	public void render(SolarTowerBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		model.renderAll(pose, consumer(buf, tex("solartex.png")), light, overlay, tile, null, 0, 0);
		pose.popPose();
	}
}
