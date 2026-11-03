/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelWinder;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.WinderBlockEntity;
import reika.rotarycraft.content.item.CoilItem;

/** Original: Reika.RotaryCraft.Renders.DMI.RenderWinder - the winder turned by its metadata, the spool by -phi, the coil when one is in. */
public class WinderRenderer extends MachineRenderer<WinderBlockEntity> {

	private final ModelWinder model;

	public WinderRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelWinder(ctx.bakeLayer(RotaryModelLayers.WINDER));
	}

	@Override
	public void render(WinderBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (isInWorld(tile)) {
			int meta = tile.getBlockMetadata();
			int var11 = switch (meta) {
				case 0 -> 180;
				case 2 -> 270;
				case 3 -> 90;
				default -> 0;
			};
			if (meta <= 3)
				pose.mulPose(TechneModel.rotation(var11-90, 0, 1, 0));
			else {
				pose.mulPose(TechneModel.rotation(var11, 1, 0, 0));
				pose.translate(0, -1, 1);
				if (meta == 5)
					pose.translate(0, 0, -2);
			}
		}
		boolean hasSpring = tile.getInventory().getStackInSlot(0).getItem() instanceof CoilItem;
		model.renderAll(pose, consumer(buf, tex("windertex.png")), light, overlay, tile,
				List.of(hasSpring, (java.util.function.Supplier<com.mojang.blaze3d.vertex.VertexConsumer>)() -> buf.getBuffer(RenderType.entityCutoutNoCull(tex("windertex.png")))), -tile.phi, 0);
		pose.popPose();
	}
}
