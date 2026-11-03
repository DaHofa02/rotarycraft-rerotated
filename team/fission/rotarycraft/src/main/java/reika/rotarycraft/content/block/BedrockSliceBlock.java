/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Original: Reika.RotaryCraft.Blocks.BlockBedrockSlice - bedrock the Bedrock Breaker is grinding: unbreakable, no drops, not in creative,
 * cut away by level/16 from the side facing the machine (original TileEntityBedrockSlice bounds; the metadata is the level). The
 * original "dustYield" field of the slice's tile entity is only changed by other mods and stays at its default of 1 here.
 */
public class BedrockSliceBlock extends Block {

	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 15);

	private static final MapCodec<BedrockSliceBlock> CODEC = simpleCodec(BedrockSliceBlock::new);

	public BedrockSliceBlock(Properties props) {
		super(props);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.EAST).setValue(LEVEL, 0));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
		b.add(FACING, LEVEL);
	}

	/** Original BlockBounds.block().cut(machineDirection, level/16). */
	public static VoxelShape shapeFor(Direction side, int level) {
		double a = level;
		return switch(side) {
			case DOWN -> Block.box(0, a, 0, 16, 16, 16);
			case UP -> Block.box(0, 0, 0, 16, 16-a, 16);
			case EAST -> Block.box(0, 0, 0, 16-a, 16, 16);
			case WEST -> Block.box(a, 0, 0, 16, 16, 16);
			case NORTH -> Block.box(0, 0, a, 16, 16, 16);
			case SOUTH -> Block.box(0, 0, 0, 16, 16, 16-a);
		};
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return shapeFor(state.getValue(FACING), state.getValue(LEVEL));
	}

	/**
	 * Middle-click gives bedrock (the slice is bedrock being cut). Deviation: in the original the slice had no item and middle-click gave
	 * nothing.
	 */
	@Override
	public net.minecraft.world.item.ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos, BlockState state) {
		return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BEDROCK);
	}
}
