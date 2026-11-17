/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelSonicBorer;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.SonicBorerBlockEntity;

/** Original: Reika.RotaryCraft.Renders.DMM.RenderSonicBorer - the borer turned by its metadata (upright ones tipped over). */
public class SonicBorerRenderer extends MachineRenderer<SonicBorerBlockEntity> {

	private final ModelSonicBorer model;

	public SonicBorerRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelSonicBorer(ctx.bakeLayer(RotaryModelLayers.SONIC_BORER));
	}

	@Override
	public void render(SonicBorerBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		int meta = isInWorld(tile) ? tile.getBlockMetadata() : 0;
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (isInWorld(tile)) {
			int var11 = switch (meta) {
				case 1 -> 180;
				case 2, 5 -> 90;
				case 3, 4 -> 270;
				default -> 0;
			};
			if (meta <= 3)
				pose.mulPose(TechneModel.rotation(var11+90, 0, 1, 0));
			else {
				pose.mulPose(TechneModel.rotation(var11, 1, 0, 0));
				pose.translate(0, -1, 1);
				if (meta == 5)
					pose.translate(0, 0, -2);
			}
		}
		model.renderAll(pose, consumer(buf, tex("sonicborertex.png")), light, overlay, tile, List.of(meta < 4), -tile.phi, 0);
		pose.popPose();
	}
}
