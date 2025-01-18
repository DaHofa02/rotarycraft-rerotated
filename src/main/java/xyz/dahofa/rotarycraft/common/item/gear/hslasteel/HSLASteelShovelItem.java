package xyz.dahofa.rotarycraft.common.item.gear.hslasteel;

import net.minecraft.world.item.ShovelItem;
import xyz.dahofa.rotarycraft.common.registry.ModItemTiers;
import xyz.dahofa.rotarycraft.common.registry.ModItems;

public class HSLASteelShovelItem extends ShovelItem {
    public HSLASteelShovelItem() {
        super(ModItemTiers.BEDROCK_TIER,
                ModItems.defaultProps());
    }
}
