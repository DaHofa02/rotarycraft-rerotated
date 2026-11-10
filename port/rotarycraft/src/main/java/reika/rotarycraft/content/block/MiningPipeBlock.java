/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.rotarycraft.registry.RotaryBlocks;

/**
 * Original: Reika.RotaryCraft.Blocks.BlockMiningPipe (BlockRegistry.MININGPIPE) - what the Borer leaves in the cells it cuts and the
 * Pile Driver's shaft. Material.iron, hardness 0, resistance 0, no drops, no item, not in the creative tab. Its metadata became
 * {@link #SHAPE}: 0 a bar along x, 1 along y, 2 along z (0.33-0.67 thick, texture minepipe), 3 a full block (minepipe; the Borer's first
 * slice), 4 a full block (minepipe2; the Pile Driver's shaft). Broken by a player, an x or z bar takes the pipes up to 64 blocks either way
 * along its line with it (original onBlockDestroyedByPlayer); y bars and full blocks only themselves.
 * Port addition: middle-click gives the machine that makes the pipe (the original gave nothing; every block of the port has a pick item).
 */
public class MiningPipeBlock extends Block {

	public enum Shape implements StringRepresentable {
		X, Y, Z, FULL, PILE;

		@Override
		public String getSerializedName() {
			return this.name().toLowerCase(Locale.ROOT);
		}

		/** The original metadata. */
		public static Shape fromMeta(int meta) {
			return values()[Math.max(0, Math.min(4, meta))];
		}
	}

	public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);

	private static final double LO = 0.33*16, HI = 0.67*16;
	private static final VoxelShape BAR_X = Block.box(0, LO, LO, 16, HI, HI);
	private static final VoxelShape BAR_Y = Block.box(LO, 0, LO, HI, 16, HI);
	private static final VoxelShape BAR_Z = Block.box(LO, LO, 0, HI, HI, 16);

	public MiningPipeBlock(Properties props) {
		super(props);
		this.registerDefaultState(this.stateDefinition.any().setValue(SHAPE, Shape.FULL));
	}

	public static BlockState state(int meta) {
		return RotaryBlocks.MINING_PIPE.get().defaultBlockState().setValue(SHAPE, Shape.fromMeta(meta));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
		b.add(SHAPE);
	}

	/** Original setBlockBoundsBasedOnState. */
	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return switch (state.getValue(SHAPE)) {
			case X -> BAR_X;
			case Y -> BAR_Y;
			case Z -> BAR_Z;
			default -> Shapes.block();
		};
	}

	/** Original getDirectionFromMeta: only the x and z bars have a direction. */
	private static Direction getDirection(BlockState state) {
		return switch (state.getValue(SHAPE)) {
			case X -> Direction.EAST;
			case Z -> Direction.SOUTH;
			default -> null;
		};
	}

	/** Original onBlockDestroyedByPlayer: removes the pipes on the line up to 64 blocks either way (break sound at each). */
	@Override
	public void destroy(net.minecraft.world.level.LevelAccessor world, BlockPos pos, BlockState state) {
		super.destroy(world, pos, state);
		Direction dir = getDirection(state);
		if (dir == null || world.isClientSide())
			return;
		for (int k = -64; k <= 64; k++) {
			BlockPos d = pos.relative(dir, k);
			BlockState at = world.getBlockState(d);
			if (at.is(this)) {
				world.playSound(null, d, at.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1, 0.8F);
				world.setBlock(d, Blocks.AIR.defaultBlockState(), 3);
			}
		}
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
		return new ItemStack(RotaryBlocks.BORER.get());
	}

	@Override
	public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
		return true;
	}
}
