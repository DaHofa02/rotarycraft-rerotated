/*******************************************************************************
 * Original: Reika.DragonAPI.Libraries.World.ReikaWorldHelper.getAmbientTemperatureAt and
 * ReikaBiomeHelper.getBiomeTemp, by Reika Kalseki. Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.lib.world;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import net.neoforged.neoforge.common.Tags;

/**
 * Ambient temperature in degrees Celsius, as used by RotaryCraft machines (engines, gearboxes, heaters).
 * Changes against the original, all forced by the 1.21.1 platform:
 * <ul>
 * <li>Biome classes come from the c: biome tags instead of the Forge 1.7.10 BiomeDictionary and hard-coded biome instances.</li>
 * <li>The temperature noise uses vanilla {@link SimplexNoise} seeded with the world seed instead of DragonAPI's Simplex3DGenerator,
 * at the same frequency (1/20) and amplitude (10).</li>
 * <li>The average ground level for the depth correction (1.7.10 WorldProvider.getAverageGroundLevel: 64, or 4 in flat worlds) is the
 * chunk generator's spawn height, which is 64 and the top of the layers in flat worlds; the client, which has no generator, uses the
 * sea level + 1.</li>
 * <li>The December "winter" easter egg (SpecialDayTracker) and season-mod support are not ported.</li>
 * </ul>
 */
public final class AmbientTemperature {

	private static final double TEMP_NOISE_BASE = 10;

	/** Keyed by dimension and world seed, like the original (dimension ID, seed) pair. */
	private static final Map<String, SimplexNoise> noise = new ConcurrentHashMap<>();

	private AmbientTemperature() {}

	/** Original: ReikaBiomeHelper.BiomeTemperatures */
	public enum BiomeTemperatures {
		LUNAR(-100),
		ICY(-20),
		COOL(10),
		TEMPERATE(25),
		WARM(30),
		HOT(40),
		FIERY(300);

		public final int ambientTemperature;

		BiomeTemperatures(int t) {
			ambientTemperature = t;
		}
	}

	public static BiomeTemperatures getBiomeTemperatureClass(Holder<Biome> biome, BlockPos pos) {
		if (biome.is(Tags.Biomes.IS_NETHER))
			return BiomeTemperatures.FIERY;
		if (biome.is(Tags.Biomes.IS_END))
			return BiomeTemperatures.LUNAR;
		boolean snowy = biome.is(Tags.Biomes.IS_SNOWY) || biome.is(Tags.Biomes.IS_ICY);
		if (biome.is(Tags.Biomes.IS_TAIGA) && !snowy)
			return BiomeTemperatures.COOL;
		if (snowy || biome.is(Tags.Biomes.IS_COLD) || biome.value().coldEnoughToSnow(pos))
			return BiomeTemperatures.ICY;
		if (biome.is(Tags.Biomes.IS_DESERT) || biome.is(Tags.Biomes.IS_HOT) || biome.is(Tags.Biomes.IS_JUNGLE) || biome.is(Tags.Biomes.IS_BADLANDS) || biome.is(Tags.Biomes.IS_WASTELAND))
			return BiomeTemperatures.HOT;
		return BiomeTemperatures.TEMPERATE;
	}

	public static int getBiomeTemp(Level world, BlockPos pos) {
		return getBiomeTemperatureClass(world.getBiome(pos), pos).ambientTemperature;
	}

	public static int getAmbientTemperatureAt(Level world, BlockPos pos) {
		return getAmbientTemperatureAt(world, pos, 1);
	}

	public static int getAmbientTemperatureAt(Level world, BlockPos pos, float varFactor) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		int Tamb = getBiomeTemp(world, pos);
		float temp = Tamb;

		if (varFactor > 0) {
			temp += (float)(getNoise(world).getValue(x/20D, y/20D, z/20D)*varFactor*TEMP_NOISE_BASE);
		}

		if (world.dimension() == Level.NETHER) {
			if (y > 128) {
				temp -= 50;
			}
			if (y < 45) {
				int d = y <= 30 ? 15 : 45-y;
				temp += 20*d;
			}
		}
		else if (world.dimensionType().hasSkyLight()) {
			if (world.canSeeSky(pos.above())) {
				float sun = getSunIntensity(world, true, 0);
				temp += (sun-0.75F)*(world.isRaining() ? 10 : 20);
			}
			if (!isVoidWorld(world, x, z)) {
				int h = getAverageGroundLevel(world);
				int dy = h-y;
				if (dy > 0) {
					if (dy < 20) {
						temp -= dy;
						temp = Math.max(temp, Tamb-20);
					}
					else if (dy < 25) {
						temp -= 2*(25-dy);
						temp = Math.max(temp, Tamb-20);
					}
					else {
						temp += 100*(dy-20)/h;
						temp = Math.min(temp, Tamb+70);
					}
				}
				if (y > 96) {
					temp -= (y-96)/4;
				}
			}
		}

		return (int)temp;
	}

	/** Original: WorldProvider.getAverageGroundLevel - 64, or the top of the layers in a flat world. */
	public static int getAverageGroundLevel(Level world) {
		if (world instanceof net.minecraft.server.level.ServerLevel sl)
			return sl.getChunkSource().getGenerator().getSpawnHeight(sl);
		return world.getSeaLevel()+1;
	}

	/** Original: ReikaWorldHelper.getSunIntensity */
	public static float getSunIntensity(Level world, boolean weather, float ptick) {
		float ang = world.getTimeOfDay(ptick);
		float base = 1.0F-(Mth.cos(ang*(float)Math.PI*2.0F)*2.0F+0.2F);

		if (base < 0.0F)
			base = 0.0F;

		if (base > 1.0F)
			base = 1.0F;

		base = 1.0F-base;
		if (weather) {
			base = (float)(base*(1.0D-world.getRainLevel(ptick)*5.0F / 16.0D));
			base = (float)(base*(1.0D-world.getThunderLevel(ptick)*5.0F / 16.0D));
		}
		return base;
	}

	/** Original: ReikaWorldHelper.isVoidWorld, with the bottom of the world in place of y=0 and y=1. */
	public static boolean isVoidWorld(Level world, int x, int z) {
		int bottom = world.getMinBuildHeight();
		return world.getBlockState(new BlockPos(x, bottom, z)).is(Blocks.AIR) || world.canSeeSky(new BlockPos(x, bottom+1, z));
	}

	private static SimplexNoise getNoise(Level world) {
		long seed = world instanceof ServerLevel sl ? sl.getSeed() : 0;
		return noise.computeIfAbsent(world.dimension().location()+":"+seed, k -> new SimplexNoise(RandomSource.create(seed)));
	}
}
