package xyz.dahofa.rotarycraft.api.data;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import xyz.dahofa.rotarycraft.api.lib.Names;

public class RotaryCraftTags {
    public static class Items extends RotaryCraftTags {
        public static final TagKey<Item> INGOTS_TIN = commonTag("ingots/tin");
        public static final TagKey<Item> INGOTS_SPRING_STEEL = commonTag("ingots/spring_steel");
        public static final TagKey<Item> INGOTS_HSLA_STEEL = commonTag("ingots/hsla_steel");
        public static final TagKey<Item> INGOTS_TUNGSTEN_ALLOY = commonTag("ingots/tungsten_alloy");
        public static final TagKey<Item> INGOTS_BEDROCK_ALLOY = commonTag("ingots/bedrock_alloy");

        public static final TagKey<Item> RODS_HSLA_STEEL = commonTag("rods/hsla_steel");
        public static final TagKey<Item> RODS_STONE = commonTag("rods/stone");
        public static final TagKey<Item> RODS_DIAMOND = commonTag("rods/diamond");
        public static final TagKey<Item> RODS_TUNGSTEN_ALLOY = commonTag("rods/tungsten_alloy");
        public static final TagKey<Item> RODS_BEDROCK_ALLOY = commonTag("rods/bedrock_alloy");

        public static final TagKey<Item> DUSTS_ALUMINUM = commonTag("dusts/aluminum");

        public static final TagKey<Item> GEAR_HSLA_STEEL = commonTag("gear/hsla_steel");
        public static final TagKey<Item> GEAR_WOODEN = commonTag("gear/wooden");
        public static final TagKey<Item> GEAR_STONE = commonTag("gear/stone");
        public static final TagKey<Item> GEAR_DIAMOND = commonTag("gear/diamond");
        public static final TagKey<Item> GEAR_TUNGSTEN_ALLOY = commonTag("gear/tungsten_alloy");
        public static final TagKey<Item> GEAR_BEDROCK_ALLOY = commonTag("gear/bedrock_alloy");

        static TagKey<Item> tag(String modid, String name) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(modid, name));
        }

        static TagKey<Item> modTag(String name) {
            return tag(Names.MOD_ID, name);
        }

        static TagKey<Item> neoforgeTag(String name) {
            return tag("neoforge", name);
        }

        static TagKey<Item> commonTag(String name) {
            return tag("c", name);
        }
    }
}
