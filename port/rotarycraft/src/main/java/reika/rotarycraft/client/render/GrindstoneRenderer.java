/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelGrindstone;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.GrindstoneBlockEntity;

/**
 * Original: Reika.RotaryCraft.Renders.DMI.RenderGrindstone - the model (grindstonetex.png) turned with its axle and the tool held
 * against the wheel, tilted by 12 degrees (drawn here as the item model rather than the original's flat sprite).
 */
public class GrindstoneRenderer extends MachineRenderer<GrindstoneBlockEntity> {

	private final ModelGrindstone model;

	public GrindstoneRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new ModelGrindstone(ctx.bakeLayer(RotaryModelLayers.GRINDSTONE));
	}

	@Override
	public void render(GrindstoneBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		int meta = isInWorld(tile) ? tile.getBlockMetadata() : 0;
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		if (meta == 1)
			pose.mulPose(TechneModel.rotation(90, 0.0F, 1.0F, 0.0F));
		model.renderAll(pose, consumer(buf, tex("grindstonetex.png")), light, overlay, tile, null, tile.phi, 0);
		pose.popPose();
		ItemStack is = tile.getInventory().getStackInSlot(0);
		if (isInWorld(tile) && !is.isEmpty()) {
			pose.pushPose();
			pose.translate(0.5, 0.75, 0.5);
			pose.mulPose(TechneModel.rotation(meta == 1 ? 0 : -90, 0.0F, 1.0F, 0.0F));
			pose.mulPose(TechneModel.rotation(meta == 1 ? 12 : -12, 1.0F, 0.0F, 0.0F));
			pose.scale(0.5F, 0.5F, 0.5F);
			Minecraft.getInstance().getItemRenderer().renderStatic(is, ItemDisplayContext.FIXED, light, overlay, pose, buf, tile.getLevel(), 0);
			pose.popPose();
		}
	}
}
