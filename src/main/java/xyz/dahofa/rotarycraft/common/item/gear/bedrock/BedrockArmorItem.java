package xyz.dahofa.rotarycraft.common.item.gear.bedrock;

import xyz.dahofa.rotarycraft.common.registry.ModArmorMaterials;
import xyz.dahofa.rotarycraft.common.registry.ModItems;
import net.minecraft.world.item.ArmorItem;

public class BedrockArmorItem extends ArmorItem {
    public BedrockArmorItem(ArmorItem.Type type) {
        super(ModArmorMaterials.BEDROCK.getDelegate(), type,
                ModItems.defaultProps().durability(type.getDurability(24)));
    }
}
