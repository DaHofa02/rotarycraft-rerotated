/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import reika.rotarycraft.content.blockentity.CaveScannerBlockEntity;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: MachineRegistry.CAVESCANNER in BlockMMachine - no orientation; a right click moves the scanned cube's source 4 blocks the way
 * the player looks, sneaking 4 the other way (BlockBasicMultiTE, ReikaEntityHelper.getDirectionFromEntityLook with vertical).
 */
public class CaveScannerBlock extends MachineBlock {

	public CaveScannerBlock(Properties props) {
		super(props, RotaryBlockEntities.CAVE_SCANNER, Placement.NONE, false);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof CaveScannerBlockEntity te) || !te.isPlayerAccessible(ep))
			return InteractionResult.PASS;
		if (!world.isClientSide) {
			Direction dir = Direction.orderedByNearest(ep)[0];
			te.moveSrc(ep.isShiftKeyDown() ? -4 : 4, dir);
		}
		return InteractionResult.sidedSuccess(world.isClientSide);
	}
}
