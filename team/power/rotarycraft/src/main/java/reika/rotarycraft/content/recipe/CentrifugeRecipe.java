/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.fluids.FluidStack;

import reika.rotarycraft.registry.RotaryRecipeTypes;

/**
 * Original: RecipesCentrifuge.CentrifugeRecipe with DragonAPI's ChancedOutputList - every output rolls on its own with its chance
 * (in percent, capped at 100), the fluid with {@code fluid_chance}; {@code max_stack} items of the input are processed per operation.
 */
public record CentrifugeRecipe(Ingredient ingredient, List<ChanceOutput> outputs, Optional<FluidStack> fluid, float fluidChance, int maxStack)
		implements Recipe<SingleRecipeInput> {

	public record ChanceOutput(ItemStack item, float chance) {
		public static final Codec<ChanceOutput> CODEC = RecordCodecBuilder.create(i -> i.group(
				ItemStack.STRICT_CODEC.fieldOf("item").forGetter(ChanceOutput::item),
				Codec.FLOAT.fieldOf("chance").forGetter(ChanceOutput::chance)).apply(i, ChanceOutput::new));

		/** ItemWithChance.getNormalizedChance: chance/100, at most 1. */
		public float normalized() {
			return Math.min(1, chance/100F);
		}
	}

	public static final MapCodec<CentrifugeRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CentrifugeRecipe::ingredient),
			ChanceOutput.CODEC.listOf().optionalFieldOf("outputs", List.of()).forGetter(CentrifugeRecipe::outputs),
			FluidStack.CODEC.optionalFieldOf("fluid").forGetter(CentrifugeRecipe::fluid),
			Codec.FLOAT.optionalFieldOf("fluid_chance", 100F).forGetter(CentrifugeRecipe::fluidChance),
			Codec.INT.optionalFieldOf("max_stack", 1).forGetter(CentrifugeRecipe::maxStack)).apply(i, CentrifugeRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, CentrifugeRecipe> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

	/** Original rollItems: each output with its chance (whole multiples of 100 % always). */
	public List<ItemStack> rollItems(RandomSource rand) {
		List<ItemStack> li = new ArrayList<>();
		for (ChanceOutput o : outputs) {
			float ch = o.normalized();
			while (ch >= 1) {
				li.add(o.item().copy());
				ch -= 1;
			}
			if (ch > 0 && rand.nextDouble() < ch)
				li.add(o.item().copy());
		}
		return li;
	}

	/** Original rollFluid. */
	public FluidStack rollFluid(RandomSource rand) {
		if (fluid.isEmpty())
			return FluidStack.EMPTY;
		return rand.nextDouble() < fluidChance/100D ? fluid.get().copy() : FluidStack.EMPTY;
	}

	@Override
	public boolean matches(SingleRecipeInput in, Level level) {
		return ingredient.test(in.item());
	}

	@Override
	public ItemStack assemble(SingleRecipeInput in, HolderLookup.Provider registries) {
		return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).item().copy();
	}

	@Override
	public boolean canCraftInDimensions(int w, int h) {
		return true;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).item();
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return RotaryRecipeTypes.CENTRIFUGE_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return RotaryRecipeTypes.CENTRIFUGE.get();
	}
}
