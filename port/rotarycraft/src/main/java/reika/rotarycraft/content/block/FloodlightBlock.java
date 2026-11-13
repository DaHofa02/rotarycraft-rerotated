/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import reika.rotarycraft.content.blockentity.FloodlightBlockEntity;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: MachineRegistry.FLOODLIGHT in BlockDMMachine - 0.875 high (full in beam mode), 6 orientations with north/south swapped (isZFlipped).
 * Right-clicking it with a lens installs the fresnel lens (original BlockBasicMultiTE.onBlockActivated).
 */
public class FloodlightBlock extends MachineBlock {
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);


	public FloodlightBlock(Properties props) {
		super(props, RotaryBlockEntities.FLOODLIGHT, Placement.SIX_SIDED_Z_FLIPPED, false);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return level.getBlockEntity(pos) instanceof FloodlightBlockEntity te && te.beammode ? Shapes.block() : SHAPE;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level world, BlockPos pos, Player ep, InteractionHand hand, BlockHitResult hit) {
		if (world.getBlockEntity(pos) instanceof FloodlightBlockEntity te && te.canUpgradeWith(held)) {
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
