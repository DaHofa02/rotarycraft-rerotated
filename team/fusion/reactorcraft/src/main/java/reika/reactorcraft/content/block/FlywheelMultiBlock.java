/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.block;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import reika.reactorcraft.powergen.ReactorFlywheelBlockEntity;

/**
 * Original: Reika.ReactorCraft.Blocks.Multi.BlockFlywheelMulti - the turbine flywheel's ring: flywheel core (0), vibration dampeners
 * (1) and frame (2). Placing any part collects the parts touching it (also diagonally, within 6 blocks); there must be exactly 20, the
 * middle of their bounds must be the flywheel, and in the plane across the flywheel's facing they must form a 5x5 ring without
 * corners: cores next to the flywheel, frame 2 out on the axes, dampeners on the diagonals and frame everywhere else. When it matches,
 * every part becomes "formed" (invisible, the flywheel draws itself; 4 times as hard) and the flywheel emits; breaking any part
 * un-forms it.
 */
public class FlywheelMultiBlock extends Block {

	public static final BooleanProperty FORMED = BooleanProperty.create("formed");

	/** 0 flywheel core, 1 vibration dampeners, 2 frame (original metadata). */
	public final int variant;

	public FlywheelMultiBlock(Properties props, int variant) {
		super(props);
		this.variant = variant;
		this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FORMED);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
	}

	/** Original getBlockHardness: 4 times as hard while formed. */
	@Override
	protected float getDestroyProgress(BlockState state, Player ep, BlockGetter world, BlockPos pos) {
		float f = super.getDestroyProgress(state, ep, world, pos);
		return state.getValue(FORMED) ? f/4 : f;
	}

	public static boolean isPart(BlockState s) {
		return s.getBlock() instanceof FlywheelMultiBlock;
	}

	/** Original block and metadata match: formed parts (metadata+8) do not count. */
	private static boolean is(Level world, BlockPos p, int variant) {
		BlockState s = world.getBlockState(p);
		return s.getBlock() instanceof FlywheelMultiBlock f && f.variant == variant && !s.getValue(FORMED);
	}

	/** Original canTriggerMultiBlockCheck: every part checks. */
	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);
		if (!world.isClientSide && placer instanceof Player) {
			if (checkForFullMultiBlock(world, pos))
				onCreateFullMultiBlock(world, pos);
		}
	}

	/** Original BlockMultiBlock.breakBlock: removing a part un-forms the structures beside it. */
	@Override
	protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
		if (!world.isClientSide && !state.is(newState.getBlock())) {
			for (Direction d : Direction.values())
				breakMultiBlock(world, pos.relative(d));
		}
		super.onRemove(state, world, pos, newState, moved);
	}

	/** Original BlockArray.recursiveAddWithBoundsRanged(range 1): parts touching each other, also diagonally, within 6 blocks of c. */
	private static void collect(Level world, BlockPos start, BlockPos c, Set<BlockPos> seen) {
		if (!isPart(world.getBlockState(start)) || seen.contains(start))
			return;
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		seen.add(start);
		queue.add(start);
		while (!queue.isEmpty()) {
			BlockPos p = queue.poll();
			for (BlockPos n : BlockPos.betweenClosed(p.offset(-1, -1, -1), p.offset(1, 1, 1))) {
				if (Math.abs(n.getX()-c.getX()) > 6 || Math.abs(n.getY()-c.getY()) > 6 || Math.abs(n.getZ()-c.getZ()) > 6)
					continue;
				if (!seen.contains(n) && isPart(world.getBlockState(n))) {
					BlockPos i = n.immutable();
					seen.add(i);
					queue.add(i);
				}
			}
		}
	}

	/** Original StructuredBlockArray.getMidX/Y/Z: min + size/2 of the bounds. */
	@Nullable
	private static BlockPos mid(Set<BlockPos> blocks) {
		if (blocks.isEmpty())
			return null;
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
		for (BlockPos p : blocks) {
			minX = Math.min(minX, p.getX());
			minY = Math.min(minY, p.getY());
			minZ = Math.min(minZ, p.getZ());
			maxX = Math.max(maxX, p.getX());
			maxY = Math.max(maxY, p.getY());
			maxZ = Math.max(maxZ, p.getZ());
		}
		return new BlockPos(minX+(maxX-minX+1)/2, minY+(maxY-minY+1)/2, minZ+(maxZ-minZ+1)/2);
	}

	public static boolean checkForFullMultiBlock(Level world, BlockPos pos) {
		Set<BlockPos> blocks = new HashSet<>();
		collect(world, pos, pos, blocks);
		if (blocks.size() != 20)
			return false;
		BlockPos m = mid(blocks);
		if (!(world.getBlockEntity(m) instanceof ReactorFlywheelBlockEntity te))
			return false;
		Direction left = te.getFacing().getCounterClockWise();
		for (int i = 1; i <= 2; i++) {
			int v = i == 1 ? 0 : 2;
			if (!is(world, m.relative(left, i), v) || !is(world, m.relative(left, -i), v) || !is(world, m.below(i), v) || !is(world, m.above(i), v))
				return false;
		}
		for (int s = -1; s <= 1; s += 2) {
			BlockPos c = m.relative(left, s);
			if (!is(world, c.above(), 1) || !is(world, c.below(), 1))
				return false;
			if (!is(world, c.above(2), 2) || !is(world, c.below(2), 2))
				return false;
			BlockPos o = m.relative(left, 2*s);
			if (!is(world, o.above(), 2) || !is(world, o.below(), 2))
				return false;
		}
		return true;
	}

	public static void onCreateFullMultiBlock(Level world, BlockPos pos) {
		Set<BlockPos> blocks = new HashSet<>();
		collect(world, pos, pos, blocks);
		for (BlockPos c : blocks) {
			BlockState s = world.getBlockState(c);
			if (!s.getValue(FORMED))
				world.setBlock(c, s.setValue(FORMED, true), 3);
		}
		BlockPos m = mid(blocks);
		if (m != null && world.getBlockEntity(m) instanceof ReactorFlywheelBlockEntity te)
			te.setHasMultiBlock(true);
	}

	public static void breakMultiBlock(Level world, BlockPos pos) {
		Set<BlockPos> blocks = new HashSet<>();
		collect(world, pos, pos, blocks);
		for (Direction d : Direction.values())
			collect(world, pos.relative(d), pos, blocks);
		for (BlockPos c : blocks) {
			BlockState s = world.getBlockState(c);
			if (isPart(s) && s.getValue(FORMED))
				world.setBlock(c, s.setValue(FORMED, false), 3);
		}
		BlockPos m = mid(blocks);
		if (m != null && world.getBlockEntity(m) instanceof ReactorFlywheelBlockEntity te)
			te.setHasMultiBlock(false);
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
		return List.of(new ItemStack(this));
	}
}
