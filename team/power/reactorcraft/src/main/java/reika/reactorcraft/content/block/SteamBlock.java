/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
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

import reika.reactorcraft.powergen.TurbineCoreBlockEntity;
import reika.rotarycraft.lib.math.RandomHelper;
import reika.rotarycraft.registry.RotaryDamage;

/**
 * Original: Reika.ReactorCraft.Blocks.BlockSteam - a rising steam block (moves every 2 ticks). Flags (original metadata): 1 = does not
 * decay when rising, 2 = can drive a turbine, 4 = ammonia, 8 = has moved sideways. Rising it decays with 20 % unless flag 1; blocked
 * above it moves to a random free side. Under a steam turbine it is carried along the turbine to behind its last stage. It scalds
 * entities (RotaryCraft heat damage 1; ammonia also poisons for 10 s). A steam bypass of the reactor generator in the
 * way carries it over the generator, the turbine flywheel's ring scatters it sideways. The air scrubber branch of the original arrives
 * with that block (until then steam reaching it disappears, as in the original's fallback).
 */
public class SteamBlock extends Block {

	public static final IntegerProperty FLAGS = IntegerProperty.create("flags", 0, 15);
	public static final int TICK_RATE = 2;

	public SteamBlock(Properties props) {
		super(props);
		this.registerDefaultState(this.stateDefinition.any().setValue(FLAGS, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FLAGS);
	}

	@Override
	protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState old, boolean moving) {
		world.scheduleTick(pos, this, TICK_RATE);
	}

	@Override
	protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
		this.update(state, world, pos, rand);
	}

	/** The original block ticked randomly too (setTickRandomly), running the same update. */
	@Override
	protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
		this.update(state, world, pos, rand);
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return true;
	}

	private void update(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
		if (pos.getY() > 256) {
			world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
			return;
		}
		this.defaultMovement(world, pos, rand, state.getValue(FLAGS));
		world.scheduleTick(pos, this, TICK_RATE);
	}

	private void defaultMovement(ServerLevel world, BlockPos pos, RandomSource rand, int meta) {
		if (world.getBlockEntity(pos.above()) instanceof TurbineCoreBlockEntity te) {
			Direction dir = te.getSteamMovement();
			int d = te.getNumberStagesTotal()-te.getStage();
			BlockPos to = pos.offset(dir.getStepX()*d, dir.getStepY(), dir.getStepZ()*d);
			if (canMoveInto(world, to))
				world.setBlock(to, this.withFlags(this.getTransmittedMetadata(meta, dir)), 2);
			else if (world.getBlockState(to).getBlock() instanceof GeneratorMultiBlock g && g.variant == 3) {
				//original: a steam bypass of the generator carries it over the generator, 3 above the turbine
				BlockPos over = te.getBlockPos().offset(dir.getStepX()*(1+d), 3, dir.getStepZ()*(1+d));
				if (canMoveInto(world, over))
					world.setBlock(over, this.withFlags(this.getTransmittedMetadata(meta, dir)), 2);
			}
			else if (world.getBlockState(to).getBlock() instanceof FlywheelMultiBlock) {
				//original: the flywheel ring scatters it sideways (up to 1 along the turbine axis, 4 across it); ammonia retries up to 20
				//times, but tests the unscattered spot each time as in the original
				int rx = 1+3*Math.abs(dir.getStepZ()), rz = 1+3*Math.abs(dir.getStepX());
				BlockPos spot = to.offset(-rx+rand.nextInt(rx*2+1), 0, -rz+rand.nextInt(rz*2+1));
				if ((meta&4) != 0) {
					int i = 0;
					while (i <= 20 && !canMoveInto(world, to)) {
						spot = to.offset(-rx+rand.nextInt(rx*2+1), 0, -rz+rand.nextInt(rz*2+1));
						i++;
					}
				}
				if (canMoveInto(world, spot))
					world.setBlock(spot, this.withFlags(this.getTransmittedMetadata(meta, dir)), 2);
			}
			world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
			return;
		}
		if (canMoveInto(world, pos.above())) {
			if ((meta&1) != 0 || RandomHelper.doWithChance(80, rand))
				world.setBlock(pos.above(), this.withFlags(this.getTransmittedMetadata(meta, Direction.UP)), 2);
			world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
			return;
		}
		Direction[] dirs = {Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};
		for (int i = dirs.length-1; i > 0; i--) {
			int k = rand.nextInt(i+1);
			Direction t = dirs[i];
			dirs[i] = dirs[k];
			dirs[k] = t;
		}
		for (Direction d : dirs) {
			BlockPos to = pos.relative(d);
			if (canMoveInto(world, to)) {
				world.setBlock(to, this.withFlags(this.getTransmittedMetadata(meta, d)), 2);
				world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
				return;
			}
		}
	}

	private BlockState withFlags(int flags) {
		return this.defaultBlockState().setValue(FLAGS, flags);
	}

	public int getTransmittedMetadata(int meta, Direction dir) {
		if (dir == Direction.UP)
			return (meta&8) == 0 ? meta : 1+(meta&4);
		return meta|8;
	}

	/** Original canMoveInto: air or soft (replaceable) blocks, not steam and not liquids. */
	public static boolean canMoveInto(Level world, BlockPos pos) {
		BlockState s = world.getBlockState(pos);
		if (s.isAir())
			return true;
		if (s.getBlock() instanceof SteamBlock)
			return false;
		if (!s.getFluidState().isEmpty())
			return false;
		return s.canBeReplaced() || s.is(Blocks.VINE);
	}

	@Override
	protected void entityInside(BlockState state, Level world, BlockPos pos, Entity e) {
		if (world.isClientSide || e instanceof ItemEntity || e instanceof ExperienceOrb)
			return;
		e.hurt(RotaryDamage.source(world, RotaryDamage.HEAT), 1);
		if ((state.getValue(FLAGS)&4) != 0 && e instanceof LivingEntity le)
			le.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
		return Shapes.empty();
	}

	@Override
	protected boolean skipRendering(BlockState state, BlockState adjacent, Direction dir) {
		return adjacent.is(this) || super.skipRendering(state, adjacent, dir);
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state, BlockGetter world, BlockPos pos) {
		return true;
	}
}
