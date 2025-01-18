package xyz.dahofa.rotarycraft.api;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;
import xyz.dahofa.rotarycraft.api.lib.Names;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

public final class RotaryCraftRegistry
{
    public static final String MOD_ID = "rotarycraft";

    /**
     * Get a resource location with the domain of PneumaticCraft: Repressurized's mod ID.
     *
     * @param path the path
     * @return a mod-specific ResourceLocation for the given path
     */
    public static ResourceLocation RL(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
