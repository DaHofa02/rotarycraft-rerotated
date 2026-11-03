/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

import reika.rotarycraft.content.blockentity.DryingBedBlockEntity;
import reika.rotarycraft.content.menu.DryingBedMenu;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: MachineRegistry.DRYING in BlockMIMachine (BlockBasicMultiTE.onBlockActivated) - a filled bucket of a dryable liquid is
 * emptied into the drying bed if it all fits and matches; otherwise right-click opens its GUI.
 */
public class DryingBedBlock extends MachineBlock {

	public DryingBedBlock(Properties props) {
		super(props, RotaryBlockEntities.DRYING_BED, Placement.NONE, false);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack is, BlockState state, Level world, BlockPos pos, Player ep, InteractionHand hand, BlockHitResult hit) {
		ItemInteractionResult tool = super.useItemOn(is, state, world, pos, ep, hand, hit);
		if (tool != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION)
			return tool;
		if (world.getBlockEntity(pos) instanceof DryingBedBlockEntity tr && tr.isPlayerAccessible(ep)) {
			var liq = FluidUtil.getFluidContained(is);
			if (liq.isPresent() && is.getItem() instanceof net.minecraft.world.item.BucketItem) {
				FluidStack f = liq.get();
				int size = is.getCount();
				if (!world.isClientSide && tr.addLiquid(new FluidStack(f.getFluid(), size*f.getAmount())) && !ep.isCreative())
					ep.setItemInHand(hand, new ItemStack(Items.BUCKET, size));
				return ItemInteractionResult.sidedSuccess(world.isClientSide);
			}
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (ep.isShiftKeyDown() || !(world.getBlockEntity(pos) instanceof DryingBedBlockEntity te))
			return InteractionResult.PASS;
		if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
			sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new DryingBedMenu(id, inv, te), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
		return InteractionResult.sidedSuccess(world.isClientSide);
	}
}
