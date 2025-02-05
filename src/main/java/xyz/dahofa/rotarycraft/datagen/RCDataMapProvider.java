package xyz.dahofa.rotarycraft.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import xyz.dahofa.rotarycraft.common.registry.RCItems;

import java.util.concurrent.CompletableFuture;

import static net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps.COMPOSTABLES;

public class RCDataMapProvider extends DataMapProvider {
    protected RCDataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void gather() {
        builder(COMPOSTABLES).add(RCItems.CANOLA_SEEDS, new Compostable(0.25f, true), false);
    }
}
