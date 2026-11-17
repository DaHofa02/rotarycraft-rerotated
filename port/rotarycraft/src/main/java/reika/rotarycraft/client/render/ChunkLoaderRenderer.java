/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelChunkLoader;
import reika.rotarycraft.content.blockentity.ChunkLoaderBlockEntity;

/** Original: Reika.RotaryCraft.Renders.MI.RenderChunkLoader - its four rings turning with phi. */
public class ChunkLoaderRenderer extends MachineRenderer<ChunkLoaderBlockEntity> {

	private final ModelChunkLoader model;

	public ChunkLoaderRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelChunkLoader(ctx.bakeLayer(RotaryModelLayers.CHUNK_LOADER));
	}

	@Override
	public void render(ChunkLoaderBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		model.renderAll(pose, consumer(buf, tex("chunkloadertex.png")), light, overlay, tile, null, tile.phi, 0);
		pose.popPose();
	}
}
