package xyz.dahofa.rotarycraft.common.item.gear.bedrock;

import net.minecraft.world.item.*;
import xyz.dahofa.rotarycraft.common.registry.RCItemTiers;
import xyz.dahofa.rotarycraft.common.registry.RCItems;

import java.util.function.Supplier;

public class BedrockItemConfig {
    public static final Supplier<SwordItem> BEDROCK_SWORD_ITEM_SUPPLIER = () -> new SwordItem(
            RCItemTiers.BEDROCK_TIER,
            RCItems.bedrockToolProps()
                    .attributes(
                            SwordItem.createAttributes(
                                    RCItemTiers.BEDROCK_TIER,
                                    3f,
                                    -2.4f
                            )

                    )
    );

    public static final Supplier<PickaxeItem> BEDROCK_PICKAXE_ITEM_SUPPLIER = () -> new PickaxeItem(
            RCItemTiers.BEDROCK_TIER,
            RCItems.bedrockToolProps()
                    .attributes(
                            PickaxeItem.createAttributes(
                                    RCItemTiers.BEDROCK_TIER,
                                    1f,
                                    -2.8f
                            )
                    )
    );

    public static final Supplier<AxeItem> BEDROCK_AXE_ITEM_SUPPLIER = () -> new AxeItem(
            RCItemTiers.BEDROCK_TIER,
            RCItems.bedrockToolProps()
                    .attributes(
                            AxeItem.createAttributes(
                                    RCItemTiers.BEDROCK_TIER,
                                    4f,
                                    -3f
                            )
                    )
    );

    public static final Supplier<ShovelItem> BEDROCK_SHOVEL_ITEM_SUPPLIER = () -> new ShovelItem(
            RCItemTiers.BEDROCK_TIER,
            RCItems.bedrockToolProps()
                    .attributes(
                            ShovelItem.createAttributes(
                                    RCItemTiers.BEDROCK_TIER,
                                    1.5f,
                                    -3f
                            )
                    )
    );

    public static final Supplier<BedrockHoeItem> BEDROCK_HOE_ITEM_SUPPLIER = () -> new BedrockHoeItem(
            RCItemTiers.BEDROCK_TIER,
            2,
            RCItems.bedrockToolProps()
                    .attributes(
                            BedrockHoeItem.createAttributes(
                                    RCItemTiers.BEDROCK_TIER,
                                    1,
                                    1f
                            )
                    )
    );

    public static final Supplier<ShearsItem> BEDROCK_SHEARS_ITEM_SUPPLIER = () -> new ShearsItem(
            RCItems.bedrockToolProps()
    );
}
