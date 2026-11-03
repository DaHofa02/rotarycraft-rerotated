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
import reika.rotarycraft.client.model.ModelMirror;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.MirrorBlockEntity;

/** Original: Reika.RotaryCraft.Renders.RenderMirror - the heliostat turned by -phi and tilted by -theta/2; a broken one has no glass. */
public class MirrorRenderer extends MachineRenderer<MirrorBlockEntity> {

	private final ModelMirror model;

	public MirrorRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelMirror(ctx.bakeLayer(RotaryModelLayers.MIRROR));
	}

	@Override
	public void render(MirrorBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (!isInWorld(tile))
			pose.mulPose(TechneModel.rotation(-90, 0, 1, 0));
		model.renderAll(pose, consumer(buf, tex("mirrortex.png")), light, overlay, tile, List.of(tile.broken), -tile.phi, -tile.theta/2);
		pose.popPose();
	}

	@Override
	public AABB getRenderBoundingBox(MirrorBlockEntity te) {
		return new AABB(te.getBlockPos()).inflate(1);
	}
}
