package xyz.dahofa.rotarycraft.common.item.gear.hslasteel;

import net.minecraft.world.item.SwordItem;
import xyz.dahofa.rotarycraft.common.registry.ModItemTiers;
import xyz.dahofa.rotarycraft.common.registry.ModItems;

public class HSLASteelSwordItem extends SwordItem {
    public HSLASteelSwordItem() {
        super(ModItemTiers.BEDROCK_TIER,
                ModItems.defaultProps());
    }
}
