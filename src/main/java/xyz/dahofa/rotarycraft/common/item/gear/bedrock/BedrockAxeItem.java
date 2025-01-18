package xyz.dahofa.rotarycraft.common.item.gear.bedrock;

import net.minecraft.world.item.AxeItem;
import xyz.dahofa.rotarycraft.common.registry.ModItemTiers;
import xyz.dahofa.rotarycraft.common.registry.ModItems;

public class BedrockAxeItem extends AxeItem {
    public BedrockAxeItem() {
        super(ModItemTiers.BEDROCK_TIER,
                ModItems.defaultProps());
    }
}
