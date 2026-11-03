/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelSplitter;
import reika.rotarycraft.client.model.ModelSplitter2;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.SplitterBlockEntity;

/** Original: Reika.RotaryCraft.Renders.RenderSplitter */
public class SplitterRenderer extends MachineRenderer<SplitterBlockEntity> {

	private final ModelSplitter model;
	private final ModelSplitter2 model2;

	public SplitterRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelSplitter(ctx.bakeLayer(RotaryModelLayers.SPLITTER));
		model2 = new ModelSplitter2(ctx.bakeLayer(RotaryModelLayers.SPLITTER2));
	}

	@Override
	public void render(SplitterBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		this.setupGL(tile, pose);
		int meta;
		if (isInWorld(tile)) {
			meta = tile.getBlockMetadata();
			int var11 = switch(meta) {
				case 0, 4, 12 -> -90;
				case 8 -> 270;
				case 2, 6, 10, 14 -> 90;
				case 3, 7, 11, 15 -> 180;
				default -> 0;
			};
			pose.mulPose(TechneModel.rotation((float)var11-90, 0.0F, 1.0F, 0.0F));
		}
		else {
			meta = 0;
			pose.mulPose(TechneModel.rotation(180F, 0.0F, 1.0F, 0.0F));
		}
		var vc = consumer(buf, MachineRenderer.tex("transmission/"+(tile.isBedrock() ? "bedsplittertex" : "splittertex")+".png"));
		if (meta < 4 || (meta >= 8 && meta < 12))
			model.renderAll(pose, vc, light, overlay, tile, List.of(tile.failed), -tile.phi, 0);
		else
			model2.renderAll(pose, vc, light, overlay, tile, List.of(tile.failed), -tile.phi, 0);
		this.closeGL(tile, pose);
	}
}
