/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelLamp;
import reika.rotarycraft.client.model.ModelVLamp;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.FloodlightBlockEntity;

/** Original: Reika.RotaryCraft.Renders.DM.RenderLamp - the lamp turned by its metadata; upright the vertical lamp (beam look optional). */
public class FloodlightRenderer extends MachineRenderer<FloodlightBlockEntity> {

	private final ModelLamp lamp;
	private final ModelVLamp vertical;

	public FloodlightRenderer(BlockEntityRendererProvider.Context ctx) {
		lamp = new ModelLamp(ctx.bakeLayer(RotaryModelLayers.FLOODLIGHT));
		vertical = new ModelVLamp(ctx.bakeLayer(RotaryModelLayers.FLOODLIGHT_VERTICAL));
	}

	@Override
	public void render(FloodlightBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		boolean upright = isInWorld(tile) && tile.getBlockMetadata() > 3;
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (isInWorld(tile) && !tile.beammode) {
			int meta = tile.getBlockMetadata();
			int var11 = switch (meta) {
				case 1 -> 180;
				case 2 -> 270;
				case 3 -> 90;
				default -> 0;
			};
			if (meta <= 3)
				pose.mulPose(TechneModel.rotation(var11+90, 0, 1, 0));
			else if (meta == 5) {
				pose.mulPose(TechneModel.rotation(180, 1, 0, 0));
				pose.translate(0, -2.0F, 0);
			}
		}
		if (upright)
			vertical.renderAll(pose, consumer(buf, tex("lampvertical.png")), light, overlay, tile, List.of(tile.beammode), 0, 0);
		else
			lamp.renderAll(pose, consumer(buf, tex("lamptex.png")), light, overlay, tile, List.of(false), 0, 0);
		pose.popPose();
	}
}
