/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelRam;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.LineBuilderBlockEntity;

/** Original: Reika.RotaryCraft.Renders.DMI.RenderLineBuilder (ModelRam, ramtex.png). The ram stays retracted (its timer is not synced). */
public class LineBuilderRenderer extends MachineRenderer<LineBuilderBlockEntity> {

	private final ModelRam model;

	public LineBuilderRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelRam(ctx.bakeLayer(RotaryModelLayers.LINE_BUILDER));
	}

	@Override
	public void render(LineBuilderBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		int meta = isInWorld(tile) ? tile.getBlockMetadata() : 0;
		int r = switch (meta) {
			case 0 -> 180;
			case 2, 5 -> 270;
			case 3, 4 -> 90;
			default -> 0;
		};
		if (meta <= 3)
			pose.mulPose(TechneModel.rotation(r+90, 0.0F, 1.0F, 0.0F));
		else {
			pose.mulPose(TechneModel.rotation(r, 1.0F, 0.0F, 0.0F));
			pose.translate(0, -1, 1);
			if (meta == 4)
				pose.translate(0, 0, -2);
		}
		model.renderAll(pose, consumer(buf, tex("ramtex.png")), light, overlay, tile, null, tile.phi, 0);
		pose.popPose();
	}
}
