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

import reika.reactorcraft.content.piping.HeatPipeBlockEntity;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/**
 * Original: Reika.ReactorCraft.Blocks.BlockSteamLine (shared with the steam line: hardness 0, resistance 1) with SteamLineRenderer - the heat
 * pipe is drawn like the steam line, a core of 1/3 block with an arm to every connected side, in the snow texture tinted with
 * TileEntityHeatPipe.getRenderColor (0xFFA54D; the hotter red never shows, see HeatPipeBlockEntity); the box is the middle half of the block.
 */
public class HeatPipeBlock extends BaseEntityBlock {

	private static final VoxelShape SHAPE = Block.box(4, 4, 4, 12, 12, 12);

	public static final MapCodec<HeatPipeBlock> CODEC = simpleCodec(HeatPipeBlock::new);

	public HeatPipeBlock(Properties props) {
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
		return new HeatPipeBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> given) {
		return createTickerHelper(given, ReactorBlockEntities.HEAT_PIPE.get(), RotaryCraftBlockEntity::tick);
	}

	private static BlockState withConnections(Level world, BlockPos pos, BlockState state) {
		BlockState ns = state;
		for (Direction dir : Direction.values())
			ns = ns.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), HeatPipeBlockEntity.connectsTo(world, pos, dir));
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
