/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelWoodcutter;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.WoodcutterBlockEntity;

/** Original: Reika.RotaryCraft.Renders.DM.RenderWoodcutter - the woodcutter turned by its metadata, saw blades turning with phi. */
public class WoodcutterRenderer extends MachineRenderer<WoodcutterBlockEntity> {

	private final ModelWoodcutter model;

	public WoodcutterRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelWoodcutter(ctx.bakeLayer(RotaryModelLayers.WOODCUTTER));
	}

	@Override
	public void render(WoodcutterBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (isInWorld(tile)) {
			int var11 = switch (tile.getBlockMetadata()) {
				case 0 -> 180;
				case 2 -> 270;
				case 3 -> 90;
				default -> 0;
			};
			pose.mulPose(TechneModel.rotation(var11-90, 0, 1, 0));
		}
		model.renderAll(pose, consumer(buf, tex("woodcuttertex.png")), light, overlay, tile, null, isInWorld(tile) ? tile.phi : 0, 0);
		pose.popPose();
	}
}
