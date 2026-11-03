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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import reika.rotarycraft.content.blockentity.FermenterBlockEntity;
import reika.rotarycraft.content.menu.FermenterMenu;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: MachineRegistry.FERMENTER in BlockIMachine - a steel cube with ferm_front on the side the 4-sided metadata picks,
 * ferm_front_active while it works and ferm_back opposite; a water bucket adds 1000 mB (BlockBasicMultiTE.onBlockActivated).
 */
public class FermenterBlock extends MachineBlock {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	private static final MapCodec<FermenterBlock> CODEC = simpleCodec(FermenterBlock::new);

	public FermenterBlock(Properties props) {
		super(props, RotaryBlockEntities.FERMENTER, Placement.FOUR_SIDED, false);
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
	protected ItemInteractionResult useItemOn(ItemStack is, BlockState state, Level world, BlockPos pos, Player ep, InteractionHand hand, BlockHitResult hit) {
		ItemInteractionResult tool = super.useItemOn(is, state, world, pos, ep, hand, hit);
		if (tool != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION)
			return tool;
		if (world.getBlockEntity(pos) instanceof FermenterBlockEntity fm && is.is(Items.WATER_BUCKET) && fm.getWater()+1000 <= FermenterBlockEntity.CAPACITY) {
			if (!world.isClientSide) {
				fm.addWater(1000);
				if (!ep.isCreative())
					ep.setItemInHand(hand, new ItemStack(Items.BUCKET));
				fm.syncAllData();
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (ep.isShiftKeyDown() || !(world.getBlockEntity(pos) instanceof FermenterBlockEntity te))
			return InteractionResult.PASS;
		if (!world.isClientSide && ep instanceof ServerPlayer sp && te.isPlayerAccessible(sp))
			sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new FermenterMenu(id, inv, te), state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
		return InteractionResult.sidedSuccess(world.isClientSide);
	}
}
