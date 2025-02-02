package xyz.dahofa.rotarycraft.api.util;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static xyz.dahofa.rotarycraft.api.util.RotaryCraftUtils.xlate;

public class CreativeTabHelper {
    private static CreativeModeTab buildTab(String name, DeferredItem<Item> icon) {
        return CreativeModeTab.builder()
                .title(xlate(name))
                .icon(() -> new ItemStack(icon.get()))
                .displayItems(CreativeTabHelper::genDisplay)
                .build();
    }

    private static void genDisplay(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output) {
        /*List<ItemStack> items = RCItems.ITEMS.getEntries().stream()
                .flatMap(ro -> stacksForItem(ro.get()))
                //.sorted(new ItemSorter())
                .collect(Collectors.toCollection(ArrayList::new));
        output.acceptAll(items);*/
    }

    /*private static Stream<ItemStack> stacksForItem(Item item) {
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
    }*/
}
