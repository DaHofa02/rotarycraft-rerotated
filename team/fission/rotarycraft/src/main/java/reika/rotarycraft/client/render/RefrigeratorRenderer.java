/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelFridge;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.RefrigeratorBlockEntity;

/** Original: Reika.RotaryCraft.Renders.DMI.RenderFridge - the refrigerator turned by its metadata. */
public class RefrigeratorRenderer extends MachineRenderer<RefrigeratorBlockEntity> {

	private final ModelFridge model;

	public RefrigeratorRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelFridge(ctx.bakeLayer(RotaryModelLayers.REFRIGERATOR));
	}

	@Override
	public void render(RefrigeratorBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (isInWorld(tile)) {
			int var11 = switch (tile.getBlockMetadata()) {
				case 1 -> 180;
				case 2 -> 90;
				case 3 -> 270;
				default -> 0;
			};
			pose.mulPose(TechneModel.rotation(var11-90, 0, 1, 0));
		}
		model.renderAll(pose, consumer(buf, tex("fridgetex.png")), light, overlay, tile, null, -tile.phi, 0);
		pose.popPose();
	}
}
