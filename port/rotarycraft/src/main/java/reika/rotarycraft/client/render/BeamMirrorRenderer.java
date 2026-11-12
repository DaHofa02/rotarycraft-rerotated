/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelBeamMirror;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.BeamMirrorBlockEntity;

/**
 * Original: Reika.RotaryCraft.Renders.DM.RenderBeamMirror (beammirrortex.png). As in the original, the model gets no tilt angle (the
 * renderer passed only phi), so the mirror is drawn upright.
 */
public class BeamMirrorRenderer extends MachineRenderer<BeamMirrorBlockEntity> {

	private final ModelBeamMirror model;

	public BeamMirrorRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelBeamMirror(ctx.bakeLayer(RotaryModelLayers.BEAM_MIRROR));
	}

	@Override
	public void render(BeamMirrorBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (isInWorld(tile)) {
			int r = switch (tile.getBlockMetadata()) {
				case 0 -> 180;
				case 2 -> 270;
				case 3 -> 90;
				default -> 0;
			};
			pose.mulPose(TechneModel.rotation(r+90, 0.0F, 1.0F, 0.0F));
		}
		model.renderAll(pose, consumer(buf, tex("beammirrortex.png")), light, overlay, tile, null, -tile.phi, 0);
		pose.popPose();
	}
}
