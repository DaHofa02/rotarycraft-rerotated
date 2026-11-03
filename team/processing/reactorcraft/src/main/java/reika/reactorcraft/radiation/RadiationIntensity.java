/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.radiation;

import java.util.function.Predicate;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;

import reika.reactorcraft.content.item.HazmatSuitItem;

/**
 * Original: RadiationEffects.RadiationIntensity - the effect duration per level and the shielding: background always, low level a
 * full suit of hazmat, bedrock (not ported yet) or dense armour (gold, or a chestplate of at least 6 armour), moderate a full suit of
 * hazmat (or bedrock), high level only a full hazmat suit, lethal nothing.
 */
public enum RadiationIntensity {
	BACKGROUND(0),
	LOWLEVEL(100),
	MODERATE(1200),
	HIGHLEVEL(6000),
	LETHAL(36000);

	public final int potionDuration;

	public static final RadiationIntensity[] radiationList = values();

	RadiationIntensity(int t) {
		potionDuration = t;
	}

	public boolean causesHarm() {
		return this != BACKGROUND;
	}

	public boolean isAtLeast(RadiationIntensity ri) {
		return this.ordinal() >= ri.ordinal();
	}

	public boolean hasSufficientShielding(LivingEntity e) {
		return switch (this) {
			case BACKGROUND -> true;
			case LETHAL -> false;
			case HIGHLEVEL -> wearsFullSuitOf(e, HazmatSuitItem::isHazmat);
			case MODERATE -> wearsFullSuitOf(e, HazmatSuitItem::isHazmat);
			case LOWLEVEL -> wearsFullSuitOf(e, is -> HazmatSuitItem.isHazmat(is) || isDenseArmor(is));
		};
	}

	/** Original ReikaEntityHelper.isEntityWearingFullSuitOf: all four armour slots match. */
	private static boolean wearsFullSuitOf(LivingEntity e, Predicate<ItemStack> f) {
		for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			ItemStack is = e.getItemBySlot(s);
			if (is.isEmpty() || !f.test(is))
				return false;
		}
		return true;
	}

	/** Original ReikaItemHelper.isDenseArmor: gold, or a material whose chestplate gives at least 6 armour. */
	private static boolean isDenseArmor(ItemStack is) {
		if (is.getItem() instanceof ArmorItem a)
			return a.getMaterial().is(ArmorMaterials.GOLD) || a.getMaterial().value().getDefense(ArmorItem.Type.CHESTPLATE) >= 6;
		return false;
	}
}
