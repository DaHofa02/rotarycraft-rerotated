/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelScaleChest;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.ScaleableChestBlockEntity;

/** Original: Reika.RotaryCraft.Renders.DMI.RenderScaleChest - the 1.7.10 chest model (chest.png) turned to its input side, lid opening. */
public class ScaleableChestRenderer extends MachineRenderer<ScaleableChestBlockEntity> {

	private final ModelScaleChest model;

	public ScaleableChestRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelScaleChest(ctx.bakeLayer(RotaryModelLayers.SCALEABLE_CHEST));
	}

	@Override
	public void render(ScaleableChestBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 1.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (isInWorld(tile)) {
			int r = switch (tile.getBlockMetadata()) {
				case 0 -> 0;
				case 1 -> 180;
				case 2 -> 90;
				default -> 270;
			};
			pose.mulPose(TechneModel.rotation(r+90, 0.0F, 1.0F, 0.0F));
		}
		pose.translate(-0.5F, -0.5F, -0.5F);
		float lid = tile.prevLidAngle+(tile.lidAngle-tile.prevLidAngle)*partial;
		model.render(pose, consumer(buf, tex("chest.png")), light, overlay, lid);
		pose.popPose();
	}
}
