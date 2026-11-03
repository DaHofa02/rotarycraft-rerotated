/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelClutch;
import reika.rotarycraft.client.model.ModelVClutch;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.ClutchBlockEntity;

/** Original: Reika.RotaryCraft.Renders.RenderClutch */
public class ClutchRenderer extends MachineRenderer<ClutchBlockEntity> {

	private final ModelClutch model;
	private final ModelVClutch modelV;

	public ClutchRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelClutch(ctx.bakeLayer(RotaryModelLayers.CLUTCH));
		modelV = new ModelVClutch(ctx.bakeLayer(RotaryModelLayers.CLUTCH_V));
	}

	@Override
	public void render(ClutchBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		this.setupGL(tile, pose);
		if (isInWorld(tile)) {
			int var11 = switch(tile.getBlockMetadata()) {
				case 1 -> 180;
				case 2 -> 90;
				case 3 -> 270;
				default -> 0;
			};
			pose.mulPose(TechneModel.rotation(var11, 0.0F, 1.0F, 0.0F));
		}
		var vc = consumer(buf, MachineRenderer.tex("transmission/shaft/shafttex.png"));
		if (tile.getBlockMetadata() < 4)
			model.renderAll(pose, vc, light, overlay, tile, null, -tile.phi, 0);
		else
			modelV.renderAll(pose, vc, light, overlay, tile, null, tile.getBlockMetadata() == 5 ? tile.phi : -tile.phi, 0);
		this.closeGL(tile, pose);
	}
}
