/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.reactorcraft.radiation.RadiationEffects;
import reika.reactorcraft.radiation.RadiationIntensity;
import reika.rotarycraft.lib.math.RandomHelper;

/**
 * Original: Reika.ReactorCraft.Blocks.BlockThoriumFuel (a Forge 1.7.10 BlockFluidFinite of LiFBe fuel, 8 quanta per block) - spilled
 * thorium fuel from a fuel dump. It flows down into free space below (filling up the fuel there), otherwise levels out with the free or
 * lower spaces beside it (the Forge finite fluid rules); a single quantum stays put. Hardness 100, resistance 500, opaque; with 0.5 %
 * per update it contaminates its surroundings (moderate radiation, radius 2). As that Forge fluid class no longer exists it is a
 * block with a level (layers like snow) drawn with the LiFBe fuel texture.
 */
public class ThoriumFuelBlock extends Block {

	public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 7);
	public static final int QUANTA = 8;
	/** Forge BlockFluidBase tickRate: the fluid's viscosity (850*3) / 200. */
	public static final int TICK_RATE = 2550/200;

	public ThoriumFuelBlock(Properties props) {
		super(props);
		this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 7));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LEVEL);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
		return Block.box(0, 0, 0, 16, 2*(state.getValue(LEVEL)+1), 16);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
		return Shapes.empty();
	}

	@Override
	protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState old, boolean moving) {
		world.scheduleTick(pos, this, TICK_RATE);
	}

	@Override
	protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos from, boolean moving) {
		world.scheduleTick(pos, this, TICK_RATE);
	}

	/** Original canOverwrite (displaceIfPossible): soft blocks, RotaryCraft pipes and other mods' ducts/cables/pipes; never thorium fuel. */
	public static boolean canOverwrite(Level world, BlockPos pos) {
		BlockState s = world.getBlockState(pos);
		if (s.getBlock() instanceof ThoriumFuelBlock)
			return false;
		if (world.getBlockEntity(pos) instanceof reika.rotarycraft.content.piping.PipingBlockEntity)
			return true;
		if (s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty() || s.is(Blocks.VINE))
			return true;
		String n = s.getBlock().getClass().getSimpleName().toLowerCase(java.util.Locale.ENGLISH);
		return n.contains("duct") || n.contains("conduit") || n.contains("cable") || n.contains("pipe");
	}

	/** Original TileEntityFuelDump.canDumpAt. */
	public static boolean canDumpAt(Level world, BlockPos pos) {
		BlockState s = world.getBlockState(pos);
		return canOverwrite(world, pos) || (s.getBlock() instanceof ThoriumFuelBlock && s.getValue(LEVEL) < 7);
	}

	private int quanta(Level world, BlockPos pos) {
		BlockState s = world.getBlockState(pos);
		if (s.isAir())
			return 0;
		if (s.is(this))
			return s.getValue(LEVEL)+1;
		return -1;
	}

	private int quantaBelow(Level world, BlockPos pos, int belowThis) {
		int q = this.quanta(world, pos);
		return q >= belowThis ? -1 : q;
	}

	private void set(Level world, BlockPos pos, int quanta) {
		if (quanta <= 0)
			world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		else
			world.setBlock(pos, this.defaultBlockState().setValue(LEVEL, quanta-1), 3);
	}

	/** Forge BlockFluidFinite.tryToFlowVerticallyInto. */
	private int tryToFlowDown(Level world, BlockPos pos, int amt) {
		BlockPos below = pos.below();
		if (canOverwrite(world, below)) {
			world.destroyBlock(below, true);
			this.set(world, below, amt);
			world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			return 0;
		}
		int q = this.quantaBelow(world, below, QUANTA);
		if (q >= 0) {
			q += amt;
			if (q > QUANTA) {
				this.set(world, below, QUANTA);
				return q-QUANTA;
			}
			else if (q > 0) {
				this.set(world, below, q);
				world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				return 0;
			}
		}
		return amt;
	}

	@Override
	protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
		if (RandomHelper.doWithChance(0.005, rand))
			RadiationEffects.contaminateArea(world, pos.above(rand.nextInt(2)), 2, 0.25F, 0, false, RadiationIntensity.MODERATE);
		boolean changed = false;
		int quantaRemaining = state.getValue(LEVEL)+1;
		int prev = quantaRemaining;
		quantaRemaining = this.tryToFlowDown(world, pos, quantaRemaining);
		if (quantaRemaining < 1)
			return;
		else if (quantaRemaining != prev) {
			changed = true;
			if (quantaRemaining == 1) {
				this.set(world, pos, 1);
				return;
			}
		}
		else if (quantaRemaining == 1) {
			return;
		}
		int lowerthan = quantaRemaining-1;
		Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
		int[] q = new int[4];
		int total = quantaRemaining;
		int count = 1;
		for (int i = 0; i < 4; i++) {
			BlockPos p = pos.relative(dirs[i]);
			if (canOverwrite(world, p) && !world.getBlockState(p).isAir())
				world.destroyBlock(p, true);
			q[i] = this.quantaBelow(world, p, lowerthan);
			if (q[i] >= 0) {
				count++;
				total += q[i];
			}
		}
		if (count == 1) {
			if (changed)
				this.set(world, pos, quantaRemaining);
			return;
		}
		int each = total/count;
		int rem = total%count;
		for (int i = 0; i < 4; i++) {
			if (q[i] < 0)
				continue;
			int nq = each;
			if (rem == count || rem > 1 && rand.nextInt(count-rem) != 0) {
				nq++;
				rem--;
			}
			if (nq != q[i]) {
				BlockPos p = pos.relative(dirs[i]);
				this.set(world, p, nq);
				world.scheduleTick(p, this, TICK_RATE);
			}
			count--;
		}
		if (rem > 0)
			each++;
		this.set(world, pos, each);
	}

	@Override
	protected boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext ctx) {
		return true;
	}
}
