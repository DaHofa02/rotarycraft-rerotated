package xyz.dahofa.rotarycraft.common.item.gear.bedrock;

import net.minecraft.world.item.ArmorItem;
import xyz.dahofa.rotarycraft.common.registry.RCArmorMaterials;
import xyz.dahofa.rotarycraft.common.registry.RCItems;

public class BedrockArmorItem extends ArmorItem {
    public BedrockArmorItem(ArmorItem.Type type) {
        super(RCArmorMaterials.BEDROCK.getDelegate(), type,
                RCItems.bedrockToolProps());
    }
}
