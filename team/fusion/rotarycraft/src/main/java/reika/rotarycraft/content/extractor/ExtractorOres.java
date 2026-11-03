/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft / DragonAPI). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.extractor;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: DragonAPI ReikaOreHelper (the eight vanilla ores, their rarity) with RotaryCraft's RecipesExtractor chain
 * ore -> dust -> slurry -> solution -> flakes and ExtractorBonus. Ores are recognised like the original ore dictionary lookup
 * (getEntryByOreDict) through the convention tags c:ores/*, so deepslate and nether variants count too. Mod ores (ModOreList) and
 * custom extracts are not ported yet; their bonuses are left out.
 */
public final class ExtractorOres {

	private ExtractorOres() {}

	public enum OreRarity {
		EVERYWHERE, COMMON, AVERAGE, SCATTERED, SCARCE, RARE
	}

	public enum Ore {
		COAL("coal", OreRarity.COMMON, CraftingPart.COALOREDUST, CraftingPart.COALORESLURRY, CraftingPart.COALSOLUTION, CraftingPart.COALOREFLAKES),
		IRON("iron", OreRarity.AVERAGE, CraftingPart.IRONOREDUST, CraftingPart.IRONORESLURRY, CraftingPart.IRONSOLUTION, CraftingPart.IRONOREFLAKES),
		GOLD("gold", OreRarity.SCATTERED, CraftingPart.GOLDOREDUST, CraftingPart.GOLDORESLURRY, CraftingPart.GOLDSOLUTION, CraftingPart.GOLDOREFLAKES),
		REDSTONE("redstone", OreRarity.COMMON, CraftingPart.REDOREDUST, CraftingPart.REDORESLURRY, CraftingPart.REDSOLUTION, CraftingPart.REDOREFLAKES),
		LAPIS("lapis", OreRarity.SCARCE, CraftingPart.LAPISOREDUST, CraftingPart.LAPISORESLURRY, CraftingPart.LAPISSOLUTION, CraftingPart.LAPISOREFLAKES),
		DIAMOND("diamond", OreRarity.SCARCE, CraftingPart.DIAMONDOREDUST, CraftingPart.DIAMONDORESLURRY, CraftingPart.DIAMONDSOLUTION, CraftingPart.DIAMONDOREFLAKES),
		EMERALD("emerald", OreRarity.RARE, CraftingPart.EMERALDOREDUST, CraftingPart.EMERALDORESLURRY, CraftingPart.EMERALDSOLUTION, CraftingPart.EMERALDOREFLAKES),
		QUARTZ("quartz", OreRarity.EVERYWHERE, CraftingPart.NETHERQUARTZDUST, CraftingPart.QUARTZSLURRY, CraftingPart.QUARTZSOLUTION, CraftingPart.QUARTZFLAKES);

		public final TagKey<Item> oreTag;
		public final OreRarity rarity;
		/** dust, slurry, solution, flakes */
		private final CraftingPart[] stages;

		Ore(String tag, OreRarity r, CraftingPart... stages) {
			oreTag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ores/"+tag));
			rarity = r;
			this.stages = stages;
		}

		public Item stage(int i) {
			return RotaryItems.part(stages[i]);
		}
	}

	/** Original getOreType: the ore an item belongs to, from an ore block or one of its extracts. */
	@Nullable
	public static Ore getOre(ItemStack is) {
		if (is.isEmpty())
			return null;
		for (Ore o : Ore.values()) {
			if (is.is(o.oreTag))
				return o;
			for (int i = 0; i < 4; i++)
				if (is.is(o.stage(i)))
					return o;
		}
		return null;
	}

	/** Which extractor stage (0 ore, 1 dust, 2 slurry, 3 solution) an item is input for; -1 if none. */
	public static int getInputStage(ItemStack is) {
		for (Ore o : Ore.values()) {
			if (is.is(o.oreTag))
				return 0;
			for (int i = 0; i < 3; i++)
				if (is.is(o.stage(i)))
					return i+1;
		}
		return -1;
	}

	/** Original RecipesExtractor.getExtractionResult. */
	public static ItemStack getExtractionResult(ItemStack is) {
		Ore o = getOre(is);
		int stage = getInputStage(is);
		if (o == null || stage < 0)
			return ItemStack.EMPTY;
		return new ItemStack(o.stage(stage));
	}

	/** Original ExtractorBonus for the vanilla solutions (only those whose bonus item exists without other mods). */
	public enum Bonus {
		GOLD(Ore.GOLD, CraftingPart.SILVERFLAKES, 0.125F),
		IRON(Ore.IRON, CraftingPart.TUNGSTENFLAKES, 0.025F),
		COAL(Ore.COAL, null, 0.0625F), //gunpowder
		LAPIS(Ore.LAPIS, CraftingPart.ALUMINUMPOWDER, 0.125F), //getAluminumOutput() with "other mods' aluminum dust" off
		REDSTONE(Ore.REDSTONE, CraftingPart.ALUMINUMPOWDER, 0.25F);
		//Not ported: QUARTZ -> certus quartz flakes and EMERALD -> ruby flakes (mod ores)

		private final Ore source;
		@Nullable
		private final CraftingPart bonus;
		private final float probability;

		Bonus(Ore src, @Nullable CraftingPart out, float p) {
			source = src;
			bonus = out;
			probability = p;
		}

		public ItemStack getBonusItem() {
			return bonus == null ? new ItemStack(Items.GUNPOWDER) : new ItemStack(RotaryItems.part(bonus));
		}

		/** Original doBonus: 1 in (int)(1/probability). */
		public boolean doBonus(net.minecraft.util.RandomSource rand) {
			int chance = (int)(1F/probability);
			return rand.nextInt(chance) == 0;
		}

		@Nullable
		public static Bonus getBonusForIngredient(ItemStack is) {
			for (Bonus b : values())
				if (is.is(b.source.stage(2)))
					return b;
			return null;
		}
	}
}
