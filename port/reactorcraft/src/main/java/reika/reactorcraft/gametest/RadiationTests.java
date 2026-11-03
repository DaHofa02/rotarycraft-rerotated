/*******************************************************************************
 * Tests for the ported ReactorCraft radiation (original RadiationEffects, EntityNuclearWaste, ItemNuclearWaste, PotionRadiation).
 * Private NeoForge 1.21.1 port of ReactorCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.entity.NuclearWasteEntity;
import reika.reactorcraft.content.item.FuelItem;
import reika.reactorcraft.radiation.Isotopes;
import reika.reactorcraft.radiation.RadiationIntensity;
import reika.reactorcraft.registry.ReactorComponents;
import reika.reactorcraft.registry.ReactorEffects;
import reika.reactorcraft.registry.ReactorItems;

@GameTestHolder(ReactorCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class RadiationTests {

	private static final String PLATFORM = "platform_9x4x9";

	/** Dropped nuclear waste becomes the indestructible waste entity and gives animals within 6 blocks radiation sickness. */
	@GameTest(template = PLATFORM, timeoutTicks = 100)
	public static void droppedWasteIrradiates(GameTestHelper h) {
		Pig pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(6, 1, 4));
		Vec3 v = h.absoluteVec(new Vec3(2.5, 1.2, 4.5));
		h.getLevel().addFreshEntity(new ItemEntity(h.getLevel(), v.x, v.y, v.z, NuclearWasteItemStack()));
		h.succeedWhen(() -> {
			h.assertEntityPresent(reika.reactorcraft.registry.ReactorEntities.NUCLEAR_WASTE.get());
			h.assertTrue(pig.hasEffect(ReactorEffects.RADIATION), "pig not irradiated");
		});
	}

	private static ItemStack NuclearWasteItemStack() {
		return reika.reactorcraft.content.item.NuclearWasteItem.of(Isotopes.Cs137);
	}

	/** The waste entity ignores damage (original: cannot be destroyed). */
	@GameTest(template = PLATFORM)
	public static void wasteEntityIndestructible(GameTestHelper h) {
		Vec3 v = h.absoluteVec(new Vec3(4.5, 1.2, 4.5));
		NuclearWasteEntity e = new NuclearWasteEntity(h.getLevel(), v.x, v.y, v.z, NuclearWasteItemStack());
		h.getLevel().addFreshEntity(e);
		e.hurt(h.getLevel().damageSources().lava(), 1000);
		h.assertTrue(e.isAlive() && e.fireImmune(), "waste entity was destroyed");
		h.succeed();
	}

	/** Fuel depletes in 100 steps; the name and the bar follow the depletion component. */
	@GameTest(template = PLATFORM)
	public static void fuelDepletionDisplay(GameTestHelper h) {
		ItemStack is = new ItemStack(ReactorItems.FUEL.get());
		h.assertTrue(FuelItem.getDepletion(is) == 0 && is.getHoverName().getString().contains("Fresh"), "fresh: "+is.getHoverName().getString());
		is.set(ReactorComponents.DEPLETION.get(), 50);
		h.assertTrue(is.getHoverName().getString().contains("50% Depleted"), "half: "+is.getHoverName().getString());
		h.assertTrue(is.isBarVisible(), "no depletion bar");
		h.succeed();
	}

	private static net.minecraft.world.entity.player.Player suited(GameTestHelper h, net.minecraft.world.item.Item helmet, net.minecraft.world.item.Item chest,
			net.minecraft.world.item.Item legs, net.minecraft.world.item.Item boots) {
		var p = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(helmet));
		p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(chest));
		p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(legs));
		p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(boots));
		return p;
	}

	/** Original shielding: a full hazmat suit stops low, moderate and high level radiation, not lethal; dense armour (iron) only low level. */
	@GameTest(template = PLATFORM)
	public static void hazmatShielding(GameTestHelper h) {
		var haz = suited(h, ReactorItems.HAZ_HELMET.get(), ReactorItems.HAZ_CHEST.get(), ReactorItems.HAZ_LEGS.get(), ReactorItems.HAZ_BOOTS.get());
		var iron = suited(h, net.minecraft.world.item.Items.IRON_HELMET, net.minecraft.world.item.Items.IRON_CHESTPLATE, net.minecraft.world.item.Items.IRON_LEGGINGS,
				net.minecraft.world.item.Items.IRON_BOOTS);
		var part = suited(h, ReactorItems.HAZ_HELMET.get(), ReactorItems.HAZ_CHEST.get(), ReactorItems.HAZ_LEGS.get(), net.minecraft.world.item.Items.AIR);
		h.assertTrue(RadiationIntensity.HIGHLEVEL.hasSufficientShielding(haz) && RadiationIntensity.MODERATE.hasSufficientShielding(haz)
				&& RadiationIntensity.LOWLEVEL.hasSufficientShielding(haz), "hazmat suit does not shield");
		h.assertTrue(!RadiationIntensity.LETHAL.hasSufficientShielding(haz), "hazmat suit shields lethal radiation");
		h.assertTrue(RadiationIntensity.LOWLEVEL.hasSufficientShielding(iron) && !RadiationIntensity.MODERATE.hasSufficientShielding(iron), "iron armour shielding");
		h.assertTrue(!RadiationIntensity.LOWLEVEL.hasSufficientShielding(part), "an incomplete suit shields");
		h.assertTrue(!reika.reactorcraft.radiation.RadiationEffects.applyEffects(haz, RadiationIntensity.HIGHLEVEL) && !haz.hasEffect(ReactorEffects.RADIATION),
				"suited player got sick");
		h.assertTrue(haz.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getMaxDamage() == 0, "hazmat suit wears out");
		h.succeed();
	}

	/** The Geiger counter uses 1 kJ with 1 in 8 per tick while held; uncharged it does nothing. */
	@GameTest(template = PLATFORM)
	public static void geigerUsesCharge(GameTestHelper h) {
		var p = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		ItemStack g = reika.reactorcraft.content.item.GeigerCounterItem.charged(ReactorItems.GEIGER.get(), 1000);
		for (int i = 0; i < 400; i++)
			g.inventoryTick(h.getLevel(), p, 0, true);
		int c = reika.reactorcraft.content.item.GeigerCounterItem.getCharge(g);
		h.assertTrue(c < 1000-20 && c > 1000-90, "charge "+c+" after 400 ticks");
		ItemStack empty = new ItemStack(ReactorItems.GEIGER.get());
		empty.inventoryTick(h.getLevel(), p, 0, true);
		h.assertTrue(reika.reactorcraft.content.item.GeigerCounterItem.getCharge(empty) == 0, "uncharged counter changed");
		h.succeed();
	}
}
