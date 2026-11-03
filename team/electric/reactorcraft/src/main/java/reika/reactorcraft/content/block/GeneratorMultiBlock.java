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

import reika.reactorcraft.powergen.ReactorGeneratorBlockEntity;

/**
 * Original: Reika.ReactorCraft.Blocks.Multi.BlockGeneratorMulti - the reactor generator's multiblock: rotor core (0), rotor windings (1),
 * housing (2) and steam bypass (3). Placing a rotor core checks the structure from that block along the direction the player looks:
 * 9 cores in a row ending at the generator (which must face back along the row); windings above, below and beside every core (the
 * first two rings of steam bypasses instead); housing 2 above/below and diagonally, and 2 to each side; the generator end cap is a
 * 5x5 ring of housing without corners; the bypass end has housing at 2 to the side, 2 up/down for its first two rings, with a bypass
 * on top of the second ring. When it matches, every connected part becomes "formed" (invisible, the generator draws the machine; 4
 * times as hard) and the generator runs; breaking any part un-forms the whole connected structure. Steam arriving at a steam bypass
 * from a turbine is carried over the generator (see SteamBlock).
 */
public class GeneratorMultiBlock extends Block {

	public static final BooleanProperty FORMED = BooleanProperty.create("formed");

	/** 0 rotor core, 1 rotor windings, 2 housing, 3 steam bypass (original metadata). */
	public final int variant;

	public GeneratorMultiBlock(Properties props, int variant) {
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
		return s.getBlock() instanceof GeneratorMultiBlock;
	}

	/** Original block and metadata match: formed parts (metadata+8) do not count. */
	private static boolean is(Level world, BlockPos p, int variant) {
		BlockState s = world.getBlockState(p);
		return s.getBlock() instanceof GeneratorMultiBlock g && g.variant == variant && !s.getValue(FORMED);
	}

	private static boolean isPartOrGenerator(Level world, BlockPos p) {
		return isPart(world.getBlockState(p)) || world.getBlockEntity(p) instanceof ReactorGeneratorBlockEntity;
	}

	/** Original canTriggerMultiBlockCheck: only placing a rotor core checks. */
	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);
		if (!world.isClientSide && variant == 0 && placer instanceof Player) {
			Direction dir = placer.getDirection();
			if (checkForFullMultiBlock(world, pos, dir))
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

	public static boolean checkForFullMultiBlock(Level world, BlockPos pos, Direction dir) {
		if (!checkCore(world, pos, dir) || !checkWindings(world, pos, dir) || !checkHousing(world, pos, dir) || !checkEndCap(world, pos, dir))
			return false;
		int l = ReactorGeneratorBlockEntity.GENERATOR_LENGTH-1;
		return world.getBlockEntity(pos.relative(dir, l)) instanceof ReactorGeneratorBlockEntity;
	}

	private static boolean checkCore(Level world, BlockPos pos, Direction dir) {
		int l = ReactorGeneratorBlockEntity.GENERATOR_LENGTH-1;
		for (int i = 0; i < l; i++) {
			if (!is(world, pos.relative(dir, i), 0))
				return false;
		}
		return world.getBlockEntity(pos.relative(dir, l)) instanceof ReactorGeneratorBlockEntity te && dir == te.getFacing().getOpposite();
	}

	private static boolean checkWindings(Level world, BlockPos pos, Direction dir) {
		int l = ReactorGeneratorBlockEntity.GENERATOR_LENGTH-1;
		Direction left = dir.getCounterClockWise();
		for (int i = 0; i < l; i++) {
			int seekmeta = i < 2 ? 3 : 1;
			BlockPos d = pos.relative(dir, i);
			for (int k = -1; k <= 1; k++) {
				BlockPos c = d.above(k);
				if (!is(world, c.relative(left), seekmeta) || !is(world, c.relative(left, -1), seekmeta))
					return false;
				if (k != 0 && !is(world, c, seekmeta))
					return false;
			}
		}
		return true;
	}

	private static boolean checkHousing(Level world, BlockPos pos, Direction dir) {
		int l = ReactorGeneratorBlockEntity.GENERATOR_LENGTH-1;
		Direction left = dir.getCounterClockWise();
		for (int i = 0; i < l; i++) {
			BlockPos d = pos.relative(dir, i);
			int seekmeta = 2;
			for (int k = -2; k <= 2; k += 4) {
				BlockPos c = d.above(k);
				if (i == 1 && k == 2)
					seekmeta = 3;
				if (!is(world, c.relative(left), 2) || !is(world, c.relative(left, -1), 2))
					return false;
				if (!is(world, c, seekmeta))
					return false;
			}
			for (int k = -1; k <= 1; k++) {
				BlockPos c = d.above(k);
				if (!is(world, c.relative(left, 2), 2) || !is(world, c.relative(left, -2), 2))
					return false;
			}
		}
		return true;
	}

	private static boolean checkEndCap(Level world, BlockPos pos, Direction dir) {
		int l = ReactorGeneratorBlockEntity.GENERATOR_LENGTH-1;
		Direction left = dir.getCounterClockWise();
		BlockPos d = pos.relative(dir, l);
		for (int k = -2; k <= 2; k++) {
			for (int m = -2; m <= 2; m++) {
				if ((Math.abs(k) != 2 || Math.abs(m) != 2) && (k != 0 || m != 0)) {
					if (!is(world, d.above(k).relative(left, m), 2))
						return false;
				}
			}
		}
		for (int i = 0; i < 2; i++) {
			BlockPos c = pos.relative(dir, i);
			for (int k = 2; k >= -2; k -= 4) {
				if (!is(world, c.above(k).relative(left, 2), 2) || !is(world, c.above(k).relative(left, -2), 2))
					return false;
			}
		}
		return true;
	}

	/** Original BlockArray.recursiveAddMultipleWithBounds: connected parts and generators, within 12 blocks (4 vertically). */
	private static Set<BlockPos> connected(Level world, BlockPos start) {
		Set<BlockPos> seen = new HashSet<>();
		if (!isPartOrGenerator(world, start))
			return seen;
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		seen.add(start);
		queue.add(start);
		while (!queue.isEmpty()) {
			BlockPos p = queue.poll();
			for (Direction d : Direction.values()) {
				BlockPos n = p.relative(d);
				if (Math.abs(n.getX()-start.getX()) > 12 || Math.abs(n.getY()-start.getY()) > 4 || Math.abs(n.getZ()-start.getZ()) > 12)
					continue;
				if (!seen.contains(n) && isPartOrGenerator(world, n)) {
					seen.add(n);
					queue.add(n);
				}
			}
		}
		return seen;
	}

	public static void onCreateFullMultiBlock(Level world, BlockPos pos) {
		for (BlockPos c : connected(world, pos)) {
			BlockState s = world.getBlockState(c);
			if (world.getBlockEntity(c) instanceof ReactorGeneratorBlockEntity te)
				te.setHasMultiBlock(true);
			else if (isPart(s) && !s.getValue(FORMED))
				world.setBlock(c, s.setValue(FORMED, true), 3);
		}
	}

	public static void breakMultiBlock(Level world, BlockPos pos) {
		for (BlockPos c : connected(world, pos)) {
			BlockState s = world.getBlockState(c);
			if (world.getBlockEntity(c) instanceof ReactorGeneratorBlockEntity te)
				te.setHasMultiBlock(false);
			else if (isPart(s) && s.getValue(FORMED)) //the generator may just have blown up
				world.setBlock(c, s.setValue(FORMED, false), 3);
		}
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
		return List.of(new ItemStack(this));
	}
}
