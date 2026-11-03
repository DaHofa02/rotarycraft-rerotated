/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelBedrockBreaker;
import reika.rotarycraft.client.model.ModelBedrockBreakerV;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.BedrockBreakerBlockEntity;

/**
 * Original: Reika.RotaryCraft.Renders.DM.RenderBedrockBreaker - horizontal or vertical model (bedrocktex / bedrockvtex) whose drill shaft
 * extends to the head ("step") and stretches into the slice being ground; infinite render box like the original.
 */
public class BedrockBreakerRenderer extends MachineRenderer<BedrockBreakerBlockEntity> {

	private final ModelBedrockBreaker model;
	private final ModelBedrockBreakerV modelV;

	public BedrockBreakerRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelBedrockBreaker(ctx.bakeLayer(RotaryModelLayers.BEDROCK_BREAKER));
		modelV = new ModelBedrockBreakerV(ctx.bakeLayer(RotaryModelLayers.BEDROCK_BREAKER_V));
	}

	@Override
	public void render(BedrockBreakerBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		boolean world = isInWorld(tile);
		int meta = world ? tile.getBlockMetadata() : 0;
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (world) {
			int var11 = switch(meta) {
				case 1 -> 180;
				case 2 -> -270;
				case 3 -> -90;
				case 4 -> 270;
				case 5 -> 90;
				default -> 0;
			};
			if (meta <= 3)
				pose.mulPose(TechneModel.rotation(var11, 0.0F, 1.0F, 0.0F));
			else {
				if (meta == 4)
					pose.mulPose(TechneModel.rotation(180, 0.0F, 1.0F, 0.0F));
				pose.mulPose(TechneModel.rotation(var11, 0F, 0F, 1F));
				pose.translate(-1F, -1F, 0F);
				if (meta == 5)
					pose.translate(2F, 0F, 0F);
			}
		}
		float f = world ? tile.getGrindFraction() : 0;
		List<Object> li = List.of(world ? tile.getStep() : 1, f);
		if (world && meta > 3)
			modelV.renderAll(pose, consumer(buf, MachineRenderer.tex("bedrockvtex.png")), light, overlay, tile, li, tile.phi, 0);
		else
			model.renderAll(pose, consumer(buf, MachineRenderer.tex("bedrocktex.png")), light, overlay, tile, li, tile.phi, 0);
		pose.popPose();
	}

	@Override
	public AABB getRenderBoundingBox(BedrockBreakerBlockEntity te) {
		return AABB.INFINITE;
	}

	@Override
	public boolean shouldRenderOffScreen(BedrockBreakerBlockEntity te) {
		return true;
	}
}
