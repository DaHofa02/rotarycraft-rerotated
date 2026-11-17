/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import reika.rotarycraft.content.blockentity.PowerBusBlockEntity;
import reika.rotarycraft.content.menu.PowerBusMenu;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: MachineRegistry.POWERBUS in BlockMachine - a full block; set down, it joins the bus next to it (ItemMachinePlacer
 * findNetwork); a right click opens the GUI, where the gear parts go in. (The original's sneak-right-click insertion in
 * BlockBasicMultiTE is never reached: sneaking with an item in hand skips the block, then as now.)
 */
public class PowerBusBlock extends MachineBlock {

	public PowerBusBlock(Properties props) {
		super(props, RotaryBlockEntities.POWER_BUS, Placement.NONE, false);
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);
		if (!world.isClientSide && world.getBlockEntity(pos) instanceof PowerBusBlockEntity te)
			te.findNetwork(world, pos);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (ep.isShiftKeyDown() || !(world.getBlockEntity(pos) instanceof PowerBusBlockEntity te))
			return InteractionResult.PASS;
		if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
			sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new PowerBusMenu(id, inv, te), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
		return InteractionResult.sidedSuccess(world.isClientSide);
	}
}
