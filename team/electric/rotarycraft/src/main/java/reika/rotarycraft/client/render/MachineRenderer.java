/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.client.model.TechneModel;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/**
 * Original: Reika.RotaryCraft.Base.RotaryTERenderer. {@link #setupGL} reproduces its transform chain
 * (scale 1,-1,-1; +0.5; 0,-2,-1; optional flip), so the converted models sit exactly as in 1.7.10.
 * Block entities without a level are being drawn as items (the original "!isInWorld()" branch).
 */
public abstract class MachineRenderer<T extends RotaryCraftBlockEntity> implements BlockEntityRenderer<T> {

	public static ResourceLocation tex(String path) {
		return ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/machine/"+path);
	}

	protected static boolean isInWorld(RotaryCraftBlockEntity te) {
		return te.getLevel() != null;
	}

	protected static VertexConsumer consumer(MultiBufferSource buf, ResourceLocation texture) {
		return buf.getBuffer(RenderType.entityCutout(texture));
	}

	protected void setupGL(T tile, PoseStack pose) {
		pose.pushPose();
		pose.scale(1.0F, -1.0F, -1.0F);
		pose.translate(0.5F, 0.5F, 0.5F);
		pose.translate(0, -2, -1);

		if (isInWorld(tile) && tile.isFlipped) {
			pose.mulPose(TechneModel.rotation(180, 1, 0, 0));
			pose.translate(0, -2, 0);
			if (this.flipRotatesY(tile))
				pose.mulPose(TechneModel.rotation(180, 0, 1, 0));
		}
	}

	/** Original: rot = tile.getBlockMetadata() > 1 for ordinary machines. */
	protected boolean flipRotatesY(T tile) {
		return tile.getBlockMetadata() > 1;
	}

	protected void closeGL(T tile, PoseStack pose) {
		pose.popPose();
	}

	@Override
	public AABB getRenderBoundingBox(T te) {
		return new AABB(te.getBlockPos()).inflate(1);
	}
}
