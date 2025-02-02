package xyz.dahofa.rotarycraft.common.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import xyz.dahofa.rotarycraft.api.lib.Names;
import xyz.dahofa.rotarycraft.common.recipe.GrinderRecipe;
import xyz.dahofa.rotarycraft.common.registry.RCRecipes;
import xyz.dahofa.rotarycraft.common.screen.GrinderScreen;

import java.util.List;

@JeiPlugin
public class JEIRotaryCraftPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new GrinderRecipeCategory(
                registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();

        List<GrinderRecipe> grinderRecipes = recipeManager
                .getAllRecipesFor(RCRecipes.GRINDER_TYPE.get()).stream().map(RecipeHolder::value).toList();
        registration.addRecipes(GrinderRecipeCategory.GRINDER_RECIPE_RECIPE_TYPE, grinderRecipes);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(GrinderScreen.class, 99, 34, 24, 17, GrinderRecipeCategory.GRINDER_RECIPE_RECIPE_TYPE);
    }
}
