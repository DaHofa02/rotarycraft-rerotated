package xyz.dahofa.rotarycraft.common.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import xyz.dahofa.rotarycraft.common.entity.block.GrinderBlockEntity;
import xyz.dahofa.rotarycraft.common.registry.RCItems;

public class GrinderBlockEntityRenderer implements BlockEntityRenderer<GrinderBlockEntity> {
    public GrinderBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(GrinderBlockEntity grinderBlockEntity, float pPartialTick, PoseStack poseStack, MultiBufferSource multiBufferSource, int pPackedLight, int pPackedOverlay) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        ItemStack stack = RCItems.IRON_FLYWHEEL_CORE.get().getDefaultInstance();

        poseStack.pushPose();
        poseStack.translate(0.5F, 1.15F, 0.5F);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, getLightLevel(grinderBlockEntity.getLevel(), grinderBlockEntity.getBlockPos()), OverlayTexture.NO_OVERLAY, poseStack, multiBufferSource, grinderBlockEntity.getLevel(), 1);
        poseStack.popPose();
    }

    private int getLightLevel(Level level, BlockPos pos) {
        int bLight = level.getBrightness(LightLayer.BLOCK, pos);
        int sLight = level.getBrightness(LightLayer.SKY, pos);
        return LightTexture.pack(bLight, sLight);
    }
}
