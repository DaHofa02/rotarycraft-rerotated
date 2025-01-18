package xyz.dahofa.rotarycraft.common.item.gear.bedrock;

import net.minecraft.world.item.SwordItem;
import xyz.dahofa.rotarycraft.common.registry.ModItemTiers;
import xyz.dahofa.rotarycraft.common.registry.ModItems;

public class BedrockSwordItem extends SwordItem {
    public BedrockSwordItem() {
        super(ModItemTiers.BEDROCK_TIER,
                ModItems.defaultProps());
    }
}
