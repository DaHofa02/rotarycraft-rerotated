package xyz.dahofa.rotarycraft.common.item.gear.bedrock;

import net.minecraft.world.item.HoeItem;
import xyz.dahofa.rotarycraft.common.registry.ModItemTiers;
import xyz.dahofa.rotarycraft.common.registry.ModItems;

public class BedrockHoeItem extends HoeItem {
    public BedrockHoeItem() {
        super(ModItemTiers.BEDROCK_TIER,
                ModItems.defaultProps());
    }
}
