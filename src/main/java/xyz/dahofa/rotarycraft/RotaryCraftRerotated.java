package xyz.dahofa.rotarycraft;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import xyz.dahofa.rotarycraft.api.lib.Names;
import xyz.dahofa.rotarycraft.common.registry.RCArmorMaterials;
import xyz.dahofa.rotarycraft.common.registry.RCBlocks;
import xyz.dahofa.rotarycraft.common.registry.RCCreativeModeTab;
import xyz.dahofa.rotarycraft.common.registry.RCItems;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Names.MOD_ID)
public class RotaryCraftRerotated {
    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public RotaryCraftRerotated(ModContainer container, IEventBus modBus) {
        IEventBus forgeBus = NeoForge.EVENT_BUS;
        registerAllDeferredRegistryObjects(modBus);
    }

    private void registerAllDeferredRegistryObjects(IEventBus modBus) {
        RCItems.BLOCKS.register(modBus);
        RCItems.ITEMS.register(modBus);
        RCItems.TOOLS.register(modBus);
        RCItems.ORES.register(modBus);
        RCItems.ENCHANTED_TOOLS.register(modBus);

        RCBlocks.BLOCKS.register(modBus);

        RCArmorMaterials.ARMOR_MATERIALS.register(modBus);

        RCCreativeModeTab.TABS.register(modBus);
    }

}
