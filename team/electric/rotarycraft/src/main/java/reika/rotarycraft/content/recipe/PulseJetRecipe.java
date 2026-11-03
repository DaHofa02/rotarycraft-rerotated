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
 * Original: RecipesPulseFurnace.PulseJetRecipe - smelting in the Pulse Jet Furnace once it is at {@code temperature} or hotter
 * (default: half the item's melting point, 400-850 °C). The furnace does the matching.
 */
public record PulseJetRecipe(Ingredient ingredient, ItemStack result, int temperature) implements Recipe<SingleRecipeInput> {

	public static final MapCodec<PulseJetRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(PulseJetRecipe::ingredient),
			ItemStack.STRICT_CODEC.fieldOf("result").forGetter(PulseJetRecipe::result),
			Codec.INT.fieldOf("temperature").forGetter(PulseJetRecipe::temperature)).apply(i, PulseJetRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, PulseJetRecipe> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

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
		return RotaryRecipeTypes.PULSE_JET_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return RotaryRecipeTypes.PULSE_JET.get();
	}
}
