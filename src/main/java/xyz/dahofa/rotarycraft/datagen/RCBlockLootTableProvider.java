package xyz.dahofa.rotarycraft.datagen;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import xyz.dahofa.rotarycraft.common.block.CanolaSeedBlock;
import xyz.dahofa.rotarycraft.common.registry.RCBlocks;
import xyz.dahofa.rotarycraft.common.registry.RCItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RCBlockLootTableProvider extends BlockLootSubProvider {
    protected RCBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        for (var holder : RCBlocks.BLOCKS.getEntries()) {
            Block b = holder.get();
            /*if (b instanceof PneumaticCraftEntityBlock && BuiltInRegistries.ITEM.containsKey(holder.getId())) {
                addStandardSerializedDrop(b, holder.getId());
            } else
            if (b instanceof SlabBlock) {
                add(b, this::createSlabItemTable);
            } else */
            if (b instanceof CanolaSeedBlock) {
                LootItemCondition.Builder lootItemConditionBuilder = LootItemBlockStatePropertyCondition.hasBlockStateProperties(b)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CanolaSeedBlock.AGE, CanolaSeedBlock.MAX_AGE));

                this.add(b, this.createCropDrops(b, RCItems.CANOLA_SEEDS.get(), RCItems.CANOLA_SEEDS.asItem(),
                        lootItemConditionBuilder, 1, 13));
            } else if (b.asItem() != Items.AIR) {
                dropSelf(b);
            }
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        List<Block> l = new ArrayList<>();
        for (var holder : RCBlocks.BLOCKS.getEntries()) {
            if (BuiltInRegistries.ITEM.containsKey(holder.getId())) {
                l.add(holder.get());
            }
        }
        return l;
    }

    protected LootTable.Builder createCropDrops(Block cropBlock, Item grownCropItem, Item seedsItem, LootItemCondition.Builder dropGrownCropCondition, float dropChance, int dropAmount) {
        HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return (LootTable.Builder) this.applyExplosionDecay(
                cropBlock, LootTable.lootTable().withPool(
                                LootPool.lootPool().add(
                                        ((LootPoolSingletonContainer.Builder) LootItem.lootTableItem(grownCropItem)
                                                .when(dropGrownCropCondition))
                                                .otherwise(LootItem.lootTableItem(seedsItem))))
                        .withPool(LootPool.lootPool().when(dropGrownCropCondition)
                                .add(LootItem.lootTableItem(seedsItem)
                                        .apply(ApplyBonusCount.addBonusBinomialDistributionCount(registrylookup.getOrThrow(Enchantments.FORTUNE), dropChance, dropAmount)))));
    }
}
