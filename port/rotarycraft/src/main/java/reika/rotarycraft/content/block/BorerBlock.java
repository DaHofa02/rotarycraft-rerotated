/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import reika.rotarycraft.content.blockentity.BorerBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.content.menu.BorerMenu;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: MachineRegistry.BORER in BlockDMachine - a textured cube: borer_front on the side it digs towards (borer_front_active while
 * the power covers the slice), borer_back where the power comes in, steel elsewhere. Placed like the original ItemMachinePlacer does for
 * unmodelled 4-sided machines without inventory (4-sided metadata with 2 and 3 swapped: it digs towards the player). Right-click opens
 * the GUI (GuiBorer); with a drill in hand it replaces a worn-out drill (original BlockBasicMultiTE.onBlockActivated).
 */
public class BorerBlock extends MachineBlock {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	private static final MapCodec<BorerBlock> CODEC = simpleCodec(BorerBlock::new);

	public BorerBlock(Properties props) {
		super(props, RotaryBlockEntities.BORER, Placement.FOUR_SIDED_Z_FLIPPED, false);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.WEST).setValue(ACTIVE, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
		b.add(FACING, ACTIVE);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (ep.isShiftKeyDown() || !(world.getBlockEntity(pos) instanceof BorerBlockEntity te))
			return InteractionResult.PASS;
		if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
			sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new BorerMenu(id, inv, te), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
		return InteractionResult.sidedSuccess(world.isClientSide);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level world, BlockPos pos, Player ep, InteractionHand hand, BlockHitResult hit) {
		if (held.is(RotaryItems.part(CraftingPart.DRILL)) && world.getBlockEntity(pos) instanceof BorerBlockEntity te) {
			if (!world.isClientSide) {
				if (te.repair() && !ep.isCreative())
					held.shrink(1);
				te.syncAllData();
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		return super.useItemOn(held, state, world, pos, ep, hand, hit);
	}
}
