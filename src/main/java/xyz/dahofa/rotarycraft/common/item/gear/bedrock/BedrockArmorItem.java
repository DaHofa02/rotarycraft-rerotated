package xyz.dahofa.rotarycraft.common.item.gear.bedrock;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Unbreakable;
import xyz.dahofa.rotarycraft.common.registry.RCArmorMaterials;
import xyz.dahofa.rotarycraft.common.registry.RCItems;

public class BedrockArmorItem extends ArmorItem {
    public BedrockArmorItem(ArmorItem.Type type) {
        super(RCArmorMaterials.BEDROCK.getDelegate(), type,
                RCItems.toolProps().rarity(Rarity.EPIC).component(DataComponents.UNBREAKABLE, new Unbreakable(true)));
    }
}
