package xyz.dahofa.rotarycraft.common.item.gear.bedrock;

import net.minecraft.world.item.PickaxeItem;
import xyz.dahofa.rotarycraft.common.registry.ModItemTiers;
import xyz.dahofa.rotarycraft.common.registry.ModItems;

public class BedrockPickaxeItem extends PickaxeItem {
    public BedrockPickaxeItem() {
        super(ModItemTiers.BEDROCK_TIER,
                ModItems.defaultProps());
    }
}
