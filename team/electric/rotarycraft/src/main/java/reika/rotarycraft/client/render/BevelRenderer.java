/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelBevel;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.BevelGearBlockEntity;

/** Original: Reika.RotaryCraft.Renders.RenderBevel (its own GL setup without the flip, and 24 orientations). Face colour/number overlays are not ported yet. */
public class BevelRenderer extends MachineRenderer<BevelGearBlockEntity> {

	private final ModelBevel model;

	public BevelRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelBevel(ctx.bakeLayer(RotaryModelLayers.BEVEL));
	}

	@Override
	public void render(BevelGearBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		int var11 = 0;
		int var12 = 0;
		int var13 = 0;
		int dir = 1;
		if (isInWorld(tile)) {
			switch(tile.direction) {
				case 0 -> var11 = 90;
				case 1 -> var11 = 180;
				case 2 -> var11 = 270;
				case 3 -> var11 = 0;
				case 4 -> { var11 = 0; dir = -1; }
				case 5 -> { var11 = 90; dir = -1; }
				case 6 -> { var11 = 180; dir = -1; }
				case 7 -> { var11 = 270; dir = -1; }
				case 8 -> { var11 = 0; var12 = 270; pose.translate(0F, 1F, 1F); dir = -1; }
				case 9 -> { var11 = 90; var12 = 270; pose.translate(1F, 1F, -0F); dir = -1; }
				case 10 -> { var11 = 180; var12 = 270; pose.translate(0F, 1F, -1F); dir = -1; }
				case 11 -> { var11 = -90; var12 = 270; pose.translate(-1F, 1F, -0F); dir = -1; }
				case 12 -> { var11 = 0; var12 = 90; pose.translate(0F, 1F, -1F); dir = -1; }
				case 13 -> { var11 = 90; var12 = 90; pose.translate(-1F, 1F, -0F); }
				case 14 -> { var11 = 180; var12 = 90; pose.translate(0F, 1F, 1F); dir = -1; }
				case 15 -> { var11 = -90; var12 = 90; pose.translate(1F, 1F, -0F); dir = -1; }
				case 16 -> { var11 = 0; var12 = 90; pose.translate(0F, 1F, -1F); dir = -1; }
				case 17 -> { var11 = 90; var12 = 90; pose.translate(-1F, 1F, -0F); dir = -1; }
				case 18 -> { var11 = 180; var12 = 90; pose.translate(0F, 1F, 1F); dir = -1; }
				case 19 -> { var11 = -90; var12 = 90; pose.translate(1F, 1F, -0F); dir = -1; }
				case 20 -> { var11 = 0; var12 = 270; pose.translate(0F, 1F, 1F); }
				case 21 -> { var11 = 90; var12 = 270; pose.translate(1F, 1F, -0F); }
				case 22 -> { var11 = 180; var12 = 270; pose.translate(0F, 1F, -1F); }
				case 23 -> { var11 = -90; var12 = 270; pose.translate(-1F, 1F, 0F); }
			}
			pose.mulPose(TechneModel.rotation(var11, 0.0F, 1.0F, 0.0F));
			pose.mulPose(TechneModel.rotation(var12, 1.0F, 0.0F, 0.0F));
			pose.mulPose(TechneModel.rotation(var13, 0.0F, 0.0F, 1.0F));
		}
		else {
			pose.mulPose(TechneModel.rotation(90F, 0.0F, 1.0F, 0.0F));
		}
		model.renderAll(pose, consumer(buf, MachineRenderer.tex("transmission/beveltex.png")), light, overlay, tile, null, tile.phi*dir, 0);
		pose.popPose();
	}
}
