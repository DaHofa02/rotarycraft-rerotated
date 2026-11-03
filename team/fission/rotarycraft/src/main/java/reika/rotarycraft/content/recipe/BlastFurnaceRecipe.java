/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.recipe;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import reika.rotarycraft.registry.RotaryRecipeTypes;

/**
 * Original: RecipesBlastFurnace.BlastRecipe ("mainline" Blast Furnace production). Additives that are absent must have an empty slot
 * (original FlexibleIngredient.EMPTY); present additives need at least {@code count} items, and each of those is used up with the given
 * percent chance (scaled by the amount made and the difficulty, see the block entity). The main grid must hold only main items, exactly
 * {@code main_count} of them when {@code exact}, otherwise at least that many. Every {@code main_count} main items make the results once.
 * Original "steel block" case (iron block in, 2 steel + 3 charcoal + 5 iron out per block) is the only recipe with several results.
 */
public record BlastFurnaceRecipe(Optional<Additive> primary, Optional<Additive> secondary, Optional<Additive> tertiary, Ingredient main, int mainCount,
		boolean exact, List<ItemStack> results, float bonus, float xp, int temperature, boolean needsEmpty, boolean alloy) implements Recipe<BlastFurnaceInput> {

	public static final int CENTER_ADDITIVE = 0;
	public static final int LOWER_ADDITIVE = 11;
	public static final int UPPER_ADDITIVE = 14;

	/** Original FlexibleIngredient: chance in percent, count = number used (and required). */
	public record Additive(Ingredient ingredient, float chance, int count) {
		public static final Codec<Additive> CODEC = RecordCodecBuilder.create(i -> i.group(
				Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(Additive::ingredient),
				Codec.FLOAT.fieldOf("chance").forGetter(Additive::chance),
				Codec.INT.optionalFieldOf("count", 1).forGetter(Additive::count)).apply(i, Additive::new));

		/** Consumption chance as a fraction (original chanceToUse = chance/100). */
		public float chanceToUse() {
			return chance/100F;
		}
	}

	public static final MapCodec<BlastFurnaceRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Additive.CODEC.optionalFieldOf("primary").forGetter(BlastFurnaceRecipe::primary),
			Additive.CODEC.optionalFieldOf("secondary").forGetter(BlastFurnaceRecipe::secondary),
			Additive.CODEC.optionalFieldOf("tertiary").forGetter(BlastFurnaceRecipe::tertiary),
			Ingredient.CODEC_NONEMPTY.fieldOf("main").forGetter(BlastFurnaceRecipe::main),
			Codec.INT.optionalFieldOf("main_count", 1).forGetter(BlastFurnaceRecipe::mainCount),
			Codec.BOOL.optionalFieldOf("exact", false).forGetter(BlastFurnaceRecipe::exact),
			ItemStack.STRICT_CODEC.listOf().fieldOf("results").forGetter(BlastFurnaceRecipe::results),
			Codec.FLOAT.optionalFieldOf("bonus", 0F).forGetter(BlastFurnaceRecipe::bonus),
			Codec.FLOAT.optionalFieldOf("xp", 0F).forGetter(BlastFurnaceRecipe::xp),
			Codec.INT.fieldOf("temperature").forGetter(BlastFurnaceRecipe::temperature),
			Codec.BOOL.optionalFieldOf("needs_empty_output", false).forGetter(BlastFurnaceRecipe::needsEmpty),
			Codec.BOOL.optionalFieldOf("alloy", false).forGetter(BlastFurnaceRecipe::alloy)).apply(i, BlastFurnaceRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, BlastFurnaceRecipe> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

	public static class Serializer implements RecipeSerializer<BlastFurnaceRecipe> {
		@Override
		public MapCodec<BlastFurnaceRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, BlastFurnaceRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}

	/** Original FlexibleIngredient.matchWithSize; an absent additive needs an empty slot. */
	private static boolean matchWithSize(Optional<Additive> a, ItemStack in) {
		if (a.isEmpty())
			return in.isEmpty();
		return !in.isEmpty() && a.get().ingredient().test(in) && in.getCount() >= a.get().count();
	}

	public boolean isValidMainItem(ItemStack is) {
		return !is.isEmpty() && main.test(is);
	}

	/** Original RecipesBlastFurnace.matchRecipe and the temperature check of getRecipe. */
	@Override
	public boolean matches(BlastFurnaceInput in, Level level) {
		if (in.temperature() < temperature)
			return false;
		if (!matchWithSize(primary, in.getItem(CENTER_ADDITIVE)))
			return false;
		if (!matchWithSize(secondary, in.getItem(LOWER_ADDITIVE)))
			return false;
		if (!matchWithSize(tertiary, in.getItem(UPPER_ADDITIVE)))
			return false;
		int num = 0;
		for (int i = 1; i <= 9; i++) {
			ItemStack is = in.getItem(i);
			if (!is.isEmpty()) {
				if (this.isValidMainItem(is))
					num++;
				else
					return false;
			}
		}
		return exact ? num == mainCount : num >= mainCount;
	}

	/** Original getNumberProduced. */
	public int getNumberProduced(int mainItems) {
		return mainCount > 1 ? mainItems/mainCount : mainItems;
	}

	@Override
	public ItemStack assemble(BlastFurnaceInput in, HolderLookup.Provider registries) {
		return results.get(0).copy();
	}

	@Override
	public boolean canCraftInDimensions(int w, int h) {
		return true;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return results.get(0).copy();
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return RotaryRecipeTypes.BLAST_FURNACE_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return RotaryRecipeTypes.BLAST_FURNACE.get();
	}
}
