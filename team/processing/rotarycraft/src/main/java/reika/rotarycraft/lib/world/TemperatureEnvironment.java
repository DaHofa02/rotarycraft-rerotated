/*******************************************************************************
 * @author Reika Kalseki (original DragonAPI). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.lib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Original: DragonAPI ReikaWorldHelper.temperatureEnvironment / isMeltable / flammable / ignite and Instantiable.TemperatureEffect.
 * The original keyed the effects by block Material, which no longer exists. Each Material is mapped to the vanilla tags or blocks
 * that made it up in 1.7.10:
 * <ul>
 * <li>rock / iron: pickaxe-mineable blocks; "iron" are those with metal sound (melt at 2000 instead of 1500)</li>
 * <li>ice: #ice; snow / craftedSnow: #snow</li>
 * <li>cloth: #wool; wood: #mineable/axe</li>
 * <li>grass, ground, sand: #dirt, #sand and gravel</li>
 * <li>leaves, plants, vine, web: #leaves, #flowers, #saplings, #crops, grass, ferns, dead bush, vines, cobweb</li>
 * <li>tnt: TNT</li>
 * </ul>
 * Only the non-corner form (6 directions, 1-3 blocks out) is ported; nothing ported so far uses the corner form.
 */
public final class TemperatureEnvironment {

	private TemperatureEnvironment() {}

	private enum Effect {
		ROCK_MELTING(1500),
		GROUND_GLASSING(900),
		SNOW_VAPORIZATION(0),
		ICE_MELTING(0),
		WOOD_IGNITION(450),
		WOOL_IGNITION(600),
		TNT_IGNITION(300),
		PLANT_IGNITION(230);

		final int minimumTemperature;

		Effect(int t) {
			minimumTemperature = t;
		}

		void apply(Level world, BlockPos p, int temperature) {
			switch(this) {
				case ROCK_MELTING -> {
					if (isMeltable(world, p, temperature))
						world.setBlockAndUpdate(p, flowing(Blocks.LAVA));
				}
				case GROUND_GLASSING -> world.setBlockAndUpdate(p, Blocks.GLASS.defaultBlockState());
				case SNOW_VAPORIZATION -> world.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
				case ICE_MELTING -> world.setBlockAndUpdate(p, flowing(Blocks.WATER));
				default -> {
					if (flammable(world, p))
						ignite(world, p);
				}
			}
		}
	}

	/** Original setBlock(Blocks.flowing_water / flowing_lava) with metadata 0, which is a source block. */
	private static BlockState flowing(Block b) {
		return b.defaultBlockState();
	}

	private static Effect getEffect(BlockState b) {
		if (b.isAir())
			return null;
		if (b.is(BlockTags.ICE))
			return Effect.ICE_MELTING;
		if (b.is(BlockTags.SNOW))
			return Effect.SNOW_VAPORIZATION;
		if (b.is(BlockTags.WOOL))
			return Effect.WOOL_IGNITION;
		if (b.is(Blocks.TNT))
			return Effect.TNT_IGNITION;
		if (b.is(BlockTags.LEAVES) || b.is(BlockTags.FLOWERS) || b.is(BlockTags.SAPLINGS) || b.is(BlockTags.CROPS) || b.is(Blocks.SHORT_GRASS)
				|| b.is(Blocks.TALL_GRASS) || b.is(Blocks.FERN) || b.is(Blocks.LARGE_FERN) || b.is(Blocks.DEAD_BUSH) || b.is(Blocks.VINE) || b.is(Blocks.COBWEB))
			return Effect.PLANT_IGNITION;
		if (b.is(BlockTags.DIRT) || b.is(BlockTags.SAND) || b.is(Blocks.GRAVEL))
			return Effect.GROUND_GLASSING;
		if (b.is(BlockTags.MINEABLE_WITH_AXE))
			return Effect.WOOD_IGNITION;
		if (b.is(BlockTags.MINEABLE_WITH_PICKAXE))
			return Effect.ROCK_MELTING;
		return null;
	}

	/** Applies temperature effects to the environment. */
	public static void temperatureEnvironment(Level world, BlockPos pos, int temperature) {
		if (temperature < 0) {
			for (Direction side : Direction.values()) {
				BlockPos p = pos.relative(side);
				BlockState b = world.getBlockState(p);
				if (b.is(Blocks.WATER) && world.isLoaded(p))
					world.setBlockAndUpdate(p, Blocks.ICE.defaultBlockState());
			}
		}
		for (Direction dir : Direction.values()) {
			for (int d = 1; d < 4; d++) {
				BlockPos p = pos.relative(dir, d);
				if (!world.isLoaded(p))
					continue;
				Effect eff = getEffect(world.getBlockState(p));
				if (eff != null && temperature >= eff.minimumTemperature) {
					eff.apply(world, p, temperature);
				}
			}
		}
	}

	public static boolean isMeltable(Level world, BlockPos p, int temperature) {
		BlockState b = world.getBlockState(p);
		if (b.isAir() || b.is(Blocks.BEDROCK))
			return false;
		if (b.is(Blocks.OBSIDIAN))
			return temperature >= 1800;
		if (!b.is(BlockTags.MINEABLE_WITH_PICKAXE))
			return false;
		if (b.getSoundType(world, p, null) == SoundType.METAL)
			return temperature >= 2000;
		return temperature >= 1500;
	}

	/** Original flammable(): not air, not a trapdoor or chest, and burns. */
	public static boolean flammable(Level world, BlockPos p) {
		BlockState b = world.getBlockState(p);
		if (b.isAir())
			return false;
		if (b.is(BlockTags.WOODEN_TRAPDOORS) || b.is(Blocks.CHEST))
			return false;
		return b.isFlammable(world, p, Direction.UP);
	}

	/** Surrounds the block with fire. */
	public static void ignite(Level world, BlockPos p) {
		if (world.getBlockState(p).isAir())
			return;
		for (Direction d : Direction.values()) {
			BlockPos n = p.relative(d);
			if (world.getBlockState(n).isAir())
				world.setBlockAndUpdate(n, net.minecraft.world.level.block.BaseFireBlock.getState(world, n));
		}
	}
}
