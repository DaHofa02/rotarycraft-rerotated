package xyz.dahofa.rotarycraft.api;

import net.neoforged.neoforge.common.NeoForge;
import xyz.dahofa.rotarycraft.api.RotaryCraftRerotated;
import xyz.dahofa.rotarycraft.api.lib.Names;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Names.MOD_ID)
public class RotaryCraftRerotated
{

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public RotaryCraftRerotated(ModContainer container, IEventBus modBus) {
        IEventBus forgeBus = NeoForge.EVENT_BUS;

    }

}
