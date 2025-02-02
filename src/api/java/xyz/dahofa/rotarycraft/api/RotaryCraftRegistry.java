package xyz.dahofa.rotarycraft.api;

import net.minecraft.resources.ResourceLocation;
import xyz.dahofa.rotarycraft.api.lib.Names;

public final class RotaryCraftRegistry
{
    /**
     * Get a resource location with the domain of RotaryCraft: Rerotated's mod ID.
     *
     * @param path the path
     * @return a mod-specific ResourceLocation for the given path
     */
    public static ResourceLocation RL(String path) {
        return ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, path);
    }
}
