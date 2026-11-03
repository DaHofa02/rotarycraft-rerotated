/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import reika.rotarycraft.content.blockentity.ExtractorBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.content.menu.ExtractorMenu;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: MachineRegistry.EXTRACTOR in BlockMIMachine and the extractor branch of BlockBasicMultiTE.onBlockActivated: a water bucket
 * adds 1000 mB, a bedrock drill upgrades the drill; otherwise the GUI opens.
 */
public class ExtractorBlock extends MachineBlock {

	private static final MapCodec<ExtractorBlock> CODEC = simpleCodec(ExtractorBlock::new);

	public ExtractorBlock(Properties props) {
		super(props, RotaryBlockEntities.EXTRACTOR, Placement.NONE, false);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack is, BlockState state, Level world, BlockPos pos, Player ep, InteractionHand hand, BlockHitResult hit) {
		ItemInteractionResult tool = super.useItemOn(is, state, world, pos, ep, hand, hit);
		if (tool != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION)
			return tool;
		if (!(world.getBlockEntity(pos) instanceof ExtractorBlockEntity ex))
			return tool;
		if (is.is(Items.WATER_BUCKET) && is.getCount() == 1 && ex.getWater()+1000 <= ExtractorBlockEntity.CAPACITY) {
			if (!world.isClientSide) {
				ex.addWater(1000);
				if (!ep.isCreative())
					ep.setItemInHand(hand, new ItemStack(Items.BUCKET));
				ex.syncAllData();
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		if (is.is(RotaryItems.part(CraftingPart.BEDROCKDRILL))) {
			if (!world.isClientSide && ex.upgrade(world) && !ep.isCreative())
				is.shrink(1);
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (ep.isShiftKeyDown() || !(world.getBlockEntity(pos) instanceof ExtractorBlockEntity te))
			return InteractionResult.PASS;
		if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
			sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new ExtractorMenu(id, inv, te), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
		return InteractionResult.sidedSuccess(world.isClientSide);
	}
}
