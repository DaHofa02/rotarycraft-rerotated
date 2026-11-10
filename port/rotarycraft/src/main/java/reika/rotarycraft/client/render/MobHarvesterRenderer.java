/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelHarvester;
import reika.rotarycraft.content.blockentity.MobHarvesterBlockEntity;

/**
 * Original: Reika.RotaryCraft.Renders.M.RenderHarvester - the model without orientation, and while a mob stands in the column the laser:
 * a red core (0.2 wide) inside a pale red sheath (0.125 wider). The original also outlined the damage column in orange after a
 * screwdriver click (with the I/O overlay); that outline and the enchantment glint are not drawn.
 */
public class MobHarvesterRenderer extends MachineRenderer<MobHarvesterBlockEntity> {

	private final ModelHarvester model;

	public MobHarvesterRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelHarvester(ctx.bakeLayer(RotaryModelLayers.MOB_HARVESTER));
	}

	@Override
	public void render(MobHarvesterBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		model.renderAll(pose, consumer(buf, tex("harvestertex.png")), light, overlay, tile, null, 0, 0);
		pose.popPose();
		if (isInWorld(tile) && tile.laser) {
			BlockPos p = tile.getBlockPos();
			AABB core = tile.getLaser().move(-p.getX(), -p.getY(), -p.getZ());
			AABB sheath = core.inflate(0.125, 0.001, 0.125);
			box(pose, buf, core, 1F, 0F, 0F, 0.75F);
			box(pose, buf, sheath, 1F, 0.5F, 0.5F, 0.4F);
		}
	}

	private static void box(PoseStack pose, MultiBufferSource buf, AABB b, float r, float g, float bl, float a) {
		LevelRenderer.addChainedFilledBoxVertices(pose, buf.getBuffer(RenderType.debugFilledBox()), b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, r, g, bl, a);
	}

	@Override
	public AABB getRenderBoundingBox(MobHarvesterBlockEntity te) {
		return new AABB(te.getBlockPos()).expandTowards(0, 7, 0);
	}
}
