/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.recipe;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryItems;
import reika.rotarycraft.registry.RotaryRecipeTypes;

/**
 * Original: WorktableRecipes.WorktableRecipe. The Worktable crafts machine recipes (which the crafting table only makes with
 * "Crafting Table can Make Machines"), the RotaryCraft item recipes that ItemRegistry registered for both, and recycling recipes
 * (RecyclingRecipe: parts back into scrap). Three serializers, one recipe type.
 */
public interface WorktableRecipe extends Recipe<CraftingInput> {

	@Override
	default RecipeType<?> getType() {
		return RotaryRecipeTypes.WORKTABLE.get();
	}

	@Override
	default boolean isSpecial() {
		return true;
	}

	@Override
	default boolean canCraftInDimensions(int w, int h) {
		return true;
	}

	default boolean isRecycling() {
		return false;
	}

	/** Pattern positions (0-8 in a 3x3 grid) of the ingredients, for uncrafting; null where the recipe has none. */
	List<Ingredient> getGridLayout();

	record Shaped(ShapedRecipe recipe) implements WorktableRecipe {
		public static final MapCodec<Shaped> CODEC = ShapedRecipe.Serializer.CODEC.xmap(Shaped::new, Shaped::recipe);
		public static final StreamCodec<RegistryFriendlyByteBuf, Shaped> STREAM_CODEC = ShapedRecipe.Serializer.STREAM_CODEC.map(Shaped::new, Shaped::recipe);

		@Override
		public boolean matches(CraftingInput in, Level level) {
			return recipe.matches(in, level);
		}

		@Override
		public ItemStack assemble(CraftingInput in, HolderLookup.Provider registries) {
			return recipe.assemble(in, registries);
		}

		@Override
		public ItemStack getResultItem(HolderLookup.Provider registries) {
			return recipe.getResultItem(registries);
		}

		@Override
		public NonNullList<Ingredient> getIngredients() {
			return recipe.getIngredients();
		}

		@Override
		public List<Ingredient> getGridLayout() {
			List<Ingredient> li = new ArrayList<>();
			for (int y = 0; y < 3; y++)
				for (int x = 0; x < 3; x++)
					li.add(x < recipe.getWidth() && y < recipe.getHeight() ? recipe.getIngredients().get(x+y*recipe.getWidth()) : Ingredient.EMPTY);
			return li;
		}

		@Override
		public RecipeSerializer<?> getSerializer() {
			return RotaryRecipeTypes.WORKTABLE_SHAPED.get();
		}
	}

	record Shapeless(ShapelessRecipe recipe) implements WorktableRecipe {
		@SuppressWarnings("unchecked")
		public static final MapCodec<Shapeless> CODEC = ((MapCodec<ShapelessRecipe>)RecipeSerializer.SHAPELESS_RECIPE.codec()).xmap(Shapeless::new, Shapeless::recipe);
		public static final StreamCodec<RegistryFriendlyByteBuf, Shapeless> STREAM_CODEC = RecipeSerializer.SHAPELESS_RECIPE.streamCodec().map(Shapeless::new, Shapeless::recipe);

		@Override
		public boolean matches(CraftingInput in, Level level) {
			return recipe.matches(in, level);
		}

		@Override
		public ItemStack assemble(CraftingInput in, HolderLookup.Provider registries) {
			return recipe.assemble(in, registries);
		}

		@Override
		public ItemStack getResultItem(HolderLookup.Provider registries) {
			return recipe.getResultItem(registries);
		}

		@Override
		public NonNullList<Ingredient> getIngredients() {
			return recipe.getIngredients();
		}

		@Override
		public List<Ingredient> getGridLayout() {
			List<Ingredient> li = new ArrayList<>(recipe.getIngredients());
			while (li.size() < 9)
				li.add(Ingredient.EMPTY);
			return li;
		}

		@Override
		public RecipeSerializer<?> getSerializer() {
			return RotaryRecipeTypes.WORKTABLE_SHAPELESS.get();
		}
	}

	/** Original RecyclingRecipe: the listed parts (shapeless) give {@code scrap} pieces of scrap, split into full stacks. */
	record Recycling(NonNullList<Ingredient> ingredients, int scrap) implements WorktableRecipe {
		public static final MapCodec<Recycling> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").forGetter(r -> r.ingredients),
				Codec.intRange(1, 9*64).fieldOf("scrap").forGetter(Recycling::scrap))
				.apply(i, (li, n) -> new Recycling(NonNullList.of(Ingredient.EMPTY, li.toArray(Ingredient[]::new)), n)));
		public static final StreamCodec<RegistryFriendlyByteBuf, Recycling> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

		private ShapelessRecipe matcher() {
			return new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(RotaryItems.part(CraftingPart.SCRAP)), ingredients);
		}

		@Override
		public boolean matches(CraftingInput in, Level level) {
			return this.matcher().matches(in, level);
		}

		@Override
		public ItemStack assemble(CraftingInput in, HolderLookup.Provider registries) {
			return this.getResultItem(registries);
		}

		/** Original getRecipeOutput: scrap x value (may be more than a stack; it is split when crafted). */
		@Override
		public ItemStack getResultItem(HolderLookup.Provider registries) {
			return new ItemStack(RotaryItems.part(CraftingPart.SCRAP), scrap);
		}

		/** Original getSplitOutput. */
		public List<ItemStack> getSplitOutput() {
			List<ItemStack> li = new ArrayList<>();
			int val = scrap;
			ItemStack one = new ItemStack(RotaryItems.part(CraftingPart.SCRAP));
			while (val > 0) {
				int num = Math.min(val, one.getMaxStackSize());
				li.add(one.copyWithCount(num));
				val -= num;
			}
			return li;
		}

		@Override
		public NonNullList<Ingredient> getIngredients() {
			return ingredients;
		}

		@Override
		public boolean isRecycling() {
			return true;
		}

		@Override
		public List<Ingredient> getGridLayout() {
			return List.of();
		}

		@Override
		public RecipeSerializer<?> getSerializer() {
			return RotaryRecipeTypes.RECYCLING.get();
		}
	}

	static <T extends Recipe<?>> RecipeSerializer<T> serializer(MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> stream) {
		return new RecipeSerializer<>() {
			@Override
			public MapCodec<T> codec() {
				return codec;
			}

			@Override
			public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
				return stream;
			}
		};
	}
}
