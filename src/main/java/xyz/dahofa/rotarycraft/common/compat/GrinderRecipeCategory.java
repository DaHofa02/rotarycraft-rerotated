package xyz.dahofa.rotarycraft.common.compat;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import xyz.dahofa.rotarycraft.api.lib.Names;
import xyz.dahofa.rotarycraft.common.recipe.GrinderRecipe;
import xyz.dahofa.rotarycraft.common.registry.RCBlocks;

public class GrinderRecipeCategory implements IRecipeCategory<GrinderRecipe> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, "grinding");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, "textures/gui/grinder.png");

    public static final RecipeType<GrinderRecipe> GRINDER_RECIPE_RECIPE_TYPE =
            new RecipeType<>(UID, GrinderRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    private static final int xOffset = 5;
    private static final int yOffset = 5;

    public GrinderRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, xOffset, yOffset, 176 - (xOffset * 2), 83 - yOffset);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(RCBlocks.GRINDER.asItem()));
    }

    @Override
    public RecipeType<GrinderRecipe> getRecipeType() {
        return GRINDER_RECIPE_RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Grinder");
    }

    @Override
    public @Nullable IDrawable getBackground() {
        return background;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GrinderRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 76 - xOffset, 35 - yOffset).addIngredients(recipe.getIngredients().get(0));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 136 - xOffset, 35 - yOffset).addItemStack(recipe.getResultItem(null));
    }
}
