/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelDistribClutch;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.DistributionClutchBlockEntity;

/** Original: Reika.RotaryCraft.Renders.RenderDistribClutch - the distribution clutch turned by its metadata, gears turning with phi. */
public class DistributionClutchRenderer extends MachineRenderer<DistributionClutchBlockEntity> {

	private final ModelDistribClutch model;

	public DistributionClutchRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelDistribClutch(ctx.bakeLayer(RotaryModelLayers.DISTRIBUTION_CLUTCH));
	}

	@Override
	public void render(DistributionClutchBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (isInWorld(tile)) {
			int var11 = switch (tile.getBlockMetadata()%4) {
				case 0 -> 90;
				case 1 -> -90;
				case 2 -> 180;
				default -> 0;
			};
			pose.mulPose(TechneModel.rotation(var11, 0, 1, 0));
		}
		model.renderAll(pose, consumer(buf, tex("transmission/distribclutchtex.png")), light, overlay, tile, null, tile.phi, 0);
		pose.popPose();
	}
}
