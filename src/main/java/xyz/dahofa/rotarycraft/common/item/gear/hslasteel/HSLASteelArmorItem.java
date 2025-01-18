package xyz.dahofa.rotarycraft.common.item.gear.hslasteel;

import xyz.dahofa.rotarycraft.common.registry.ModArmorMaterials;
import xyz.dahofa.rotarycraft.common.registry.ModItems;
import net.minecraft.world.item.ArmorItem;

public class HSLASteelArmorItem extends ArmorItem {
    public HSLASteelArmorItem(ArmorItem.Type type) {
        super(ModArmorMaterials.HSLA_STEEL.getDelegate(), type,
                ModItems.defaultProps().durability(type.getDurability(24)));
    }
}
