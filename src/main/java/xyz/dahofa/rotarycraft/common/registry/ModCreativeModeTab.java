package xyz.dahofa.rotarycraft.common.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import xyz.dahofa.rotarycraft.api.lib.Names;
import xyz.dahofa.rotarycraft.common.item.gear.bedrock.BedrockArmorItem;
import xyz.dahofa.rotarycraft.common.item.CreativeTabStackProvider;
import xyz.dahofa.rotarycraft.common.item.gear.hslasteel.HSLASteelArmorItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static xyz.dahofa.rotarycraft.common.util.RotaryCraftUtils.xlate;

public class ModCreativeModeTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Names.MOD_ID);

    public static final Supplier<CreativeModeTab> TOOLS = TABS.register("tools", ModCreativeModeTab::buildToolsTab);
    public static final Supplier<CreativeModeTab> ITEMS = TABS.register("items", ModCreativeModeTab::buildItemsTab);

    private static CreativeModeTab buildToolsTab() {
        return CreativeModeTab.builder()
                .title(xlate("itemGroup.rotarycraft.tools"))
                .icon(() -> new ItemStack(ModItems.SCREWDRIVER.get()))
                .displayItems(ModCreativeModeTab::genDisplayTools)
                .build();
    }

    private static CreativeModeTab buildItemsTab() {
        return CreativeModeTab.builder()
                .title(xlate("itemGroup.rotarycraft.items"))
                .icon(() -> new ItemStack(ModItems.HSLA_STEEL_INGOT.get()))
                .displayItems(ModCreativeModeTab::genDisplayItems)
                .build();
    }

    private static void genDisplayTools(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output) {
        List<ItemStack> items = ModItems.TOOLS.getEntries().stream()
                .flatMap(ro -> stacksForItem(ro.get()))
                .sorted(new ItemSorter())
                .collect(Collectors.toCollection(ArrayList::new));
        output.acceptAll(items);
    }

    private static void genDisplayItems(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output) {
        List<ItemStack> items = ModItems.ITEMS.getEntries().stream()
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
