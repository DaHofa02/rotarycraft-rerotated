/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.registry;

import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import reika.rotarycraft.RotaryCraft;

/**
 * Original: Reika.RotaryCraft.Auxiliary.MachineDamage and RotaryEventManager.armorDamageBoost. The damage types are data
 * (data/rotarycraft/damage_type); armour in the listed slots scales the damage and takes extra wear, as before.
 */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID)
public final class RotaryDamage {

	public static final ResourceKey<DamageType> HYDROKINETIC = key("hydrokinetic");
	public static final ResourceKey<DamageType> JETINGEST = key("jetingest");
	/** Original: RotaryCraft.heatDamage ("burned up", fire damage: tagged #minecraft:is_fire). */
	public static final ResourceKey<DamageType> HEAT = key("heat");
	/** Original: RotaryCraft.grind ("was ground to a pulp"), the Grinder's contact damage. */
	public static final ResourceKey<DamageType> GRIND = key("grind");
	/** Original: RotaryCraft.shock ("was electrified", bypasses armour), the Van de Graaff's strike. */
	public static final ResourceKey<DamageType> SHOCK = key("shock");
	/** Original: RotaryCraft.freezeDamage ("froze"), a broken refrigerator's cold burst. */
	public static final ResourceKey<DamageType> FREEZE = key("freeze");

	/** factor, armour damage, slots (original setArmorBlocking: 1 feet .. 4 helmet). */
	private record ArmorBlocking(float factor, int armorDamage, EquipmentSlot... slots) {}

	private static final Map<ResourceKey<DamageType>, ArmorBlocking> BLOCKING = Map.of(
			HYDROKINETIC, new ArmorBlocking(0.5F, 2, EquipmentSlot.HEAD),
			JETINGEST, new ArmorBlocking(1F, 90, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD),
			GRIND, new ArmorBlocking(0.5F, 6, EquipmentSlot.FEET));

	private RotaryDamage() {}

	private static ResourceKey<DamageType> key(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, name));
	}

	public static DamageSource source(Level world, ResourceKey<DamageType> type) {
		return new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type));
	}

	@SubscribeEvent
	public static void armorDamageBoost(LivingIncomingDamageEvent evt) {
		for (Map.Entry<ResourceKey<DamageType>, ArmorBlocking> e : BLOCKING.entrySet()) {
			if (!evt.getSource().is(e.getKey()))
				continue;
			LivingEntity ent = evt.getEntity();
			float base = evt.getAmount();
			for (EquipmentSlot slot : e.getValue().slots()) {
				ItemStack armor = ent.getItemBySlot(slot);
				if (armor.getItem() instanceof ArmorItem) {
					armor.hurtAndBreak(e.getValue().armorDamage(), ent, slot);
					base *= e.getValue().factor();
				}
			}
			evt.setAmount(base);
		}
	}
}
