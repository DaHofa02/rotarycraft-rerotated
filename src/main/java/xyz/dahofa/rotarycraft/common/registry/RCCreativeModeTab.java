package xyz.dahofa.rotarycraft.common.registry;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredRegister;
import xyz.dahofa.rotarycraft.api.lib.Names;
import xyz.dahofa.rotarycraft.common.config.RCConfig;
import xyz.dahofa.rotarycraft.common.item.CreativeTabStackProvider;
import xyz.dahofa.rotarycraft.common.item.gear.bedrock.*;
import xyz.dahofa.rotarycraft.common.item.gear.hslasteel.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static xyz.dahofa.rotarycraft.api.util.RotaryCraftUtils.xlate;

public class RCCreativeModeTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Names.MOD_ID);

    public static final Supplier<CreativeModeTab> TOOLS = TABS.register("tools", RCCreativeModeTab::buildToolsTab);
    public static final Supplier<CreativeModeTab> ITEMS = TABS.register("items", RCCreativeModeTab::buildItemsTab);
    public static final Supplier<CreativeModeTab> ORES = TABS.register("ores", RCCreativeModeTab::buildOresTab);

    private static CreativeModeTab buildToolsTab() {
        return CreativeModeTab.builder()
                .title(xlate("itemGroup.rotarycraft.tools"))
                .icon(() -> new ItemStack(RCItems.SCREWDRIVER.get()))
                .displayItems(RCCreativeModeTab::genDisplayTools)
                .build();
    }

    private static CreativeModeTab buildItemsTab() {
        return CreativeModeTab.builder()
                .title(xlate("itemGroup.rotarycraft.items"))
                .icon(() -> new ItemStack(RCItems.HSLA_STEEL_INGOT.get()))
                .displayItems(RCCreativeModeTab::genDisplayItems)
                .build();
    }

    private static CreativeModeTab buildOresTab() {
        return CreativeModeTab.builder()
                .title(xlate("itemGroup.rotarycraft.ores"))
                .icon(() -> new ItemStack(RCItems.BEDROCK_DUST.get()))
                .displayItems(RCCreativeModeTab::genDisplayOres)
                .build();
    }

    private static void genDisplayTools(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output) {
        HolderLookup.RegistryLookup<Enchantment> lookup = params.holders().lookupOrThrow(Registries.ENCHANTMENT);
        generateGearWithEnchants(output, RCItems.BEDROCK_HELMET,
                new EnchantmentInstance(lookup.getOrThrow(Enchantments.PROJECTILE_PROTECTION), 4),
                new EnchantmentInstance(lookup.getOrThrow(Enchantments.RESPIRATION), 3));
        generateGearWithEnchants(output, RCItems.BEDROCK_CHESTPLATE,
                new EnchantmentInstance(lookup.getOrThrow(Enchantments.BLAST_PROTECTION), 4));
        generateGearWithEnchants(output, RCItems.BEDROCK_LEGGINGS,
                new EnchantmentInstance(lookup.getOrThrow(Enchantments.FIRE_PROTECTION), 4));
        generateGearWithEnchants(output, RCItems.BEDROCK_BOOTS,
                new EnchantmentInstance(lookup.getOrThrow(Enchantments.FEATHER_FALLING), 4));
        generateGearWithEnchants(output, RCItems.BEDROCK_SWORD,
                new EnchantmentInstance(lookup.getOrThrow(Enchantments.LOOTING), 5),
                new EnchantmentInstance(lookup.getOrThrow(Enchantments.SHARPNESS), 5));
        generateGearWithEnchants(output, RCItems.BEDROCK_PICKAXE,
                new EnchantmentInstance(lookup.getOrThrow(Enchantments.SILK_TOUCH), 1),
                new EnchantmentInstance(lookup.getOrThrow(Enchantments.FORTUNE), 5));

        List<ItemStack> items = RCItems.TOOLS.getEntries().stream()
                .flatMap(ro -> stacksForItem(ro.get()))
                //.sorted(new ItemSorter())
                .collect(Collectors.toCollection(ArrayList::new));
        output.acceptAll(items);
    }

    private static void genDisplayItems(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output) {
        List<ItemStack> items = RCItems.ITEMS.getEntries().stream()
                .flatMap(ro -> stacksForItem(ro.get()))
                //.sorted(new ItemSorter())
                .collect(Collectors.toCollection(ArrayList::new));
        output.acceptAll(items);
    }

    private static void genDisplayOres(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output) {
        List<ItemStack> items = RCItems.ORES.getEntries().stream()
                .flatMap(ro -> stacksForItem(ro.get()))
                .sorted(new ItemSorter())
                .collect(Collectors.toCollection(ArrayList::new));
        output.acceptAll(items);
    }

    private static Stream<ItemStack> stacksForItem(Item item) {
        ItemStack stack = new ItemStack(item);
        switch (item) {
            case CreativeTabStackProvider provider -> {
                return provider.getStacksForItem();
            }
            case BlockItem bi when bi.getBlock() instanceof CreativeTabStackProvider provider -> {
                return provider.getStacksForItem();
            }
            default -> {
                return Stream.of(stack);
            }
        }
    }

    private static void generateGearWithEnchants(CreativeModeTab.Output output, ItemLike item, EnchantmentInstance... instances) {
        ItemStack stack = new ItemStack(item);
        if (RCConfig.defaultItemEnchants) {
            for (EnchantmentInstance enchant : instances) {
                stack.enchant(enchant.enchantment, enchant.level);
            }
        }
        output.accept(stack);
    }

    private static class ItemSorter implements Comparator<ItemStack> {
        @Override
        public int compare(ItemStack s1, ItemStack s2) {
            for (Class<?> cls : List.of(BlockItem.class, BedrockArmorItem.class, HSLASteelArmorItem.class)) {
                if (cls.isAssignableFrom(s1.getItem().getClass()) && !cls.isAssignableFrom(s2.getItem().getClass())) {
                    return -1;
                } else if (cls.isAssignableFrom(s2.getItem().getClass()) && !cls.isAssignableFrom(s1.getItem().getClass())) {
                    return 1;
                }
            }
            return s1.getDisplayName().getString().compareTo(s2.getDisplayName().getString());
        }
    }
}
