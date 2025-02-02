package xyz.dahofa.rotarycraft.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredRegister;
import xyz.dahofa.rotarycraft.api.lib.Names;
import xyz.dahofa.rotarycraft.common.registry.RCItems;

public class RCItemModelProvider extends ItemModelProvider {
    public RCItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Names.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        registerItems(RCItems.ITEMS);
        registerItems(RCItems.TOOLS);
        registerItems(RCItems.ORES);
        registerItems(RCItems.ENCHANTED_TOOLS);
    }

    private void registerItems(DeferredRegister.Items items) {
        for (var holder: items.getEntries()) {
            Item b = holder.get();
            if (b.asItem() != Items.AIR) {
                basicItem(b);
            }
        }
    }
}
