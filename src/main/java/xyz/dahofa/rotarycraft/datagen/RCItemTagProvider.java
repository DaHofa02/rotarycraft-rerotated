package xyz.dahofa.rotarycraft.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;
import xyz.dahofa.rotarycraft.api.data.RotaryCraftTags;
import xyz.dahofa.rotarycraft.api.lib.Names;
import xyz.dahofa.rotarycraft.common.registry.RCItems;
import xyz.dahofa.rotarycraft.common.registry.RCTagRegistry;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class RCItemTagProvider extends ItemTagsProvider {
    public RCItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, Names.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        addItemsToTag(RotaryCraftTags.Items.INGOTS_TIN, RCItems.TIN_INGOT);
        appendToTag(Tags.Items.INGOTS, RotaryCraftTags.Items.INGOTS_TIN);
        addItemsToTag(RotaryCraftTags.Items.INGOTS_SPRING_STEEL, RCItems.SPRING_STEEL_INGOT);
        appendToTag(Tags.Items.INGOTS, RotaryCraftTags.Items.INGOTS_SPRING_STEEL);
        addItemsToTag(RotaryCraftTags.Items.INGOTS_HSLA_STEEL, RCItems.HSLA_STEEL_INGOT);
        appendToTag(Tags.Items.INGOTS, RotaryCraftTags.Items.INGOTS_HSLA_STEEL);
        addItemsToTag(RotaryCraftTags.Items.INGOTS_TUNGSTEN_ALLOY, RCItems.TUNGSTEN_ALLOY_INGOT);
        appendToTag(Tags.Items.INGOTS, RotaryCraftTags.Items.INGOTS_TUNGSTEN_ALLOY);
        addItemsToTag(RotaryCraftTags.Items.INGOTS_BEDROCK_ALLOY, RCItems.BEDROCK_ALLOY_INGOT);
        appendToTag(Tags.Items.INGOTS, RotaryCraftTags.Items.INGOTS_BEDROCK_ALLOY);

        addItemsToTag(RotaryCraftTags.Items.RODS_HSLA_STEEL, RCItems.HSLA_STEEL_ROD);
        appendToTag(Tags.Items.RODS, RotaryCraftTags.Items.RODS_HSLA_STEEL);
        addItemsToTag(RotaryCraftTags.Items.RODS_STONE, RCItems.STONE_ROD);
        appendToTag(Tags.Items.RODS, RotaryCraftTags.Items.RODS_STONE);
        addItemsToTag(RotaryCraftTags.Items.RODS_DIAMOND, RCItems.DIAMOND_ROD);
        appendToTag(Tags.Items.RODS, RotaryCraftTags.Items.RODS_DIAMOND);
        addItemsToTag(RotaryCraftTags.Items.RODS_TUNGSTEN_ALLOY, RCItems.TUNGSTEN_ALLOY_ROD);
        appendToTag(Tags.Items.RODS, RotaryCraftTags.Items.RODS_TUNGSTEN_ALLOY);
        addItemsToTag(RotaryCraftTags.Items.RODS_BEDROCK_ALLOY, RCItems.BEDROCK_ROD);
        appendToTag(Tags.Items.RODS, RotaryCraftTags.Items.RODS_BEDROCK_ALLOY);

        addItemsToTag(RotaryCraftTags.Items.DUSTS_ALUMINUM, RCItems.POWDERED_ALUMINUM);
        appendToTag(Tags.Items.DUSTS, RotaryCraftTags.Items.DUSTS_ALUMINUM);

        addItemsToTag(RotaryCraftTags.Items.GEAR_HSLA_STEEL, RCItems.HSLA_STEEL_GEAR);
        appendToTag(RCTagRegistry.GEARS, RotaryCraftTags.Items.GEAR_HSLA_STEEL);
        addItemsToTag(RotaryCraftTags.Items.GEAR_WOODEN, RCItems.WOODEN_GEAR);
        appendToTag(RCTagRegistry.GEARS, RotaryCraftTags.Items.GEAR_WOODEN);
        addItemsToTag(RotaryCraftTags.Items.GEAR_STONE, RCItems.STONE_GEAR);
        appendToTag(RCTagRegistry.GEARS, RotaryCraftTags.Items.GEAR_STONE);
        addItemsToTag(RotaryCraftTags.Items.GEAR_DIAMOND, RCItems.DIAMOND_GEAR);
        appendToTag(RCTagRegistry.GEARS, RotaryCraftTags.Items.GEAR_DIAMOND);
        addItemsToTag(RotaryCraftTags.Items.GEAR_TUNGSTEN_ALLOY, RCItems.TUNGSTEN_ALLOY_GEAR);
        appendToTag(RCTagRegistry.GEARS, RotaryCraftTags.Items.GEAR_TUNGSTEN_ALLOY);
        addItemsToTag(RotaryCraftTags.Items.GEAR_BEDROCK_ALLOY, RCItems.BEDROCK_GEAR);
        appendToTag(RCTagRegistry.GEARS, RotaryCraftTags.Items.GEAR_BEDROCK_ALLOY);
    }

    @SafeVarargs
    private void addItemsToTag(TagKey<Item> tag, Supplier<? extends ItemLike>... items) {
        tag(tag).add(Arrays.stream(items).map(Supplier::get).map(ItemLike::asItem).toArray(Item[]::new));
    }

    @SafeVarargs
    private void appendToTag(TagKey<Item> tag, TagKey<Item>... toAppend) {
        tag(tag).addTags(toAppend);
    }
}
