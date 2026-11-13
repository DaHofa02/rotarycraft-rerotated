/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.rotarycraft.registry.RotaryBlocks;

/**
 * Original: Reika.RotaryCraft.Blocks.BlockLightBridge - the walkable hard-light plate a light bridge lays: a thin slab from 1/16 to
 * 0.15175 high, light 7, unbreakable, no drops, translucent; its metadata (here {@code dir}) picks the animated top texture
 * (bridge_1 to bridge_4) and tells the bridges apart (a light bridge only removes plates with its own direction).
 */
public class BridgeBlock extends Block {

	public static final IntegerProperty DIR = IntegerProperty.create("dir", 0, 3);
	private static final VoxelShape SHAPE = Block.box(0, 1, 0, 16, 16*0.15175, 16);

	public BridgeBlock(Properties props) {
		super(props);
		this.registerDefaultState(this.stateDefinition.any().setValue(DIR, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
		b.add(DIR);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	/** No item of its own: middle-click gives the light bridge that lays it. */
	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
		return new ItemStack(RotaryBlocks.LIGHT_BRIDGE.get());
	}

	/** Original shouldSideBeRendered: the top always, other faces not against another plate. */
	@Override
	protected boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
		return side != Direction.UP && adjacent.is(this) || super.skipRendering(state, adjacent, side);
	}
}
