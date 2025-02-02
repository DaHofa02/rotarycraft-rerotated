package xyz.dahofa.rotarycraft.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import xyz.dahofa.rotarycraft.api.lib.Names;
import xyz.dahofa.rotarycraft.common.registry.RCBlocks;

public class RCBlockStateProvider extends BlockStateProvider {
    public RCBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Names.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        registerBlocks(RCBlocks.BLOCKS);
    }

    private void registerBlocks(DeferredRegister.Blocks blocks) {
        for (var holder: blocks.getEntries()) {
            Block b = holder.get();
            if (b.asItem() != Items.AIR) {
                blockWithItem(b);
            }
        }
    }

    private void blockWithItem(Block block) {
        simpleBlockWithItem(block, cubeAll(block));
    }
}
