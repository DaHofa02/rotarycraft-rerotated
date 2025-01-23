package xyz.dahofa.rotarycraft.common.item.gear.hslasteel;

import net.minecraft.world.item.ArmorItem;
import xyz.dahofa.rotarycraft.common.registry.RCArmorMaterials;
import xyz.dahofa.rotarycraft.common.registry.RCItems;

public class HSLASteelArmorItem extends ArmorItem {
    public HSLASteelArmorItem(ArmorItem.Type type) {
        super(RCArmorMaterials.HSLA_STEEL.getDelegate(), type,
                RCItems.hslaSteelToolProps());
    }
}
