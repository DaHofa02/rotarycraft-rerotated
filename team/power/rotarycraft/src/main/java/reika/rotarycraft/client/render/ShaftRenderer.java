/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelCross;
import reika.rotarycraft.client.model.ModelShaft;
import reika.rotarycraft.client.model.ModelShaftV;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.ShaftBlockEntity;

/** Original: Reika.RotaryCraft.Renders.RenderShaft */
public class ShaftRenderer extends MachineRenderer<ShaftBlockEntity> {

	private final ModelShaft shaftModel;
	private final ModelShaftV vShaftModel;
	private final ModelCross crossModel;

	public ShaftRenderer(BlockEntityRendererProvider.Context ctx) {
		shaftModel = new ModelShaft(ctx.bakeLayer(RotaryModelLayers.SHAFT));
		vShaftModel = new ModelShaftV(ctx.bakeLayer(RotaryModelLayers.SHAFT_V));
		crossModel = new ModelCross(ctx.bakeLayer(RotaryModelLayers.CROSS));
	}

	private static String getImageFileName(ShaftBlockEntity tile) {
		String tex = tile.getShaftType().getBaseShaftTexture()+".png";
		if (tile.isCross())
			tex = "crosstex.png";
		else if (tile.isVertical())
			tex = "v"+tex;
		return "transmission/shaft/"+tex;
	}

	@Override
	public void render(ShaftBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		this.setupGL(tile, pose);

		boolean failed = false;
		if (isInWorld(tile)) {
			failed = tile.failed();
			int var11 = switch(tile.getBlockMetadata()) {
				case 1 -> 180;
				case 2 -> 90;
				case 3 -> 270;
				default -> 0;
			};
			pose.mulPose(TechneModel.rotation(var11, 0.0F, 1.0F, 0.0F));
		}

		var vc = consumer(buf, MachineRenderer.tex(getImageFileName(tile)));
		if (tile.isCross()) {
			switch(tile.getBlockMetadata()) {
				case 7, 8 -> crossModel.renderAll(pose, vc, light, overlay, tile, null, tile.crossphi2, tile.crossphi1);
				default -> crossModel.renderAll(pose, vc, light, overlay, tile, null, -tile.crossphi2, tile.crossphi1); //4-way symmetry
			}
		}
		else if (tile.isVertical()) {
			vShaftModel.renderAll(pose, vc, light, overlay, tile, List.of(failed), -tile.phi, 0);
		}
		else {
			shaftModel.renderAll(pose, vc, light, overlay, tile, List.of(failed), -tile.phi, 0);
		}

		this.closeGL(tile, pose);
	}
}
