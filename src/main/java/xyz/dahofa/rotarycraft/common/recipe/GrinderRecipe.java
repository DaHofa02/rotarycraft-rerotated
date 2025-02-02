package xyz.dahofa.rotarycraft.common.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import xyz.dahofa.rotarycraft.common.registry.RCRecipes;

public record GrinderRecipe(Ingredient inputItem, ItemStack output) implements Recipe<GrinderRecipeInput> {
    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(inputItem);
        return list;
    }

    @Override
    public boolean matches(GrinderRecipeInput pInput, Level pLevel) {
        if (pLevel.isClientSide()) {
            return false;
        }
        return inputItem.test(pInput.getItem(0));
    }

    @Override
    public ItemStack assemble(GrinderRecipeInput pInput, HolderLookup.Provider pRegistries) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider pRegistries) {
        return output;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RCRecipes.GRINDER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RCRecipes.GRINDER_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<GrinderRecipe> {
        public static final MapCodec<GrinderRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(GrinderRecipe::inputItem),
                ItemStack.CODEC.fieldOf("result").forGetter(GrinderRecipe::output)
        ).apply(inst, GrinderRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, GrinderRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, GrinderRecipe::inputItem,
                        ItemStack.STREAM_CODEC, GrinderRecipe::output,
                        GrinderRecipe::new);


        @Override
        public MapCodec<GrinderRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GrinderRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
