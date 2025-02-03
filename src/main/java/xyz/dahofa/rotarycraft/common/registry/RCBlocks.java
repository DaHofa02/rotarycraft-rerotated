package xyz.dahofa.rotarycraft.common.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import xyz.dahofa.rotarycraft.api.lib.Names;
import xyz.dahofa.rotarycraft.common.block.Grinder;

import java.util.function.Function;
import java.util.function.Supplier;

public class RCBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Names.MOD_ID);
    public static final DeferredRegister<Item> BLOCK_ITEMS = RCItems.BLOCKS;

    public static final DeferredBlock<Block> HSLA_STEEL_BLOCK = registerBlock("hsla_steel_block", Block::new, defaultProps());
    public static final DeferredBlock<Block> BEDROCK_ALLOY_BLOCK = registerBlock("bedrock_alloy_block", Block::new, defaultProps());
    public static final DeferredBlock<Grinder> GRINDER = registerBlock("grinder", Grinder::new, defaultProps());

    public static Block.Properties defaultProps() {
        return Block.Properties.of()
                .requiresCorrectToolForDrops()
                .mapColor(MapColor.METAL)
                .strength(5F, 6F)
                .sound(SoundType.METAL);
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, ? extends T> sup, BlockBehaviour.Properties props) {
        return registerBlock(name, sup, props, RCBlocks::itemDefault);
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, ? extends T> sup, BlockBehaviour.Properties props, Function<DeferredBlock<T>, Supplier<? extends Item>> itemCreator) {
        DeferredBlock<T> ret = registerNoItem(name, sup, props);
        BLOCK_ITEMS.register(name, itemCreator.apply(ret));
        return ret;
    }

    private static <T extends Block> DeferredBlock<T> registerNoItem(String name, Function<BlockBehaviour.Properties, ? extends T> sup, BlockBehaviour.Properties props) {
        return BLOCKS.registerBlock(name, sup, props);
    }

    private static Supplier<BlockItem> itemDefault(final DeferredBlock<? extends Block> blockSupplier) {
        return item(blockSupplier);
    }

    private static Supplier<BlockItem> item(final DeferredBlock<? extends Block> blockSupplier) {
        return () -> new BlockItem(blockSupplier.get(), RCItems.defaultProps());
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
