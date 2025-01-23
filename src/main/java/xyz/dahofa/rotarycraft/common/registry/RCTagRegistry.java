package xyz.dahofa.rotarycraft.common.registry;

import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import xyz.dahofa.rotarycraft.api.data.RotaryCraftTagHelper;


public class RCTagRegistry {
    public static final TagKey<Item> GEARS = ItemTags.create(RotaryCraftTagHelper.forge("gears"));
}
