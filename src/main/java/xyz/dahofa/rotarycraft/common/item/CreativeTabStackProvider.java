package xyz.dahofa.rotarycraft.common.item;

import net.minecraft.world.item.ItemStack;

import java.util.stream.Stream;

public interface CreativeTabStackProvider {
    Stream<ItemStack> getStacksForItem();
}
