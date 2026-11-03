/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.content.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.electricraft.content.blockentity.RFCableBlockEntity;
import reika.electricraft.content.menu.RFCableMenu;
import reika.electricraft.registry.ElectriBlockEntities;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/**
 * Original: BlockRFCable / BlockElectriCable (hardness 0.5, resistance 15) - box 0.3-0.7 extended towards connected sides; right-click
 * (except with another cable) opens the network limit GUI.
 */
public class RFCableBlock extends BaseEntityBlock {

	private static final MapCodec<RFCableBlock> CODEC = simpleCodec(RFCableBlock::new);

	public RFCableBlock(Properties props) {
		super(props);
		BlockState s = this.stateDefinition.any();
		for (BooleanProperty p : PipeBlock.PROPERTY_BY_DIRECTION.values())
			s = s.setValue(p, false);
		this.registerDefaultState(s);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PipeBlock.NORTH, PipeBlock.EAST, PipeBlock.SOUTH, PipeBlock.WEST, PipeBlock.UP, PipeBlock.DOWN);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		double lo = 0.3*16, hi = 0.7*16;
		return Block.box(state.getValue(PipeBlock.WEST) ? 0 : lo, state.getValue(PipeBlock.DOWN) ? 0 : lo, state.getValue(PipeBlock.NORTH) ? 0 : lo,
				state.getValue(PipeBlock.EAST) ? 16 : hi, state.getValue(PipeBlock.UP) ? 16 : hi, state.getValue(PipeBlock.SOUTH) ? 16 : hi);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RFCableBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> given) {
		return createTickerHelper(given, ElectriBlockEntities.RF_CABLE.get(), RotaryCraftBlockEntity::tick);
	}

	@Override
	protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos from, boolean moving) {
		super.neighborChanged(state, world, pos, block, from, moving);
		if (!world.isClientSide && world.getBlockEntity(pos) instanceof RFCableBlockEntity te)
			te.recomputeConnections(world, pos);
	}

	@Override
	protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
		if (!world.isClientSide && !state.is(newState.getBlock()) && world.getBlockEntity(pos) instanceof RFCableBlockEntity te)
			te.removeFromNetwork();
		super.onRemove(state, world, pos, newState, moved);
	}

	private static InteractionResult open(Level world, BlockPos pos, Player ep) {
		if (!world.isClientSide && ep instanceof ServerPlayer sp && world.getBlockEntity(pos) instanceof RFCableBlockEntity te && te.isPlayerAccessible(sp))
			sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new RFCableMenu(id, inv, te), te.getBlockState().getBlock().getName()), buf -> buf.writeBlockPos(pos));
		return InteractionResult.sidedSuccess(world.isClientSide);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		return open(world, pos, ep);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level world, BlockPos pos, Player ep, InteractionHand hand, BlockHitResult hit) {
		if (held.is(this.asItem()))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		open(world, pos, ep);
		return ItemInteractionResult.sidedSuccess(world.isClientSide);
	}
}
