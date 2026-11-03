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

import reika.rotarycraft.content.blockentity.FractionatorBlockEntity;
import reika.rotarycraft.content.menu.FractionatorMenu;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: MachineRegistry.FRACTIONATOR in BlockMIMachine and its branch of BlockBasicMultiTE.onBlockActivated: an ethanol bucket
 * pours 1000 mB in, an empty bucket takes 1000 mB of jet fuel out (single buckets only); otherwise the GUI opens.
 */
public class FractionatorBlock extends MachineBlock {

	private static final MapCodec<FractionatorBlock> CODEC = simpleCodec(FractionatorBlock::new);

	public FractionatorBlock(Properties props) {
		super(props, RotaryBlockEntities.FRACTIONATOR, Placement.NONE, false);
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
		if (!(world.getBlockEntity(pos) instanceof FractionatorBlockEntity tf) || is.getCount() != 1)
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (is.is(RotaryItems.ETHANOL_BUCKET.get()) && tf.getInputLevel()+1000 <= FractionatorBlockEntity.INPUT_CAPACITY) {
			if (!world.isClientSide) {
				tf.addEthanol(1000);
				if (!ep.isCreative())
					ep.setItemInHand(hand, new ItemStack(Items.BUCKET));
				tf.syncAllData();
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		if (is.is(Items.BUCKET) && tf.getFuelLevel() >= 1000) {
			if (!world.isClientSide) {
				tf.removeFuel(1000);
				if (!ep.isCreative())
					ep.setItemInHand(hand, new ItemStack(RotaryItems.JET_FUEL_BUCKET.get()));
				tf.syncAllData();
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (ep.isShiftKeyDown() || !(world.getBlockEntity(pos) instanceof FractionatorBlockEntity te))
			return InteractionResult.PASS;
		if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
			sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new FractionatorMenu(id, inv, te), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
		return InteractionResult.sidedSuccess(world.isClientSide);
	}
}
