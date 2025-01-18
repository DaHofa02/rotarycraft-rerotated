package xyz.dahofa.rotarycraft.common.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class RotaryCraftUtils {
    public static MutableComponent xlate(String key, Object... args) {
        return Component.translatable(key, args);
    }
}
