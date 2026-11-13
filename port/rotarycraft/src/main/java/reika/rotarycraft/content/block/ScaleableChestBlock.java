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

import reika.rotarycraft.content.blockentity.ScaleableChestBlockEntity;
import reika.rotarycraft.content.menu.ScaleableChestMenu;
import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.SCALECHEST in BlockDMIMachine - four directions (the input side); opens page 0 of its GUI (GuiScaleChest) while powered. */
public class ScaleableChestBlock extends MachineBlock {

	public ScaleableChestBlock(Properties props) {
		super(props, RotaryBlockEntities.SCALEABLE_CHEST, Placement.FOUR_SIDED_Z_FLIPPED, false);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (ep.isShiftKeyDown() || !(world.getBlockEntity(pos) instanceof ScaleableChestBlockEntity te))
			return InteractionResult.PASS;
		if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
			ScaleableChestMenu.open(sp, te, 0);
		return InteractionResult.sidedSuccess(world.isClientSide);
	}
}
