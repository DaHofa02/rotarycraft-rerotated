/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelFan;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.FanBlockEntity;

/**
 * Original: Reika.RotaryCraft.Renders.DM.RenderFan - the fan turned by its metadata (sideways about y, up/down about x), the blades
 * turning by -phi; the wide fan (diffuser) uses fantex_wide. The blow and harvest boxes the original drew after a screwdriver click are
 * not drawn (the I/O overlay is).
 */
public class FanRenderer extends MachineRenderer<FanBlockEntity> {

	private final ModelFan model;

	public FanRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelFan(ctx.bakeLayer(RotaryModelLayers.FAN));
	}

	@Override
	public void render(FanBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (isInWorld(tile)) {
			int meta = tile.getBlockMetadata();
			int var11 = switch (meta) {
				case 1 -> 180;
				case 2, 4 -> 270;
				case 3, 5 -> 90;
				default -> 0;
			};
			if (meta <= 3) {
				pose.mulPose(TechneModel.rotation(var11+90, 0, 1, 0));
			}
			else {
				pose.mulPose(TechneModel.rotation(var11, 1, 0, 0));
				pose.translate(0, -1, 1);
				if (meta == 5)
					pose.translate(0, 0, -2);
			}
		}
		model.renderAll(pose, consumer(buf, tex(tile.wideAreaBlow ? "fantex_wide.png" : "fantex.png")), light, overlay, tile, null, -tile.phi, 0);
		pose.popPose();
	}
}
