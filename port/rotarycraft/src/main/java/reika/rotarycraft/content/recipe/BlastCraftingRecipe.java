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
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import reika.rotarycraft.registry.RotaryRecipeTypes;

/**
 * Original: RecipesBlastFurnace.BlastCrafting (add3x3Crafting, addAlloyingRecipe) - a shaped (or, for alloys such as ReactorCraft's
 * neutron-absorbing alloy, shapeless) recipe laid out in the Blast Furnace's 3x3 grid that needs a minimum temperature. {@code speed}:
 * progress only advances on every speed-th tick (original "speed"); {@code xp} per item made.
 */
public record BlastCraftingRecipe(net.minecraft.world.item.crafting.CraftingRecipe recipe, int temperature, int speed, float xp, boolean alloy) implements Recipe<BlastFurnaceInput> {

	public static final MapCodec<BlastCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ShapedRecipe.Serializer.CODEC.forGetter(r -> (ShapedRecipe)r.recipe()),
			Codec.INT.fieldOf("temperature").forGetter(BlastCraftingRecipe::temperature),
			Codec.INT.optionalFieldOf("speed", 1).forGetter(BlastCraftingRecipe::speed),
			Codec.FLOAT.optionalFieldOf("xp", 0F).forGetter(BlastCraftingRecipe::xp),
			Codec.BOOL.optionalFieldOf("alloy", false).forGetter(BlastCraftingRecipe::alloy)).apply(i, BlastCraftingRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, BlastCraftingRecipe> STREAM_CODEC = StreamCodec.composite(
			ShapedRecipe.Serializer.STREAM_CODEC, r -> (ShapedRecipe)r.recipe(), ByteBufCodecs.VAR_INT, BlastCraftingRecipe::temperature,
			ByteBufCodecs.VAR_INT, BlastCraftingRecipe::speed, ByteBufCodecs.FLOAT, BlastCraftingRecipe::xp, ByteBufCodecs.BOOL, BlastCraftingRecipe::alloy,
			BlastCraftingRecipe::new);

	/** Shapeless variant ("rotarycraft:blast_crafting_shapeless"): the vanilla shapeless fields plus temperature, speed, xp, alloy. */
	public static final MapCodec<BlastCraftingRecipe> SHAPELESS_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			((MapCodec<net.minecraft.world.item.crafting.ShapelessRecipe>)RecipeSerializer.SHAPELESS_RECIPE.codec()).forGetter(r -> (net.minecraft.world.item.crafting.ShapelessRecipe)r.recipe()),
			Codec.INT.fieldOf("temperature").forGetter(BlastCraftingRecipe::temperature),
			Codec.INT.optionalFieldOf("speed", 1).forGetter(BlastCraftingRecipe::speed),
			Codec.FLOAT.optionalFieldOf("xp", 0F).forGetter(BlastCraftingRecipe::xp),
			Codec.BOOL.optionalFieldOf("alloy", false).forGetter(BlastCraftingRecipe::alloy)).apply(i, BlastCraftingRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, BlastCraftingRecipe> SHAPELESS_STREAM_CODEC = StreamCodec.composite(
			RecipeSerializer.SHAPELESS_RECIPE.streamCodec(), r -> (net.minecraft.world.item.crafting.ShapelessRecipe)r.recipe(),
			ByteBufCodecs.VAR_INT, BlastCraftingRecipe::temperature, ByteBufCodecs.VAR_INT, BlastCraftingRecipe::speed, ByteBufCodecs.FLOAT, BlastCraftingRecipe::xp,
			ByteBufCodecs.BOOL, BlastCraftingRecipe::alloy, BlastCraftingRecipe::new);

	public static class ShapelessSerializer implements RecipeSerializer<BlastCraftingRecipe> {
		@Override
		public MapCodec<BlastCraftingRecipe> codec() {
			return SHAPELESS_CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, BlastCraftingRecipe> streamCodec() {
			return SHAPELESS_STREAM_CODEC;
		}
	}

	public static class Serializer implements RecipeSerializer<BlastCraftingRecipe> {
		@Override
		public MapCodec<BlastCraftingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, BlastCraftingRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}

	@Override
	public boolean matches(BlastFurnaceInput in, Level level) {
		return in.temperature() >= temperature && recipe.matches(in.grid(), level);
	}

	@Override
	public ItemStack assemble(BlastFurnaceInput in, HolderLookup.Provider registries) {
		return recipe.getResultItem(registries).copy();
	}

	@Override
	public boolean canCraftInDimensions(int w, int h) {
		return true;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return recipe.getResultItem(registries);
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return recipe instanceof ShapedRecipe ? RotaryRecipeTypes.BLAST_CRAFTING_SERIALIZER.get() : RotaryRecipeTypes.BLAST_CRAFTING_SHAPELESS_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return RotaryRecipeTypes.BLAST_CRAFTING.get();
	}
}
