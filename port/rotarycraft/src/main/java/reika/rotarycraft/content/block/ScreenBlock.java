/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import reika.rotarycraft.content.blockentity.ScreenBlockEntity;
import reika.rotarycraft.content.menu.ScreenMenu;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: MachineRegistry.SCREEN in BlockDMIMachine - 4 orientations; a right click opens the dye slots ("Camera Select"),
 * sneak-right-click calls up the matching camera (BlockBasicMultiTE).
 */
public class ScreenBlock extends MachineBlock {

	public ScreenBlock(Properties props) {
		super(props, RotaryBlockEntities.SCREEN, Placement.FOUR_SIDED, false);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof ScreenBlockEntity te))
			return InteractionResult.PASS;
		if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp)) {
			if (ep.isShiftKeyDown()) {
				te.activate(sp);
				te.syncAllData();
			}
			else
				sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new ScreenMenu(id, inv, te), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
		}
		return InteractionResult.sidedSuccess(world.isClientSide);
	}
}
