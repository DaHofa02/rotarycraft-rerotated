/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.client.RotaryModelLayers;
import reika.rotarycraft.client.model.ModelCctv;
import reika.rotarycraft.content.blockentity.ProjectorBlockEntity;

/**
 * Own rendering: the projector body (the CCTV camera model in projector colours, pointing where it projects) and, while it can show, the map
 * picture on the screen face, lit full bright.
 */
public class ProjectorRenderer implements BlockEntityRenderer<ProjectorBlockEntity> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/machine/own/projector.png");

	private final ModelCctv body;

	public ProjectorRenderer(BlockEntityRendererProvider.Context ctx) {
		body = new ModelCctv(ctx.bakeLayer(RotaryModelLayers.CCTV));
	}

	@Override
	public void render(ProjectorBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		Direction dir = tile.getFacing();
		pose.pushPose();
		pose.translate(0, 2.0F, 1.0F);
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		body.renderAll(pose, buf.getBuffer(net.minecraft.client.renderer.RenderType.entityCutout(TEX)), light, overlay, tile, null, 180-dir.toYRot(), 0);
		pose.popPose();
		if (!tile.canShow() || tile.getLevel() == null)
			return;
		ItemStack map = tile.getInventory().getStackInSlot(0);
		MapId id = map.get(DataComponents.MAP_ID);
		MapItemSavedData data = MapItem.getSavedData(map, tile.getLevel());
		if (id == null || data == null)
			return;
		int r = tile.getRange(), size = tile.getImageSize();
		pose.pushPose();
		//to the middle of the screen face, facing back at the projector
		pose.translate(0.5+dir.getStepX()*(r-0.51), 0.5, 0.5+dir.getStepZ()*(r-0.51));
		pose.mulPose(Axis.YP.rotationDegrees(180-dir.toYRot()));
		pose.mulPose(Axis.ZP.rotationDegrees(180));
		float s = size/128F;
		pose.scale(s, s, s);
		pose.translate(-64, -64, 0);
		Minecraft.getInstance().gameRenderer.getMapRenderer().render(pose, buf, id, data, true, LightTexture.FULL_BRIGHT);
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen(ProjectorBlockEntity tile) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 96;
	}
}
