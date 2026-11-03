/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.fusion;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import reika.rotarycraft.content.piping.PipingBlockEntity;

/**
 * Original: Reika.ReactorCraft.Blocks.Multi.BlockSolenoidMulti, BlockInjectorMulti and BlockHeaterMulti (BlockReCMultiBlock: hardness 2,
 * 4 times as hard while formed) - one block per former metadata value; "formed" stands for metadata+8, which no longer counts for the
 * structure checks. The original's connected textures (which depend on the neighbours) are not ported: each part has its plain
 * texture, formed solenoid parts the original's formed texture.
 * <ul>
 * <li>Solenoid (0-5): placing any part checks the connected parts (diagonals count, 20 blocks around, 3 up/down): the solenoid magnet
 * must be in the middle of them, with the core (5) in the 3x3 around and above it, spokes (4) out to 7 blocks (diagonals to 5), the
 * ring at 7-8 blocks with coils (2, 3: middle layer) between rails (0, 1: above and below) and rails (1) at the corners.</li>
 * <li>Injector (0-7): placing a column part (5) next to a filler (7) checks the 9 long injector frame along that way (bottom 0/1, sides
 * 2/6, top 3/4, fillers, the injector 2 blocks along, air beside it, magnetic pipes 3-8 along).</li>
 * <li>Heater (0-4): placing any part checks the 5x5x5 box with its cap layer: corners 2, edges 3, faces 4, top 1, cap 2/3, a core of
 * at least 22 insulation parts (0 lens, exactly one; 1 insulation) around the heater, the rest RotaryCraft pipes, and magnetic pipes
 * from the heater up through the cap.</li>
 * </ul>
 */
public class FusionMultiBlock extends Block {

	public static final BooleanProperty FORMED = BooleanProperty.create("formed");

	public enum Kind {
		SOLENOID, INJECTOR, HEATER
	}

	public final Kind kind;
	public final int variant;

	public FusionMultiBlock(Properties props, Kind kind, int variant) {
		super(props);
		this.kind = kind;
		this.variant = variant;
		this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FORMED);
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player ep, BlockGetter world, BlockPos pos) {
		float f = super.getDestroyProgress(state, ep, world, pos);
		return state.getValue(FORMED) ? f/4 : f;
	}

	/** The original block metadata of this kind of part there (variant, +8 when formed), or -1. */
	private static int meta(BlockGetter world, BlockPos p, Kind k) {
		BlockState s = world.getBlockState(p);
		if (s.getBlock() instanceof FusionMultiBlock b && b.kind == k)
			return b.variant+(s.getValue(FORMED) ? 8 : 0);
		return -1;
	}

	private static boolean isKind(BlockGetter world, BlockPos p, Kind k) {
		return meta(world, p, k) >= 0;
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);
		if (world.isClientSide || !(placer instanceof Player))
			return;
		switch (kind) {
			case SOLENOID -> {
				if (checkSolenoid(world, pos))
					formSolenoid(world, pos);
			}
			case INJECTOR -> {
				if (variant == 5 && checkInjector(world, pos))
					formInjector(world, pos);
			}
			case HEATER -> {
				if (checkHeater(world, pos))
					formHeater(world, pos);
			}
		}
	}

	/** Original BlockMultiBlock.breakBlock: removing a part un-forms the structures beside it. */
	@Override
	protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
		if (!world.isClientSide && !state.is(newState.getBlock()))
			breakAround(world, pos);
		super.onRemove(state, world, pos, newState, moved);
	}

	/** Breaking a part or a machine of a structure: every multiblock beside it un-forms. */
	public static void breakAround(Level world, BlockPos pos) {
		for (Direction d : Direction.values()) {
			BlockPos p = pos.relative(d);
			if (world.getBlockState(p).getBlock() instanceof FusionMultiBlock b) {
				switch (b.kind) {
					case SOLENOID -> breakSolenoid(world, p);
					case INJECTOR -> breakInjector(world, p);
					case HEATER -> breakHeater(world, p);
				}
			}
		}
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
		return List.of(new ItemStack(this));
	}

	/** DragonAPI BlockArray.recursiveAddWithBounds(Ranged): connected blocks matching the test within the bounds (ranged: 26 neighbours). */
	private static Set<BlockPos> connected(Level world, BlockPos start, Predicate<BlockPos> test, int rxz, int ry, boolean ranged) {
		Set<BlockPos> seen = new HashSet<>();
		if (!test.test(start))
			return seen;
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		seen.add(start);
		queue.add(start);
		while (!queue.isEmpty()) {
			BlockPos p = queue.poll();
			for (int i = -1; i <= 1; i++) {
				for (int j = -1; j <= 1; j++) {
					for (int k = -1; k <= 1; k++) {
						if (!ranged && Math.abs(i)+Math.abs(j)+Math.abs(k) != 1)
							continue;
						BlockPos n = p.offset(i, j, k);
						if (Math.abs(n.getX()-start.getX()) > rxz || Math.abs(n.getY()-start.getY()) > ry || Math.abs(n.getZ()-start.getZ()) > rxz)
							continue;
						if (!seen.contains(n) && test.test(n)) {
							seen.add(n);
							queue.add(n);
						}
					}
				}
			}
		}
		return seen;
	}

	/** BlockArray min corner and getMinX()+getSizeX()/2 style middle. */
	private static BlockPos min(Set<BlockPos> set) {
		int x = Integer.MAX_VALUE, y = Integer.MAX_VALUE, z = Integer.MAX_VALUE;
		for (BlockPos p : set) {
			x = Math.min(x, p.getX());
			y = Math.min(y, p.getY());
			z = Math.min(z, p.getZ());
		}
		return new BlockPos(x, y, z);
	}

	private static BlockPos mid(Set<BlockPos> set) {
		BlockPos lo = min(set);
		int x = Integer.MIN_VALUE, y = Integer.MIN_VALUE, z = Integer.MIN_VALUE;
		for (BlockPos p : set) {
			x = Math.max(x, p.getX());
			y = Math.max(y, p.getY());
			z = Math.max(z, p.getZ());
		}
		return new BlockPos(lo.getX()+(x-lo.getX()+1)/2, lo.getY()+(y-lo.getY()+1)/2, lo.getZ()+(z-lo.getZ()+1)/2);
	}

	private static void setFormed(Level world, BlockPos p, boolean formed) {
		BlockState s = world.getBlockState(p);
		if (s.getBlock() instanceof FusionMultiBlock && s.getValue(FORMED) != formed)
			world.setBlock(p, s.setValue(FORMED, formed), 3);
	}

	//---------------------------------------------------------------- solenoid

	private static Set<BlockPos> solenoidParts(Level world, BlockPos pos) {
		return connected(world, pos, p -> isKind(world, p, Kind.SOLENOID), 20, 3, true);
	}

	public static boolean checkSolenoid(Level world, BlockPos pos) {
		Set<BlockPos> blocks = solenoidParts(world, pos);
		if (blocks.isEmpty())
			return false;
		BlockPos m = mid(blocks);
		if (!(world.getBlockEntity(m) instanceof SolenoidMagnetBlockEntity))
			return false;
		Kind K = Kind.SOLENOID;
		for (int y : new int[]{1, -1}) { //upper and lower rails
			for (int i = -5; i <= 5; i++) {
				int d = Math.abs(i) >= 4 ? 7 : 8;
				int mm = Math.abs(i) >= 3 ? 1 : 0;
				if (meta(world, m.offset(-d, y, i), K) != mm || meta(world, m.offset(d, y, i), K) != mm
						|| meta(world, m.offset(i, y, d), K) != mm || meta(world, m.offset(i, y, -d), K) != mm)
					return false;
			}
		}
		for (int i = -5; i <= 5; i++) { //coils
			int d = Math.abs(i) >= 4 ? 7 : 8;
			int mm = Math.abs(i) >= 3 ? 3 : 2;
			if (meta(world, m.offset(-d, 0, i), K) != mm || meta(world, m.offset(d, 0, i), K) != mm
					|| meta(world, m.offset(i, 0, d), K) != mm || meta(world, m.offset(i, 0, -d), K) != mm)
				return false;
		}
		if (meta(world, m.offset(-6, 1, -6), K) != 1 || meta(world, m.offset(-6, -1, -6), K) != 1) //original checkCorners: only this corner
			return false;
		for (int i = 2; i <= 7; i++) { //spokes
			if (meta(world, m.offset(i, 0, 0), K) != 4 || meta(world, m.offset(-i, 0, 0), K) != 4 || meta(world, m.offset(0, 0, i), K) != 4
					|| meta(world, m.offset(0, 0, -i), K) != 4)
				return false;
			if (i < 6 && (meta(world, m.offset(i, 0, i), K) != 4 || meta(world, m.offset(-i, 0, i), K) != 4 || meta(world, m.offset(i, 0, -i), K) != 4
					|| meta(world, m.offset(-i, 0, -i), K) != 4))
				return false;
		}
		for (int i = -1; i <= 1; i++) //core
			for (int j = 0; j <= 1; j++)
				for (int k = -1; k <= 1; k++)
					if ((i != 0 || j != 0 || k != 0) && meta(world, m.offset(i, j, k), K) != 5)
						return false;
		return true;
	}

	public static void formSolenoid(Level world, BlockPos pos) {
		Set<BlockPos> blocks = solenoidParts(world, pos);
		for (BlockPos p : blocks)
			setFormed(world, p, true);
		if (!blocks.isEmpty() && world.getBlockEntity(mid(blocks)) instanceof SolenoidMagnetBlockEntity te)
			te.setHasMultiBlock(true);
	}

	public static void breakSolenoid(Level world, BlockPos pos) {
		Set<BlockPos> blocks = new HashSet<>(solenoidParts(world, pos));
		for (Direction d : Direction.values())
			blocks.addAll(connected(world, pos.relative(d), p -> isKind(world, p, Kind.SOLENOID), 20, 3, true));
		if (blocks.isEmpty())
			return;
		for (BlockPos p : blocks)
			setFormed(world, p, false);
		if (world.getBlockEntity(mid(blocks)) instanceof SolenoidMagnetBlockEntity te)
			te.setHasMultiBlock(false);
	}

	//---------------------------------------------------------------- injector

	private static boolean at(Level world, BlockPos p, int m) {
		return meta(world, p, Kind.INJECTOR) == m;
	}

	public static boolean checkInjector(Level world, BlockPos pos) {
		Direction dir = null;
		for (Direction d : Direction.values()) {
			if (at(world, pos.relative(d), 7)) {
				dir = d;
				break;
			}
		}
		if (dir == null)
			return false;
		Direction left = dir.getAxis() == Direction.Axis.Y ? Direction.NORTH : dir.getCounterClockWise();
		BlockPos o = pos;
		while (at(world, o.below(), 5))
			o = o.below();
		final BlockPos x = o;
		final Direction f = dir;
		java.util.function.BiFunction<Integer, Integer, BlockPos> P = (i, y) -> x.relative(f, i).above(y);
		for (int i = 0; i <= 4; i++)
			if (!at(world, P.apply(i, 3), 3)) return false;
		for (int i = 5; i <= 6; i++)
			if (!at(world, P.apply(i, 2), 3)) return false;
		for (int i = 7; i <= 8; i++)
			if (!at(world, P.apply(i, 1), 3)) return false;
		for (int i = 0; i <= 8; i++)
			if (!at(world, P.apply(i, -1), 0)) return false;
		//sides
		for (int s = -1; s <= 1; s += 2) {
			Direction l = s > 0 ? left : left.getOpposite();
			if (!at(world, P.apply(1, 0).relative(l), 2)) return false;
			for (int i = 3; i <= 7; i++)
				if (!at(world, P.apply(i, 0).relative(l), 2)) return false;
			for (int i = 1; i <= 6; i++)
				if (!at(world, P.apply(i, 1).relative(l), 2)) return false;
			for (int i = 1; i <= 4; i++)
				if (!at(world, P.apply(i, 2).relative(l), 2)) return false;
		}
		for (int i = 0; i <= 2; i++)
			if (!at(world, x.above(i), 5)) return false;
		if (!world.getBlockState(P.apply(2, 0).relative(left)).isAir() || !world.getBlockState(P.apply(2, 0).relative(left.getOpposite())).isAir())
			return false;
		//corners
		for (int s = -1; s <= 1; s += 2) {
			Direction l = s > 0 ? left : left.getOpposite();
			for (int i = 0; i <= 4; i++)
				if (!at(world, P.apply(i, 3).relative(l), 4)) return false;
			for (int i = 5; i <= 6; i++)
				if (!at(world, P.apply(i, 2).relative(l), 4)) return false;
			for (int i = 7; i <= 8; i++)
				if (!at(world, P.apply(i, 1).relative(l), 4)) return false;
			for (int i = 0; i <= 8; i++)
				if (!at(world, P.apply(i, -1).relative(l), 1)) return false;
			for (int k = 0; k <= 2; k++)
				if (!at(world, x.above(k).relative(l), 6)) return false;
			if (!at(world, P.apply(8, 0).relative(l), 6)) return false;
		}
		//filler
		if (!at(world, P.apply(1, 0), 7)) return false;
		for (int i = 1; i <= 6; i++)
			if (!at(world, P.apply(i, 1), 7)) return false;
		for (int i = 1; i <= 4; i++)
			if (!at(world, P.apply(i, 2), 7)) return false;
		//pipes
		for (int i = 3; i <= 8; i++)
			if (!(world.getBlockEntity(P.apply(i, 0)) instanceof MagneticPipeBlockEntity)) return false;
		return world.getBlockEntity(P.apply(2, 0)) instanceof FusionInjectorBlockEntity;
	}

	private static Set<BlockPos> injectorParts(Level world, BlockPos pos) {
		return connected(world, pos, p -> isKind(world, p, Kind.INJECTOR), 8, 5, false);
	}

	public static void formInjector(Level world, BlockPos pos) {
		for (BlockPos c : injectorParts(world, pos)) {
			int m = meta(world, c, Kind.INJECTOR);
			setFormed(world, c, true);
			if (m == 0 && world.getBlockEntity(c.above()) instanceof FusionInjectorBlockEntity te)
				te.setHasMultiBlock(true);
		}
	}

	public static void breakInjector(Level world, BlockPos pos) {
		for (BlockPos c : injectorParts(world, pos)) {
			int m = meta(world, c, Kind.INJECTOR);
			setFormed(world, c, false);
			if (m == 8 && world.getBlockEntity(c.above()) instanceof FusionInjectorBlockEntity te)
				te.setHasMultiBlock(false);
		}
	}

	//---------------------------------------------------------------- heater

	public static boolean checkHeater(Level world, BlockPos pos) {
		Set<BlockPos> blocks = connected(world, pos, p -> isKind(world, p, Kind.HEATER) || world.getBlockEntity(p) instanceof FusionHeaterBlockEntity, 6, 6, false);
		if (blocks.isEmpty())
			return false;
		BlockPos lo = min(blocks);
		java.util.function.Function<int[], Integer> key = c -> { //getBlockKeyRelativeToMinXYZ: -2 = not in the array, -3 = the heater
			BlockPos p = lo.offset(c[0], c[1], c[2]);
			if (!blocks.contains(p))
				return -2;
			if (world.getBlockEntity(p) instanceof FusionHeaterBlockEntity)
				return -3;
			return meta(world, p, Kind.HEATER);
		};
		//corners
		for (int a : new int[]{0, 4})
			for (int b : new int[]{0, 4})
				for (int c : new int[]{0, 4})
					if (key.apply(new int[]{a, b, c}) != 2)
						return false;
		//edges
		for (int i = 1; i < 4; i++) {
			int[][] e = {{i, 0, 0}, {0, i, 0}, {0, 0, i}, {i, 0, 4}, {4, 0, i}, {i, 4, 4}, {4, 4, i}, {i, 4, 0}, {0, 4, i}, {4, i, 0}, {0, i, 4}, {4, i, 4}};
			for (int[] c : e)
				if (key.apply(c) != 3)
					return false;
		}
		//core
		int total = 0, lens = 0;
		for (int i = 1; i < 4; i++) {
			for (int j = 1; j < 4; j++) {
				for (int k = 1; k < 4; k++) {
					BlockPos p = lo.offset(i, j, k);
					if (i == 2 && k == 2 && j == 3) {
						if (!(world.getBlockEntity(p) instanceof MagneticPipeBlockEntity))
							return false;
						continue;
					}
					int b = key.apply(new int[]{i, j, k});
					if (b == -2) {
						if (!(world.getBlockEntity(p) instanceof PipingBlockEntity pp && pp.getPipeType().isStandardPipe()))
							return false;
					}
					else if (i == 2 && j == 2 && k == 2) {
						if (b != -3)
							return false;
					}
					else if (b >= 0) {
						if (b > 1)
							return false;
						total++;
						if (b == 0)
							lens++;
					}
					else {
						return false;
					}
				}
			}
		}
		if (total < 22 || lens != 1)
			return false;
		//faces
		for (int i = 1; i < 4; i++) {
			for (int k = 1; k < 4; k++) {
				if (i == 2 && k == 2) {
					if (!(world.getBlockEntity(lo.offset(i, 4, k)) instanceof MagneticPipeBlockEntity))
						return false;
					continue;
				}
				int cap = (i == 2 || k == 2) ? 3 : 2;
				if (key.apply(new int[]{i, 0, k}) != 4 || key.apply(new int[]{i, 4, k}) != 1 || key.apply(new int[]{i, 5, k}) != cap
						|| key.apply(new int[]{i, k, 0}) != 4 || key.apply(new int[]{i, k, 4}) != 4 || key.apply(new int[]{0, k, i}) != 4
						|| key.apply(new int[]{4, k, i}) != 4)
					return false;
			}
		}
		//pipe
		for (int i = 3; i <= 6; i++)
			if (!(world.getBlockEntity(lo.offset(2, i, 2)) instanceof MagneticPipeBlockEntity))
				return false;
		return true;
	}

	private static Set<BlockPos> heaterParts(Level world, BlockPos pos) {
		return connected(world, pos, p -> isKind(world, p, Kind.HEATER), 6, 6, false);
	}

	public static void formHeater(Level world, BlockPos pos) {
		for (BlockPos c : heaterParts(world, pos)) {
			int m = meta(world, c, Kind.HEATER);
			setFormed(world, c, true);
			if (m == 0)
				for (Direction d : Direction.Plane.HORIZONTAL)
					if (world.getBlockEntity(c.relative(d)) instanceof FusionHeaterBlockEntity te)
						te.setHasMultiBlock(true);
		}
	}

	public static void breakHeater(Level world, BlockPos pos) {
		for (BlockPos c : heaterParts(world, pos)) {
			int m = meta(world, c, Kind.HEATER);
			setFormed(world, c, false);
			if (m == 8)
				for (Direction d : Direction.Plane.HORIZONTAL)
					if (world.getBlockEntity(c.relative(d)) instanceof FusionHeaterBlockEntity te)
						te.setHasMultiBlock(false);
		}
	}
}
