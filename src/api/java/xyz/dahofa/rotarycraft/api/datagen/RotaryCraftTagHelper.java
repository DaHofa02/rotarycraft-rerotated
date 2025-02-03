package xyz.dahofa.rotarycraft.api.datagen;

import net.minecraft.resources.ResourceLocation;

public class RotaryCraftTagHelper {
    public static ResourceLocation forge(String path) {
        return ResourceLocation.fromNamespaceAndPath("c", path);
    }

    public static ResourceLocation ore(String path) {
        return forge("ores/" + path);
    }

    public static ResourceLocation ores_in_ground(String path) {
        return forge("ores_in_ground/" + path);
    }

    public static ResourceLocation nugget(String path) {
        return forge("nuggets/" + path);
    }

    public static ResourceLocation ingot(String path) {
        return forge("ingots/" + path);
    }

    public static ResourceLocation gem(String path) {
        return forge("gems/" + path);
    }

    public static ResourceLocation raw_materials(String path) {
        return forge("raw_materials/" + path);
    }

    public static ResourceLocation block(String path) {
        return forge("storage_blocks/" + path);
    }

    public static ResourceLocation dust(String path) {
        return forge("dusts/" + path);
    }

    public static ResourceLocation plate(String path) {
        return forge("plates/" + path);
    }

    public static ResourceLocation gear(String path) {
        return forge("gears/" + path);
    }

    public static ResourceLocation rod(String path) {
        return forge("rods/" + path);
    }

    public static ResourceLocation crystal(String path) {
        return forge("crystals/" + path);
    }

    public static ResourceLocation shard(String path) {
        return forge("shards/" + path);
    }

    public static ResourceLocation clump(String path) {
        return forge("clumps/" + path);
    }

    public static ResourceLocation dirty_dust(String path) {
        return forge("dirty_dusts/" + path);
    }

    public static ResourceLocation molten(String path) {
        return forge("molten_" + path);
    }
}
