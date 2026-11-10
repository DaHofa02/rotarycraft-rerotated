/*******************************************************************************
 * @author Reika Kalseki (original DragonAPI, ReikaBiomeHelper.getBiomeHumidity). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.lib.world;

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.Tags;

/**
 * Original ReikaBiomeHelper.getBiomeHumidity: a fixed humidity (0..1) per biome. The original compares with the 1.7.10 biome instances
 * (after mapping variants to their parent) and falls back to the Forge BiomeDictionary; here the vanilla and c: biome tags stand for
 * both. The fallback applies its limits in the order the original lists them (the original followed the order of the dictionary types).
 */
public final class BiomeHumidity {

	private BiomeHumidity() {}

	public static float get(Holder<Biome> b) {
		if (b.is(BiomeTags.IS_JUNGLE))
			return 0.95F;
		if (b.is(BiomeTags.IS_OCEAN))
			return 1F;
		if (b.is(Tags.Biomes.IS_SWAMP))
			return 0.85F;
		if (b.is(Tags.Biomes.IS_BIRCH_FOREST))
			return 0.55F;
		if (b.is(Biomes.DARK_FOREST))
			return 0.7F;
		if (b.is(Biomes.FOREST) || b.is(Biomes.FLOWER_FOREST))
			return 0.6F;
		if (b.is(Tags.Biomes.IS_PLAINS))
			return 0.4F;
		if (b.is(BiomeTags.IS_SAVANNA))
			return 0.3F;
		if (b.is(Tags.Biomes.IS_DESERT) || b.is(BiomeTags.IS_BADLANDS))
			return 0.2F;
		if (b.is(BiomeTags.IS_NETHER) || b.is(BiomeTags.IS_END))
			return 0.1F;
		if (b.is(Biomes.BEACH))
			return 0.98F;
		if (b.is(Biomes.SNOWY_PLAINS))
			return 0.4F;
		if (b.is(Biomes.MUSHROOM_FIELDS))
			return 0.75F;
		float val = 0.5F;
		if (b.is(BiomeTags.IS_BEACH))
			val = Math.max(val, 0.95F);
		if (b.is(BiomeTags.IS_RIVER) || b.is(Tags.Biomes.IS_AQUATIC))
			val = Math.max(val, 1F);
		if (b.is(Tags.Biomes.IS_SANDY))
			val = Math.min(val, 0.2F);
		if (b.is(Tags.Biomes.IS_WASTELAND))
			val = Math.min(val, 0.1F);
		if (b.is(Tags.Biomes.IS_LUSH))
			val = Math.max(val, 0.6F);
		if (b.is(Tags.Biomes.IS_WET))
			val = Math.max(val, 0.7F);
		if (b.is(Tags.Biomes.IS_DRY))
			val = Math.min(val, 0.3F);
		return val;
	}
}
