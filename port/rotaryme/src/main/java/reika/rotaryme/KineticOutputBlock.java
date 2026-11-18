/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import reika.rotarycraft.content.block.MachineBlock;
import reika.rotarycraft.registry.RotaryItems;

/** The kinetic output: four-sided like an engine (placed looking east it drives west); right click opens its GUI, a lubricant bucket fills it. */
public class KineticOutputBlock extends MachineBlock {

	public KineticOutputBlock(Properties props) {
		super(props, RotaryME.KINETIC_OUTPUT_BE, Placement.FOUR_SIDED, false);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack is, BlockState state, Level world, BlockPos pos, Player ep, InteractionHand hand, BlockHitResult hit) {
		if (is.is(RotaryItems.LUBRICANT_BUCKET.get()) && world.getBlockEntity(pos) instanceof KineticOutputBlockEntity te && te.getLubricant() <= KineticOutputBlockEntity.TANK-1000) {
			if (!world.isClientSide) {
				te.addLubricant(1000);
				if (!ep.isCreative())
					ep.setItemInHand(hand, new ItemStack(Items.BUCKET));
				te.syncAllData();
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		return super.useItemOn(is, state, world, pos, ep, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (ep.isShiftKeyDown() || !(world.getBlockEntity(pos) instanceof KineticOutputBlockEntity te))
			return InteractionResult.PASS;
		if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
			sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new KineticOutputMenu(id, inv, te), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
		return InteractionResult.sidedSuccess(world.isClientSide);
	}
}
