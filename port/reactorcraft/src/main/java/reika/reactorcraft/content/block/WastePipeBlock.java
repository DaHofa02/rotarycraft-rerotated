/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.waste.WastePipeBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/** Original: Reika.ReactorCraft.Blocks.BlockDuct - the waste pipe: drawn like the RotaryCraft pipes (concrete frame, leaf windows). */
public class WastePipeBlock extends BaseEntityBlock {

	private static final VoxelShape SHAPE = Block.box(2, 2, 2, 14, 14, 14);
	public static final MapCodec<WastePipeBlock> CODEC = simpleCodec(WastePipeBlock::new);

	public WastePipeBlock(Properties props) {
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
		return SHAPE;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WastePipeBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> given) {
		return createTickerHelper(given, ReactorBlockEntities.WASTE_PIPE.get(), RotaryCraftBlockEntity::tick);
	}

	private static BlockState withConnections(Level world, BlockPos pos, BlockState state) {
		BlockState ns = state;
		for (Direction dir : Direction.values())
			ns = ns.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), WastePipeBlockEntity.connectsTo(world.getBlockEntity(pos.relative(dir))));
		return ns;
	}

	@Override
	protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState old, boolean moving) {
		super.onPlace(state, world, pos, old, moving);
		if (!world.isClientSide && !old.is(this)) {
			BlockState ns = withConnections(world, pos, state);
			if (ns != state)
				world.setBlock(pos, ns, 3);
		}
	}

	@Override
	protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos from, boolean moving) {
		super.neighborChanged(state, world, pos, block, from, moving);
		if (!world.isClientSide) {
			BlockState ns = withConnections(world, pos, state);
			if (ns != state)
				world.setBlock(pos, ns, 3);
		}
	}
}
