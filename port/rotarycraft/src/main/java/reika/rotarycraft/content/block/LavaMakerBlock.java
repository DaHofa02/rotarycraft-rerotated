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

import reika.rotarycraft.content.blockentity.LavaMakerBlockEntity;
import reika.rotarycraft.content.menu.LavaMakerMenu;
import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.LAVAMAKER in BlockMIMachine - no orientation, opens its GUI on right-click. */
public class LavaMakerBlock extends MachineBlock {

	public LavaMakerBlock(Properties props) {
		super(props, RotaryBlockEntities.LAVA_MAKER, Placement.NONE, false);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (ep.isShiftKeyDown() || !(world.getBlockEntity(pos) instanceof LavaMakerBlockEntity te))
			return InteractionResult.PASS;
		if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
			sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new LavaMakerMenu(id, inv, te), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
		return InteractionResult.sidedSuccess(world.isClientSide);
	}
}
