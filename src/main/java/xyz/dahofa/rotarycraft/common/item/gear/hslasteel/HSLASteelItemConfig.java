package xyz.dahofa.rotarycraft.common.item.gear.hslasteel;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Unbreakable;
import xyz.dahofa.rotarycraft.common.registry.RCItemTiers;
import xyz.dahofa.rotarycraft.common.registry.RCItems;

import java.util.function.Supplier;

public class HSLASteelItemConfig {
    public static final Supplier<SwordItem> HSLA_STEEL_SWORD_ITEM_SUPPLIER = () -> new SwordItem(
            RCItemTiers.HSLA_STEEL_TIER,
            RCItems.toolProps()
                    .attributes(
                            SwordItem.createAttributes(
                                    RCItemTiers.HSLA_STEEL_TIER,
                                    3f,
                                    -2.4f
                            )

                    )
                    .rarity(Rarity.RARE)
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                    .fireResistant()
    );

    public static final Supplier<PickaxeItem> HSLA_STEEL_PICKAXE_ITEM_SUPPLIER = () -> new PickaxeItem(
            RCItemTiers.HSLA_STEEL_TIER,
            RCItems.toolProps()
                    .attributes(
                            PickaxeItem.createAttributes(
                                    RCItemTiers.HSLA_STEEL_TIER,
                                    1f,
                                    -2.8f
                            )
                    )
                    .rarity(Rarity.RARE)
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                    .fireResistant()
    );

    public static final Supplier<AxeItem> HSLA_STEEL_AXE_ITEM_SUPPLIER = () -> new AxeItem(
            RCItemTiers.HSLA_STEEL_TIER,
            RCItems.toolProps()
                    .attributes(
                            AxeItem.createAttributes(
                                    RCItemTiers.HSLA_STEEL_TIER,
                                    4f,
                                    -3f
                            )
                    )
                    .rarity(Rarity.RARE)
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                    .fireResistant()
    );

    public static final Supplier<ShovelItem> HSLA_STEEL_SHOVEL_ITEM_SUPPLIER = () -> new ShovelItem(
            RCItemTiers.HSLA_STEEL_TIER,
            RCItems.toolProps()
                    .attributes(
                            ShovelItem.createAttributes(
                                    RCItemTiers.HSLA_STEEL_TIER,
                                    1.5f,
                                    -3f
                            )
                    )
                    .rarity(Rarity.RARE)
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                    .fireResistant()
    );

    public static final Supplier<HoeItem> HSLA_STEEL_HOE_ITEM_SUPPLIER = () -> new HoeItem(
            RCItemTiers.HSLA_STEEL_TIER,
            RCItems.toolProps()
                    .attributes(
                            HoeItem.createAttributes(
                                    RCItemTiers.HSLA_STEEL_TIER,
                                    1,
                                    -1f
                            )
                    )
                    .rarity(Rarity.RARE)
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                    .fireResistant()
    );

    public static final Supplier<ShearsItem> HSLA_STEEL_SHEARS_ITEM_SUPPLIER = () -> new ShearsItem(
            RCItems.toolProps()
                    .rarity(Rarity.RARE)
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                    .fireResistant()
    );
}
