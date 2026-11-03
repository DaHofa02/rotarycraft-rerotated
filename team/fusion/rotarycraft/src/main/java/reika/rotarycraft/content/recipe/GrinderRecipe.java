/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.recipe;

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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import reika.rotarycraft.registry.RotaryRecipeTypes;

/** Original: RecipesGrinder.GrinderRecipe - one item ground into an item stack. The recipes are data (rotarycraft:grinder). */
public record GrinderRecipe(Ingredient ingredient, ItemStack result) implements Recipe<SingleRecipeInput> {

	public static final MapCodec<GrinderRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(GrinderRecipe::ingredient),
			ItemStack.STRICT_CODEC.fieldOf("result").forGetter(GrinderRecipe::result)).apply(i, GrinderRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, GrinderRecipe> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

	@Override
	public boolean matches(SingleRecipeInput in, Level level) {
		return ingredient.test(in.item());
	}

	@Override
	public ItemStack assemble(SingleRecipeInput in, HolderLookup.Provider registries) {
		return result.copy();
	}

	@Override
	public boolean canCraftInDimensions(int w, int h) {
		return true;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return result;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return RotaryRecipeTypes.GRINDER_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return RotaryRecipeTypes.GRINDER.get();
	}
}
