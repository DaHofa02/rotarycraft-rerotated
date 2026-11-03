/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelFin;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.CoolingFinBlockEntity;

/**
 * Original: Reika.RotaryCraft.Renders.DM.RenderFin - the fin model turned towards its target (the plate count follows the setting;
 * the item shows all plates without the mount) and, for a moment after using the screwdriver, a blue outline of the target block.
 */
public class CoolingFinRenderer extends MachineRenderer<CoolingFinBlockEntity> {

	private final ModelFin model;

	public CoolingFinRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelFin(ctx.bakeLayer(RotaryModelLayers.COOLING_FIN));
	}

	@Override
	public void render(CoolingFinBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		boolean inWorld = isInWorld(tile);
		if (inWorld) {
			int meta = tile.getBlockMetadata();
			int var11 = switch (meta) {
				case 1, 4 -> 180;
				case 3 -> 90;
				case 5 -> 270;
				default -> 0;
			};
			if (meta < 2) {
				pose.mulPose(TechneModel.rotation(var11, 0, 0, 1));
				if (meta == 1)
					pose.translate(0, -2, 0);
			}
			else {
				pose.mulPose(TechneModel.rotation(90, 1, 0, 0));
				pose.mulPose(TechneModel.rotation(var11, 0, 0, 1));
				pose.translate(0, -1, -1);
			}
		}
		model.renderAll(pose, consumer(buf, tex("fintex.png")), light, overlay, tile, List.of(inWorld, tile.setting), -tile.phi, 0);
		pose.popPose();
		if (inWorld && tile.ticks > 0)
			this.renderTarget(tile, pose, buf);
	}

	private void renderTarget(CoolingFinBlockEntity tile, PoseStack pose, MultiBufferSource buf) {
		BlockPos t = tile.getTarget().subtract(tile.getBlockPos());
		AABB box = new AABB(t).inflate(0.03125);
		float a = Math.min(255, tile.ticks)/255F;
		LevelRenderer.renderLineBox(pose, buf.getBuffer(RenderType.lines()), box, 0, 127/255F, 1, a);
	}

	@Override
	public AABB getRenderBoundingBox(CoolingFinBlockEntity te) {
		return new AABB(te.getBlockPos()).inflate(2);
	}
}
