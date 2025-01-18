package xyz.dahofa.rotarycraft.common.item.gear.bedrock;

import net.minecraft.world.item.ShovelItem;
import xyz.dahofa.rotarycraft.common.registry.ModItemTiers;
import xyz.dahofa.rotarycraft.common.registry.ModItems;

public class BedrockShovelItem extends ShovelItem {
    public BedrockShovelItem() {
        super(ModItemTiers.BEDROCK_TIER,
                ModItems.defaultProps());
    }
}
