/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelFlywheel;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.FlywheelBlockEntity;

/** Original: Reika.RotaryCraft.Renders.RenderFlywheel */
public class FlywheelRenderer extends MachineRenderer<FlywheelBlockEntity> {

	private final ModelFlywheel model;

	public FlywheelRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelFlywheel(ctx.bakeLayer(RotaryModelLayers.FLYWHEEL));
	}

	@Override
	public void render(FlywheelBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		this.setupGL(tile, pose);
		if (isInWorld(tile)) {
			int var11 = switch(tile.getBlockMetadata()%4) {
				case 0 -> 180;
				case 2 -> 270;
				case 3 -> 90;
				default -> 0;
			};
			pose.mulPose(TechneModel.rotation(var11, 0.0F, 1.0F, 0.0F));
		}
		var vc = consumer(buf, MachineRenderer.tex("transmission/flywheel/"+tile.getTypeOrdinal().getSerializedName()+".png"));
		model.renderAll(pose, vc, light, overlay, tile, List.of(tile.failed), -tile.phi, 0);
		this.closeGL(tile, pose);
	}
}
