package xyz.dahofa.rotarycraft.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;
import xyz.dahofa.rotarycraft.api.data.RotaryCraftTags;
import xyz.dahofa.rotarycraft.api.datagen.RotaryCraftRecipeHelper;
import xyz.dahofa.rotarycraft.api.lib.Names;
import xyz.dahofa.rotarycraft.common.registry.RCBlocks;
import xyz.dahofa.rotarycraft.common.registry.RCItems;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RCRecipeProvider extends RotaryCraftRecipeHelper {

    public RCRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        buildBlockCraftingRecipes(recipeOutput);
        buildItemCraftingRecipes(recipeOutput);
        buildSmeltingRecipes(recipeOutput);
    }

    protected void buildBlockCraftingRecipes(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCBlocks.HSLA_STEEL_BLOCK.get())
                .pattern("XXX")
                .pattern("XXX")
                .pattern("XXX")
                .define('X', RotaryCraftTags.Items.INGOTS_HSLA_STEEL)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCBlocks.BEDROCK_ALLOY_BLOCK.get())
                .pattern("XXX")
                .pattern("XXX")
                .pattern("XXX")
                .define('X', RCItems.BEDROCK_ALLOY_INGOT.get())
                .unlockedBy("has_bedrock_alloy", has(RCItems.BEDROCK_ALLOY_INGOT))
                .save(recipeOutput);
    }

    protected void buildItemCraftingRecipes(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.BASE_PANEL.get(), 3)
                .pattern("XXX")
                .define('X', RotaryCraftTags.Items.INGOTS_HSLA_STEEL)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.MOUNT.get())
                .pattern("X X")
                .pattern("XHX")
                .define('X', RotaryCraftTags.Items.INGOTS_HSLA_STEEL)
                .define('H', RCItems.BASE_PANEL.get())
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.MOUNT.get())
                .pattern("X X")
                .pattern("XHX")
                .define('X', RotaryCraftTags.Items.INGOTS_TIN)
                .define('H', RCItems.BASE_PANEL.get())
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":mount_from_tin");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.IRON_SCRAP.get(), 3)
                .pattern("XXX")
                .pattern("XXX")
                .pattern("XX ")
                .define('X', Items.IRON_BARS)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.IRON_SCRAP.get(), 3)
                .pattern("X  ")
                .define('X', Items.BUCKET)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":iron_scrap_from_bucket");





        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.WOODEN_GEAR.get(), 2)
                .requires(RCItems.WOODEN_2X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":wooden_gear_from_2x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.WOODEN_GEAR.get())
                .pattern(" X ")
                .pattern("XXX")
                .pattern(" X ")
                .define('X', ItemTags.PLANKS)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.WOODEN_2X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H', Tags.Items.RODS_WOODEN)
                .define('X', RotaryCraftTags.Items.GEAR_WOODEN)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.WOODEN_2X_GEAR_UNIT.get(), 2)
                .requires(RCItems.WOODEN_4X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":wooden_2x_gear_unit_from_4x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.WOODEN_4X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H', Tags.Items.RODS_WOODEN)
                .define('X', RotaryCraftTags.Items.GEAR_WOODEN)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.WOODEN_4X_GEAR_UNIT.get(), 2)
                .requires(RCItems.WOODEN_8X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":wooden_4x_gear_unit_from_8x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.WOODEN_8X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H', Tags.Items.RODS_WOODEN)
                .define('X', RCItems.WOODEN_4X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.WOODEN_8X_GEAR_UNIT.get(), 2)
                .requires(RCItems.WOODEN_16X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":wooden_8x_gear_unit_from_16x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.WOODEN_16X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H', Tags.Items.RODS_WOODEN)
                .define('X', RCItems.WOODEN_8X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.WOODEN_SHAFT_BEARING.get())
                .pattern("HHH")
                .pattern("HXH")
                .pattern("HHH")
                .define('H', RCItems.BALL_BEARING)
                .define('X', ItemTags.PLANKS)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.WOODEN_SHAFT_CORE.get())
                .pattern("  H")
                .pattern(" X ")
                .pattern("H  ")
                .define('H', Tags.Items.RODS_WOODEN)
                .define('X', ItemTags.PLANKS)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);



        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.STONE_GEAR.get(), 2)
                .requires(RCItems.STONE_2X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":stone_gear_from_2x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.STONE_ROD.get(), 3)
                .pattern("  X")
                .pattern(" X ")
                .pattern("X  ")
                .define('X', Tags.Items.STONES)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.STONE_GEAR.get())
                .pattern(" X ")
                .pattern("XXX")
                .pattern(" X ")
                .define('X', Tags.Items.STONES)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.STONE_2X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H', RotaryCraftTags.Items.RODS_STONE)
                .define('X', RotaryCraftTags.Items.GEAR_STONE)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.STONE_2X_GEAR_UNIT.get(), 2)
                .requires(RCItems.STONE_4X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":stone_2x_gear_unit_from_4x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.STONE_4X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H',RotaryCraftTags.Items.RODS_STONE)
                .define('X', RotaryCraftTags.Items.GEAR_STONE)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.STONE_4X_GEAR_UNIT.get(), 2)
                .requires(RCItems.STONE_8X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":stone_4x_gear_unit_from_8x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.STONE_8X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H', RotaryCraftTags.Items.RODS_STONE)
                .define('X', RCItems.STONE_4X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.STONE_8X_GEAR_UNIT.get(), 2)
                .requires(RCItems.STONE_16X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":stone_8x_gear_unit_from_16x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.STONE_16X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H', RotaryCraftTags.Items.RODS_STONE)
                .define('X', RCItems.STONE_8X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.STONE_SHAFT_BEARING.get())
                .pattern("HHH")
                .pattern("HXH")
                .pattern("HHH")
                .define('H', RCItems.BALL_BEARING)
                .define('X', Tags.Items.STONES)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.STONE_SHAFT_CORE.get())
                .pattern("  H")
                .pattern(" X ")
                .pattern("H  ")
                .define('H', RotaryCraftTags.Items.RODS_STONE)
                .define('X', Tags.Items.STONES)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);



        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.HSLA_STEEL_GEAR.get(), 2)
                .requires(RCItems.HSLA_STEEL_2X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":hsla_steel_gear_from_2x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.HSLA_STEEL_ROD.get(), 3)
                .pattern("  X")
                .pattern(" X ")
                .pattern("X  ")
                .define('X', RotaryCraftTags.Items.INGOTS_HSLA_STEEL)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.WORM_GEAR.get())
                .pattern("H  ")
                .pattern(" X ")
                .pattern("  H")
                .define('H', RotaryCraftTags.Items.RODS_HSLA_STEEL)
                .define('X', RotaryCraftTags.Items.INGOTS_HSLA_STEEL)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.HSLA_STEEL_GEAR.get())
                .pattern(" X ")
                .pattern("XXX")
                .pattern(" X ")
                .define('X', RotaryCraftTags.Items.INGOTS_HSLA_STEEL)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.HSLA_STEEL_2X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H', RotaryCraftTags.Items.RODS_HSLA_STEEL)
                .define('X', RotaryCraftTags.Items.GEAR_HSLA_STEEL)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.HSLA_STEEL_2X_GEAR_UNIT.get(), 2)
                .requires(RCItems.HSLA_STEEL_4X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":hsla_steel_2x_gear_unit_from_4x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.HSLA_STEEL_4X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H',RotaryCraftTags.Items.RODS_HSLA_STEEL)
                .define('X', RotaryCraftTags.Items.GEAR_HSLA_STEEL)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.HSLA_STEEL_4X_GEAR_UNIT.get(), 2)
                .requires(RCItems.HSLA_STEEL_8X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":hsla_steel_4x_gear_unit_from_8x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.HSLA_STEEL_8X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H', RotaryCraftTags.Items.RODS_HSLA_STEEL)
                .define('X', RCItems.HSLA_STEEL_4X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.HSLA_STEEL_8X_GEAR_UNIT.get(), 2)
                .requires(RCItems.HSLA_STEEL_16X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":hsla_steel_8x_gear_unit_from_16x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.HSLA_STEEL_16X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H', RotaryCraftTags.Items.RODS_HSLA_STEEL)
                .define('X', RCItems.HSLA_STEEL_8X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.HSLA_STEEL_SHAFT_BEARING.get())
                .pattern("HHH")
                .pattern("HXH")
                .pattern("HHH")
                .define('H', RCItems.BALL_BEARING)
                .define('X', RotaryCraftTags.Items.INGOTS_HSLA_STEEL)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.HSLA_STEEL_SHAFT_CORE.get())
                .pattern("  H")
                .pattern(" X ")
                .pattern("H  ")
                .define('H', RotaryCraftTags.Items.RODS_HSLA_STEEL)
                .define('X', RotaryCraftTags.Items.INGOTS_HSLA_STEEL)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);



        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.DIAMOND_GEAR.get(), 2)
                .requires(RCItems.DIAMOND_2X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":diamond_gear_from_2x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.DIAMOND_ROD.get(), 3)
                .pattern("  X")
                .pattern(" X ")
                .pattern("X  ")
                .define('X', Tags.Items.GEMS_DIAMOND)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.DIAMOND_GEAR.get())
                .pattern(" X ")
                .pattern("XXX")
                .pattern(" X ")
                .define('X', Tags.Items.GEMS_DIAMOND)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.DIAMOND_2X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H', RotaryCraftTags.Items.RODS_DIAMOND)
                .define('X', RotaryCraftTags.Items.GEAR_DIAMOND)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.DIAMOND_2X_GEAR_UNIT.get(), 2)
                .requires(RCItems.DIAMOND_4X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":diamond_2x_gear_unit_from_4x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.DIAMOND_4X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H',RotaryCraftTags.Items.RODS_DIAMOND)
                .define('X', RotaryCraftTags.Items.GEAR_DIAMOND)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.DIAMOND_4X_GEAR_UNIT.get(), 2)
                .requires(RCItems.DIAMOND_8X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":diamond_4x_gear_unit_from_8x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.DIAMOND_8X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H', RotaryCraftTags.Items.RODS_DIAMOND)
                .define('X', RCItems.DIAMOND_4X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.DIAMOND_8X_GEAR_UNIT.get(), 2)
                .requires(RCItems.DIAMOND_16X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":diamond_8x_gear_unit_from_16x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.DIAMOND_16X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H', RotaryCraftTags.Items.RODS_DIAMOND)
                .define('X', RCItems.DIAMOND_8X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.DIAMOND_SHAFT_BEARING.get())
                .pattern("HHH")
                .pattern("HXH")
                .pattern("HHH")
                .define('H', RCItems.BALL_BEARING)
                .define('X', Tags.Items.GEMS_DIAMOND)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.DIAMOND_SHAFT_CORE.get())
                .pattern("  H")
                .pattern(" X ")
                .pattern("H  ")
                .define('H', RotaryCraftTags.Items.RODS_DIAMOND)
                .define('X', Tags.Items.GEMS_DIAMOND)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);





        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.TUNGSTEN_ALLOY_GEAR.get(), 2)
                .requires(RCItems.TUNGSTEN_ALLOY_2X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":tungsten_alloy_gear_from_2x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.TUNGSTEN_ALLOY_ROD.get(), 3)
                .pattern("  X")
                .pattern(" X ")
                .pattern("X  ")
                .define('X', RCItems.TUNGSTEN_ALLOY_INGOT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.TUNGSTEN_ALLOY_GEAR.get())
                .pattern(" X ")
                .pattern("XXX")
                .pattern(" X ")
                .define('X', RCItems.TUNGSTEN_ALLOY_INGOT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.TUNGSTEN_ALLOY_2X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H', RotaryCraftTags.Items.RODS_TUNGSTEN_ALLOY)
                .define('X', RotaryCraftTags.Items.GEAR_TUNGSTEN_ALLOY)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.TUNGSTEN_ALLOY_2X_GEAR_UNIT.get(), 2)
                .requires(RCItems.TUNGSTEN_ALLOY_4X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":tungsten_alloy_2x_gear_unit_from_4x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.TUNGSTEN_ALLOY_4X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H',RotaryCraftTags.Items.RODS_TUNGSTEN_ALLOY)
                .define('X', RotaryCraftTags.Items.GEAR_TUNGSTEN_ALLOY)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.TUNGSTEN_ALLOY_4X_GEAR_UNIT.get(), 2)
                .requires(RCItems.TUNGSTEN_ALLOY_8X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":tungsten_alloy_4x_gear_unit_from_8x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.TUNGSTEN_ALLOY_8X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H', RotaryCraftTags.Items.RODS_TUNGSTEN_ALLOY)
                .define('X', RCItems.TUNGSTEN_ALLOY_4X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.TUNGSTEN_ALLOY_8X_GEAR_UNIT.get(), 2)
                .requires(RCItems.TUNGSTEN_ALLOY_16X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":tungsten_alloy_8x_gear_unit_from_16x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.TUNGSTEN_ALLOY_16X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H', RotaryCraftTags.Items.RODS_TUNGSTEN_ALLOY)
                .define('X', RCItems.TUNGSTEN_ALLOY_8X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.TUNGSTEN_ALLOY_SHAFT_BEARING.get())
                .pattern("HHH")
                .pattern("HXH")
                .pattern("HHH")
                .define('H', RCItems.BALL_BEARING)
                .define('X', RCItems.TUNGSTEN_ALLOY_INGOT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.TUNGSTEN_ALLOY_SHAFT_CORE.get())
                .pattern("  H")
                .pattern(" X ")
                .pattern("H  ")
                .define('H', RotaryCraftTags.Items.RODS_TUNGSTEN_ALLOY)
                .define('X', RCItems.TUNGSTEN_ALLOY_INGOT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);






        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.BEDROCK_GEAR.get(), 2)
                .requires(RCItems.BEDROCK_2X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":bedrock_gear_from_2x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.BEDROCK_2X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H', RotaryCraftTags.Items.RODS_BEDROCK_ALLOY)
                .define('X', RotaryCraftTags.Items.GEAR_BEDROCK_ALLOY)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.BEDROCK_2X_GEAR_UNIT.get(), 2)
                .requires(RCItems.BEDROCK_4X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":bedrock_2x_gear_unit_from_4x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.BEDROCK_4X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H',RotaryCraftTags.Items.RODS_BEDROCK_ALLOY)
                .define('X', RotaryCraftTags.Items.GEAR_BEDROCK_ALLOY)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.BEDROCK_4X_GEAR_UNIT.get(), 2)
                .requires(RCItems.BEDROCK_8X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":bedrock_4x_gear_unit_from_8x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.BEDROCK_8X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H', RotaryCraftTags.Items.RODS_BEDROCK_ALLOY)
                .define('X', RCItems.BEDROCK_4X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RCItems.BEDROCK_8X_GEAR_UNIT.get(), 2)
                .requires(RCItems.BEDROCK_16X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":bedrock_8x_gear_unit_from_16x");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.BEDROCK_16X_GEAR_UNIT.get())
                .pattern(" XH")
                .pattern("HX ")
                .define('H', RotaryCraftTags.Items.RODS_BEDROCK_ALLOY)
                .define('X', RCItems.BEDROCK_8X_GEAR_UNIT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.BEDROCK_SHAFT_CORE.get())
                .pattern("  H")
                .pattern(" X ")
                .pattern("H  ")
                .define('H', RotaryCraftTags.Items.RODS_BEDROCK_ALLOY)
                .define('X', RCItems.BEDROCK_ALLOY_INGOT)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);




        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.WOODEN_FLYWHEEL_CORE.get())
                .pattern("HHH")
                .pattern("HXH")
                .pattern("HHH")
                .define('H', ItemTags.PLANKS)
                .define('X', RotaryCraftTags.Items.GEAR_HSLA_STEEL)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.STONE_FLYWHEEL_CORE.get())
                .pattern("HHH")
                .pattern("HXH")
                .pattern("HHH")
                .define('H', Tags.Items.STONES)
                .define('X', RotaryCraftTags.Items.GEAR_HSLA_STEEL)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.IRON_FLYWHEEL_CORE.get())
                .pattern("HHH")
                .pattern("HXH")
                .pattern("HHH")
                .define('H', Tags.Items.INGOTS_IRON)
                .define('X', RotaryCraftTags.Items.GEAR_HSLA_STEEL)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.GOLD_FLYWHEEL_CORE.get())
                .pattern("HHH")
                .pattern("HXH")
                .pattern("HHH")
                .define('H', Tags.Items.INGOTS_GOLD)
                .define('X', RotaryCraftTags.Items.GEAR_HSLA_STEEL)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.TUNGSTEN_ALLOY_FLYWHEEL_CORE.get())
                .pattern("HHH")
                .pattern("HXH")
                .pattern("HHH")
                .define('H', RotaryCraftTags.Items.INGOTS_TUNGSTEN_ALLOY)
                .define('X', RotaryCraftTags.Items.GEAR_HSLA_STEEL)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.BEDROCK_FLYWHEEL_CORE.get())
                .pattern("HHH")
                .pattern("HXH")
                .pattern("HHH")
                .define('H', RotaryCraftTags.Items.INGOTS_BEDROCK_ALLOY)
                .define('X', RotaryCraftTags.Items.GEAR_HSLA_STEEL)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);




        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.IMPELLER.get())
                .pattern(" X ")
                .pattern("XHX")
                .pattern(" X ")
                .define('X', RotaryCraftTags.Items.INGOTS_HSLA_STEEL)
                .define('H', RotaryCraftTags.Items.GEAR_HSLA_STEEL)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.IMPELLER.get())
                .pattern(" X ")
                .pattern("XHX")
                .pattern(" X ")
                .define('X', RotaryCraftTags.Items.INGOTS_TIN)
                .define('H', RotaryCraftTags.Items.GEAR_HSLA_STEEL)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput, Names.MOD_ID+":impeller_from_tin");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.COMPRESSOR.get())
                .pattern("XXX")
                .pattern("XHX")
                .pattern("XXX")
                .define('X', RotaryCraftTags.Items.INGOTS_HSLA_STEEL)
                .define('H', RotaryCraftTags.Items.GEAR_HSLA_STEEL)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.TURBINE.get())
                .pattern("XXX")
                .pattern("XHX")
                .pattern("XXX")
                .define('X', RCItems.PROPELLER_BLADE.get())
                .define('H', RCItems.COMPRESSOR.get())
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.DIFFUSER.get())
                .pattern(" XX")
                .pattern("X  ")
                .pattern(" XX")
                .define('X', RotaryCraftTags.Items.INGOTS_HSLA_STEEL)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.COMBUSTER.get())
                .pattern("XXX")
                .pattern("XRX")
                .pattern("XIX")
                .define('X', RotaryCraftTags.Items.INGOTS_HSLA_STEEL)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('I', RCItems.IGNITION_UNIT.get())
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.CYLINDER.get(), 2)
                .pattern("XXX")
                .pattern("X X")
                .pattern("XXX")
                .define('X', RotaryCraftTags.Items.INGOTS_HSLA_STEEL)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.GOLD_COIL.get())
                .pattern("GGG")
                .pattern("GXG")
                .pattern("GGG")
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('X', RotaryCraftTags.Items.INGOTS_HSLA_STEEL)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.PADDLE_PANEL.get())
                .pattern("XXX")
                .pattern("XXX")
                .pattern("GGG")
                .define('X', RCItems.BASE_PANEL.get())
                .define('G', RotaryCraftTags.Items.INGOTS_SPRING_STEEL)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RCItems.IGNITION_UNIT.get())
                .pattern("G G")
                .pattern("XRX")
                .pattern("XXX")
                .define('X', RotaryCraftTags.Items.INGOTS_HSLA_STEEL)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_hsla_steel", has(RCItems.HSLA_STEEL_INGOT))
                .save(recipeOutput);

    }

    protected void buildSmeltingRecipes(RecipeOutput recipeOutput) {
        List<ItemLike> IRON_SCRAP_SMELTABLES = List.of(RCItems.IRON_SCRAP);
        List<ItemLike> ALUMINUM_SMELTABLES = List.of(RCItems.POWDERED_ALUMINUM);

        oreSmelting(recipeOutput, IRON_SCRAP_SMELTABLES, RecipeCategory.MISC, Items.IRON_INGOT, 0.25f, 200, "iron_scrap");
        oreSmelting(recipeOutput, ALUMINUM_SMELTABLES, RecipeCategory.MISC, RCItems.ALUMINUM_INGOT.get(), 0.25f, 200, "aluminum");
    }

}
