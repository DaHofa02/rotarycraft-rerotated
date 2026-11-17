/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelSpawner;
import reika.rotarycraft.content.blockentity.SpawnerControllerBlockEntity;

/** Original: Reika.RotaryCraft.Renders.MI.RenderSpawner - the controller cap. */
public class SpawnerControllerRenderer extends MachineRenderer<SpawnerControllerBlockEntity> {

	private final ModelSpawner model;

	public SpawnerControllerRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelSpawner(ctx.bakeLayer(RotaryModelLayers.SPAWNER_CONTROLLER));
	}

	@Override
	public void render(SpawnerControllerBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		model.renderAll(pose, consumer(buf, tex("spawnertex.png")), light, overlay, tile, null, 0, 0);
		pose.popPose();
	}
}
