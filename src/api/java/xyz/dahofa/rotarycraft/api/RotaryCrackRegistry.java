package xyz.dahofa.rotarycraft.api;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModLoadingContext;

public class RotaryCrackRegistry {
    public static final String MOD_ID = "rotarycraft";

    private static IRotaryCraftInterface instance;

    /**
     * Get an instance of the top-level API.
     *
     * @return the top-level API
     */
    public static IRotaryCraftInterface getInstance() {
        return instance;
    }

    /**
     * Do not call this method yourself.  For PneumaticCraft internal usage only!
     *
     * @param inter reference to the API interface object
     */
    public static void init(IRotaryCraftInterface inter) {
        if (instance == null && ModLoadingContext.get().getActiveContainer().getModId().equals(MOD_ID))
            instance = inter;//only allow initialization once; by PneumaticCraft
        else throw new IllegalStateException("Only rotarycraft is allowed to call this method!");
    }

    /**
     * Get a resource location with the domain of PneumaticCraft: Repressurized's mod ID.
     *
     * @param path the path
     * @return a mod-specific ResourceLocation for the given path
     */
    public static ResourceLocation RL(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    /**
     * Provides top-level access to the various registries and helpers in the API.
     * Retrieve an instance of this via {@link PneumaticRegistry#getInstance()}
     */
    public interface IRotaryCraftInterface {

    }
}
