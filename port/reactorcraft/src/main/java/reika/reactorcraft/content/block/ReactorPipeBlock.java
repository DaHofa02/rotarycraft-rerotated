/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Original: Reika.ReactorCraft.Blocks.BlockDuct - the block of the ReactorCraft ducts ({@link reika.reactorcraft.content.piping.ReactorPipingBlockEntity}):
 * drawn like the RotaryCraft pipes (multipart model on the six connection properties, fluid by PipeFluidRenderer). The connection on a
 * side is recomputed when the duct is placed and whenever a neighbour changes (original recomputeConnections).
 */
public abstract class ReactorPipeBlock extends BaseEntityBlock {

	private static final VoxelShape SHAPE = Block.box(2, 2, 2, 14, 14, 14);

	protected ReactorPipeBlock(Properties props) {
		super(props);
		BlockState s = this.stateDefinition.any();
		for (BooleanProperty p : PipeBlock.PROPERTY_BY_DIRECTION.values())
			s = s.setValue(p, false);
		this.registerDefaultState(s);
	}

	/** Whether the duct at {@code pos} connects to what is on its side {@code dir} (original isConnected). */
	protected abstract boolean connectsTo(Level world, BlockPos pos, Direction dir);

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

	private BlockState withConnections(Level world, BlockPos pos, BlockState state) {
		BlockState ns = state;
		for (Direction dir : Direction.values())
			ns = ns.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), this.connectsTo(world, pos, dir));
		return ns;
	}

	@Override
	protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState old, boolean moving) {
		super.onPlace(state, world, pos, old, moving);
		if (!world.isClientSide && !old.is(this)) {
			BlockState ns = this.withConnections(world, pos, state);
			if (ns != state)
				world.setBlock(pos, ns, 3);
		}
	}

	@Override
	protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos from, boolean moving) {
		super.neighborChanged(state, world, pos, block, from, moving);
		if (!world.isClientSide) {
			BlockState ns = this.withConnections(world, pos, state);
			if (ns != state)
				world.setBlock(pos, ns, 3);
		}
	}
}
