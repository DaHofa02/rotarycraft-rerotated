package xyz.dahofa.rotarycraft;


import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(RotaryCraft.MODID)
public class RotaryCraft
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "rotarycraft";

    public RotaryCraft(ModContainer container, IEventBus modBus) {
        IEventBus forgeBus = NeoForge.EVENT_BUS;


    }
}
