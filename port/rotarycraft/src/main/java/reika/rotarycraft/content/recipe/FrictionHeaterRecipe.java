/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.recipe;

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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import reika.rotarycraft.registry.RotaryRecipeTypes;

/**
 * Original: RecipesFrictionHeater.FrictionRecipe - extra furnace smelting that only works while a Friction Heater keeps the vanilla
 * furnace at {@code temperature} or more; takes {@code duration} heater ticks. Matching (input + temperature) is done by the heater.
 */
public record FrictionHeaterRecipe(Ingredient ingredient, ItemStack result, int temperature, int duration) implements Recipe<SingleRecipeInput> {

	public static final MapCodec<FrictionHeaterRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(FrictionHeaterRecipe::ingredient),
			ItemStack.STRICT_CODEC.fieldOf("result").forGetter(FrictionHeaterRecipe::result),
			Codec.INT.fieldOf("temperature").forGetter(FrictionHeaterRecipe::temperature),
			Codec.INT.fieldOf("duration").forGetter(FrictionHeaterRecipe::duration)).apply(i, FrictionHeaterRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, FrictionHeaterRecipe> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

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
		return RotaryRecipeTypes.FRICTION_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return RotaryRecipeTypes.FRICTION.get();
	}
}
