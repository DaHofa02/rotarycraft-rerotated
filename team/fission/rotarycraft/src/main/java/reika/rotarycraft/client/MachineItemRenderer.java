/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.blockentity.ShaftBlockEntity;
import reika.rotarycraft.registry.RotaryComponents;

/**
 * Original: Reika.RotaryCraft.ItemMachineRenderer - items are drawn by the machine's own renderer on a level-less block entity
 * that carries the item's data (material, ratio, type).
 */
public class MachineItemRenderer extends BlockEntityWithoutLevelRenderer {

	private final Map<BlockItem, BlockEntity> instances = new HashMap<>();

	public MachineItemRenderer() {
		super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
		if (!(stack.getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof EntityBlock eb))
			return;
		BlockEntity te = instances.computeIfAbsent(bi, k -> eb.newBlockEntity(BlockPos.ZERO, bi.getBlock().defaultBlockState()));
		if (!(te instanceof RotaryCraftBlockEntity rte))
			return;
		rte.applyComponentsFromItemStack(stack);
		if (te instanceof ShaftBlockEntity)
			rte.setBlockMetadata(Boolean.TRUE.equals(stack.get(RotaryComponents.SHAFT_CROSS.get())) ? 6 : 0);
		BlockEntityRenderer<BlockEntity> r = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(te);
		if (r != null)
			r.render(te, 0, pose, buf, light, overlay);
	}
}
