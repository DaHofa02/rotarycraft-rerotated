/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelPump;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.PumpBlockEntity;

/** Original: Reika.RotaryCraft.Renders.DM.RenderPump - the impeller turns with phi and is missing once the pump is broken. */
public class PumpRenderer extends MachineRenderer<PumpBlockEntity> {

	private final ModelPump model;

	public PumpRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelPump(ctx.bakeLayer(RotaryModelLayers.PUMP));
	}

	@Override
	public void render(PumpBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (isInWorld(tile))
			pose.mulPose(TechneModel.rotation(tile.getBlockMetadata() == 0 ? 90 : 0, 0.0F, 1.0F, 0.0F));
		model.renderAll(pose, consumer(buf, MachineRenderer.tex("pumptex.png")), light, overlay, tile, List.of(false, true, tile.isBroken()), -tile.phi, 0);
		pose.popPose();
	}
}
