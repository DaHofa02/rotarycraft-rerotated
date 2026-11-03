/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

/**
 * Original: ItemHazmatSuit (armour material "RCHazmat": no durability, no armour points; as ISpecialArmor it absorbs no damage) - a
 * full suit shields against low, moderate and high level radiation. Armour textures haz_1 / haz_2 of the original.
 */
public class HazmatSuitItem extends ArmorItem {

	public HazmatSuitItem(Holder<ArmorMaterial> material, Type type, Properties props) {
		super(material, type, props.stacksTo(1));
	}

	public static boolean isHazmat(ItemStack is) {
		return is.getItem() instanceof HazmatSuitItem;
	}
}
