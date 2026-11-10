/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import reika.rotarycraft.content.blockentity.FanBlockEntity;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: MachineRegistry.FAN in BlockDMMachine - a full block, 6 orientations with north/south swapped (isZFlipped: it blows towards
 * the player who set it down). Right-clicking it with a diffuser installs the wide-fan upgrade (original ItemBasic.onItemUse on an
 * UpgradeableMachine).
 */
public class FanBlock extends MachineBlock {

	public FanBlock(Properties props) {
		super(props, RotaryBlockEntities.FAN, Placement.SIX_SIDED_Z_FLIPPED, false);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level world, BlockPos pos, Player ep, InteractionHand hand, BlockHitResult hit) {
		if (world.getBlockEntity(pos) instanceof FanBlockEntity te && te.canUpgradeWith(held)) {
			if (!world.isClientSide) {
				te.upgrade(held);
				te.syncAllData();
				if (!ep.isCreative())
					held.shrink(1);
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		return super.useItemOn(held, state, world, pos, ep, hand, hit);
	}
}
