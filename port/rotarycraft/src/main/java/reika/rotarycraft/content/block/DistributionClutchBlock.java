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

import reika.rotarycraft.content.blockentity.DistributionClutchBlockEntity;
import reika.rotarycraft.content.menu.DistributionClutchMenu;
import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.DISTRIBCLUTCH in BlockDMachine - 4 orientations; a right click opens the side and torque GUI. */
public class DistributionClutchBlock extends MachineBlock {

	public DistributionClutchBlock(Properties props) {
		super(props, RotaryBlockEntities.DISTRIBUTION_CLUTCH, Placement.FOUR_SIDED, false);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (ep.isShiftKeyDown() || !(world.getBlockEntity(pos) instanceof DistributionClutchBlockEntity te))
			return InteractionResult.PASS;
		if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
			sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new DistributionClutchMenu(id, inv, te), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
		return InteractionResult.sidedSuccess(world.isClientSide);
	}
}
