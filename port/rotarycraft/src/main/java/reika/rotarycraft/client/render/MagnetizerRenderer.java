/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelMagnetizer;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.MagnetizerBlockEntity;

/** Original: Reika.RotaryCraft.Renders.DMI.RenderMagnetizer - the model (magnettex.png) with the core shown while one is in. */
public class MagnetizerRenderer extends MachineRenderer<MagnetizerBlockEntity> {

	private final ModelMagnetizer model;

	public MagnetizerRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelMagnetizer(ctx.bakeLayer(RotaryModelLayers.MAGNETIZER));
	}

	@Override
	public void render(MagnetizerBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (isInWorld(tile)) {
			int r = switch (tile.getBlockMetadata()) {
				case 0 -> 180;
				case 2 -> 270;
				case 3 -> 90;
				default -> 0;
			};
			pose.mulPose(TechneModel.rotation(r-90, 0.0F, 1.0F, 0.0F));
		}
		model.renderAll(pose, consumer(buf, tex("magnettex.png")), light, overlay, tile, List.of(tile.hasCore()), -tile.phi, 0);
		pose.popPose();
	}
}
