/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.api.interfaces.Laserable;
import reika.rotarycraft.lib.world.TemperatureEnvironment;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Weaponry.TileEntityHeatRay (TileEntityBeamMachine) - takes at least 2 MW from behind and
 * fires a beam 8 + (power - 2 MW)/256 blocks ahead (at most the configured range, at least 64, default 128). Every tick it sets
 * flammable blocks in the beam alight, gives Laserable blocks / block entities / entities their effect, applies its block effects
 * (plants, snow, water and webs vanish, stone melts to lava, ice to water, leaves and logs burn, gravel and mossy cobble become
 * cobblestone, grass becomes dirt, dirt becomes sand, sand becomes glass, TNT is lit, netherrack explodes; these wait
 * getDelayTick ticks, during which they shield what is behind them, and need the "attack blocks" option) and stops at the first
 * opaque block. Entities in the beam (not items) burn for 2 + 16*power/2MW seconds. As in the original the burn box is empty when
 * firing west or down.
 */
public class HeatRayBlockEntity extends PowerReceiverBlockEntity {

	/** Rate of conversion - one power++ = 1/falloff ++ blocks range. */
	public static final int FALLOFF = 256;

	private static final List<Effect> BLOCK_EFFECTS = new ArrayList<>();

	private record Effect(Predicate<BlockState> match, LaserEffect effect) {}

	private static final LaserEffect DELETION = new LaserEffect() {
		@Override
		public boolean doEffect(Level world, BlockPos p, long power, int range, HeatRayBlockEntity te) {
			world.removeBlock(p, false);
			fizz(world, p);
			return false;
		}

		@Override
		public int getDelayTick(BlockState b, long surplus, int dist) {
			return (int)Math.min(Integer.MAX_VALUE, (4*dist/(8*surplus)));
		}
	};

	private static final LaserEffect IGNITE = new LaserEffect() {
		@Override
		public boolean doEffect(Level world, BlockPos p, long power, int range, HeatRayBlockEntity te) {
			TemperatureEnvironment.ignite(world, p);
			return false;
		}

		@Override
		public int getDelayTick(BlockState b, long surplus, int dist) {
			return 0;
		}
	};

	static {
		Block[] delete = {Blocks.SHORT_GRASS, Blocks.FERN, Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM, Blocks.WHEAT, Blocks.PUMPKIN_STEM,
				Blocks.ATTACHED_PUMPKIN_STEM, Blocks.MELON_STEM, Blocks.ATTACHED_MELON_STEM, Blocks.POTATOES, Blocks.CARROTS, Blocks.DEAD_BUSH,
				Blocks.VINE, Blocks.LILY_PAD, Blocks.COBWEB, Blocks.SNOW, Blocks.WATER};
		for (Block b : delete)
			add(b, DELETION);
		add(s -> s.is(BlockTags.SMALL_FLOWERS), DELETION); //red_flower, yellow_flower
		add(s -> s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS_THAT_BURN), IGNITE);
		LaserEffect lava = new MeltEffect(Blocks.LAVA, 2);
		for (Block b : new Block[]{Blocks.COBBLESTONE, Blocks.STONE, Blocks.SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.CUT_SANDSTONE,
				Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS})
			add(b, lava);
		LaserEffect water = new MeltEffect(Blocks.WATER, 8);
		add(Blocks.ICE, water);
		add(Blocks.SNOW_BLOCK, water);
		add(Blocks.GRAVEL, new BlockChangeEffect(Blocks.COBBLESTONE));
		add(Blocks.MOSSY_COBBLESTONE, new BlockChangeEffect(Blocks.COBBLESTONE));
		add(Blocks.GRASS_BLOCK, new BlockChangeEffect(Blocks.DIRT));
		add(Blocks.MYCELIUM, new BlockChangeEffect(Blocks.DIRT));
		LaserEffect sandify = new BlockChangeEffect(Blocks.SAND);
		for (Block b : new Block[]{Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.PODZOL, Blocks.FARMLAND})
			add(b, sandify);
		add(Blocks.SAND, new BlockChangeEffect(Blocks.GLASS));
		add(Blocks.RED_SAND, new BlockChangeEffect(Blocks.GLASS));
		add(Blocks.TNT, new LaserEffect() {
			@Override
			public boolean doEffect(Level world, BlockPos p, long power, int range, HeatRayBlockEntity te) {
				world.removeBlock(p, false);
				PrimedTnt tnt = new PrimedTnt(world, p.getX()+0.5D, p.getY()+0.5D, p.getZ()+0.5D, null);
				world.addFreshEntity(tnt);
				world.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
				return false;
			}

			@Override
			public int getDelayTick(BlockState b, long surplus, int dist) {
				return 0;
			}
		});
		add(Blocks.NETHERRACK, new LaserEffect() {
			@Override
			public boolean doEffect(Level world, BlockPos p, long power, int range, HeatRayBlockEntity te) {
				world.explode(null, p.getX()+0.5, p.getY()+0.5, p.getZ()+0.5, 5F, true, Level.ExplosionInteraction.BLOCK);
				return true;
			}

			@Override
			public int getDelayTick(BlockState b, long surplus, int dist) {
				return 6;
			}
		});
	}

	private static void add(Block b, LaserEffect e) {
		add(s -> s.is(b), e);
	}

	private static void add(Predicate<BlockState> p, LaserEffect e) {
		BLOCK_EFFECTS.add(new Effect(p, e));
	}

	@Nullable
	private static LaserEffect getEffect(BlockState s) {
		for (Effect e : BLOCK_EFFECTS)
			if (e.match().test(s))
				return e.effect();
		return null;
	}

	private static void fizz(Level world, BlockPos p) {
		world.playSound(null, p, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F+(rand.nextFloat()-rand.nextFloat())*0.8F);
	}

	private Direction facing = Direction.WEST;
	private int tickcount;

	public HeatRayBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.HEAT_RAY.get(), pos, state, 1, 1, 2097152);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	/** Original TileEntityBeamMachine.getIOSides: input behind, the beam goes the other way. */
	private void getIOSides(int meta) {
		read = switch (meta) {
			case 0 -> Direction.EAST;
			case 1 -> Direction.WEST;
			case 2 -> Direction.NORTH;
			case 3 -> Direction.SOUTH;
			case 4 -> Direction.DOWN;
			default -> Direction.UP;
		};
		facing = read.getOpposite();
	}

	public Direction getFacing() {
		return facing;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		tickcount++;
		this.getIOSides(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		this.readPower();
		power = (long)omega*(long)torque;
		this.makeBeam(world, pos, this.getBlockMetadata());
	}

	private void makeBeam(Level world, BlockPos pos, int meta) {
		if (power < MINPOWER) //2MW+ (real military laser)
			return;
		boolean blocked = false;
		int step;
		int maxdist = this.getRange();
		for (step = 1; step < maxdist && step < this.getMaxRange() && !blocked; step++) {
			BlockPos p = pos.relative(facing, step);
			BlockState s = world.getBlockState(p);
			if (!s.isAir() && s.isFlammable(world, p, Direction.UP))
				TemperatureEnvironment.ignite(world, p);
			if (this.affectBlock(world, p, step, s))
				blocked = true;
			if (s.isSolidRender(world, p))
				blocked = true;
		}
		AABB zone = this.getBurnZone(meta, step);
		if (zone == null)
			return;
		for (Entity caught : world.getEntitiesOfClass(Entity.class, zone)) {
			if (!(caught instanceof ItemEntity)) //Do not burn drops
				caught.igniteForSeconds(this.getBurnTime());
			if (caught instanceof PrimedTnt && world instanceof net.minecraft.server.level.ServerLevel sl)
				sl.sendParticles(ParticleTypes.LAVA, caught.getX()+rand.nextFloat(), caught.getY()+rand.nextFloat(), caught.getZ()+rand.nextFloat(), 1, 0, 0, 0, 0);
			if (caught instanceof Laserable l)
				l.whenInBeam(world, caught.blockPosition(), power, step);
		}
	}

	public int getBurnTime() {
		return 2+(int)(16L*power/MINPOWER);
	}

	public int getRange() {
		int r = (int)(8L+(power-MINPOWER)/FALLOFF);
		return Math.min(r, this.getMaxRange());
	}

	public int getMaxRange() {
		return Math.max(64, RotaryConfig.HEATRAYRANGE.get());
	}

	/** Original getBurnZone with its corner numbers; the west (0) and down (5) boxes there have no volume, so nothing is caught. */
	@Nullable
	private AABB getBurnZone(int meta, int step) {
		int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
		return switch (meta) {
			case 1 -> new AABB(x+1, y, z, x+step, y+1, z+1);
			case 2 -> new AABB(x, y, z+1, x+1, y+1, z+step);
			case 3 -> new AABB(x, y, z-step, x+1, y+1, z-1);
			case 4 -> new AABB(x, y+1, 0, x+1, y+step, z+1); //sic: minz stays 0
			default -> null;
		};
	}

	private boolean affectBlock(Level world, BlockPos p, int step, BlockState s) {
		if (s.isAir())
			return false;
		if (world.getBlockEntity(p) instanceof Laserable l) {
			l.whenInBeam(world, p, power, step);
			if (l.blockBeam(world, p, power))
				return true;
		}
		if (s.getBlock() instanceof Laserable l) {
			l.whenInBeam(world, p, power, step);
			return l.blockBeam(world, p, power);
		}
		if (!RotaryConfig.ATTACKBLOCKS.get())
			return false;
		LaserEffect e = getEffect(s);
		if (e == null)
			return false;
		long surp = power/MINPOWER;
		int n = e.getDelayTick(s, surp, step);
		if (tickcount >= n) {
			tickcount = 0;
			return e.doEffect(world, p, power, step, this);
		}
		return true;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	private interface LaserEffect {

		int getDelayTick(BlockState b, long surplus, int dist);

		boolean doEffect(Level world, BlockPos p, long power, int range, HeatRayBlockEntity te);
	}

	private record BlockChangeEffect(Block replacement) implements LaserEffect {

		@Override
		public boolean doEffect(Level world, BlockPos p, long power, int range, HeatRayBlockEntity te) {
			world.setBlockAndUpdate(p, replacement.defaultBlockState());
			return true;
		}

		@Override
		public int getDelayTick(BlockState b, long surplus, int dist) {
			return (int)Math.min(Integer.MAX_VALUE, (4*dist)/surplus);
		}
	}

	/** Original MeltLaserEffect: the block becomes the (flowing, full) liquid; lava every 4*dist/(2*surplus), water 4*dist/(8*surplus) ticks. */
	private record MeltEffect(Block liquid, int d) implements LaserEffect {

		@Override
		public boolean doEffect(Level world, BlockPos p, long power, int range, HeatRayBlockEntity te) {
			world.setBlock(p, liquid.defaultBlockState(), 3);
			if (liquid == Blocks.LAVA && world instanceof net.minecraft.server.level.ServerLevel sl)
				sl.sendParticles(ParticleTypes.LAVA, p.getX()+rand.nextFloat(), p.getY()+rand.nextFloat(), p.getZ()+rand.nextFloat(), 1, 0, 0, 0, 0);
			fizz(world, p);
			return false;
		}

		@Override
		public int getDelayTick(BlockState b, long surplus, int dist) {
			return (int)Math.min(Integer.MAX_VALUE, (4*dist/(d*surplus)));
		}
	}
}
