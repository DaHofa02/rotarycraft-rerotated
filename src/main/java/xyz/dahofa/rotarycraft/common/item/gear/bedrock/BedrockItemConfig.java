package xyz.dahofa.rotarycraft.common.item.gear.bedrock;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Unbreakable;
import xyz.dahofa.rotarycraft.common.registry.RCItemTiers;
import xyz.dahofa.rotarycraft.common.registry.RCItems;

import java.util.function.Supplier;

public class BedrockItemConfig {
    public static final Supplier<SwordItem> BEDROCK_SWORD_ITEM_SUPPLIER = () -> new SwordItem(
            RCItemTiers.BEDROCK_TIER,
            RCItems.toolProps()
                    .attributes(
                            SwordItem.createAttributes(
                                    RCItemTiers.BEDROCK_TIER,
                                    3,
                                    -2.4f
                                    )

                    )
                    .rarity(Rarity.EPIC)
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                    .fireResistant()
    );

    public static final Supplier<PickaxeItem> BEDROCK_PICKAXE_ITEM_SUPPLIER = () -> new PickaxeItem(
            RCItemTiers.BEDROCK_TIER,
            RCItems.toolProps()
                    .attributes(
                            PickaxeItem.createAttributes(
                                    RCItemTiers.BEDROCK_TIER,
                                    1,
                                    1f
                            )
                    )
                    .rarity(Rarity.EPIC)
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                    .fireResistant()
    );

    public static final Supplier<AxeItem> BEDROCK_AXE_ITEM_SUPPLIER = () -> new AxeItem(
            RCItemTiers.BEDROCK_TIER,
            RCItems.toolProps()
                    .attributes(
                            AxeItem.createAttributes(
                                    RCItemTiers.BEDROCK_TIER,
                                    1,
                                    1f
                            )
                    )
                    .rarity(Rarity.EPIC)
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                    .fireResistant()
    );

    public static final Supplier<ShovelItem> BEDROCK_SHOVEL_ITEM_SUPPLIER = () -> new ShovelItem(
            RCItemTiers.BEDROCK_TIER,
            RCItems.toolProps()
                    .attributes(
                            ShovelItem.createAttributes(
                                    RCItemTiers.BEDROCK_TIER,
                                    1,
                                    1f
                            )
                    )
                    .rarity(Rarity.EPIC)
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                    .fireResistant()
    );

    public static final Supplier<HoeItem> BEDROCK_HOE_ITEM_SUPPLIER = () -> new HoeItem(
            RCItemTiers.BEDROCK_TIER,
            RCItems.toolProps()
                    .attributes(
                            HoeItem.createAttributes(
                                    RCItemTiers.BEDROCK_TIER,
                                    1,
                                    1f
                            )
                    )
                    .rarity(Rarity.EPIC)
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                    .fireResistant()
    );

    public static final Supplier<ShearsItem> BEDROCK_SHEARS_ITEM_SUPPLIER = () -> new ShearsItem(
            RCItems.toolProps()
                    .rarity(Rarity.EPIC)
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                    .fireResistant()
    );
}
