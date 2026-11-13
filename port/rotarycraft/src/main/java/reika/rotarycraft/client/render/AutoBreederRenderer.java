/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelBreeder;
import reika.rotarycraft.content.blockentity.AutoBreederBlockEntity;

/** Original: Reika.RotaryCraft.Renders.MI.RenderBreeder - breedertex.png while it holds wheat, else emptybreedertex.png. */
public class AutoBreederRenderer extends MachineRenderer<AutoBreederBlockEntity> {

	private final ModelBreeder model;

	public AutoBreederRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelBreeder(ctx.bakeLayer(RotaryModelLayers.AUTO_BREEDER));
	}

	private static boolean hasWheat(AutoBreederBlockEntity te) {
		for (int i = 0; i < te.getInventory().getSlots(); i++)
			if (te.getInventory().getStackInSlot(i).is(net.minecraft.world.item.Items.WHEAT))
				return true;
		return false;
	}

	@Override
	public void render(AutoBreederBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		model.renderAll(pose, consumer(buf, tex(hasWheat(tile) ? "breedertex.png" : "emptybreedertex.png")), light, overlay, tile, null, 0, 0);
		pose.popPose();
	}
}
