/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.registry;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.world.item.Item;

import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.reactorcraft.ReactorCraft;

/** Original: ReactorItems RAW, FLUORITE and INGOTS (one item per former metadata value). */
public final class ReactorItems {

	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ReactorCraft.MOD_ID);

	/** Original ReactorStacks / ReactorNames.rawNames, in metadata order. */
	public enum Raw {
		HF, FUELDUST, DEPDUST, AMMONIUM, LIME, CALCITE, MAGNETITE, THORDUST, EMERALDDUST, WASTEDUST;

		public String getName() {
			return this.name().toLowerCase(Locale.ENGLISH);
		}
	}

	/** Original INGOTS metadata 0-3 (the ores' products: uraninite gives uranium). */
	public enum Ingot {
		URANIUM, CADMIUM, INDIUM, SILVER;

		public String getName() {
			return this.name().toLowerCase(Locale.ENGLISH);
		}
	}

	/** Original CraftingItems (ReactorItems.CRAFTING metadata order). */
	public enum Crafting {
		CANISTER, ROD, TANK, ALLOY, BACKING, MAGNETIC, MAGNETCORE, COOLANT, WIRE, SHIELD, FERROINGOT, HYSTERESIS, HYSTERESISRING, GRAPHITE, UDUST,
		FABRIC, CARBIDEFLAKES, CARBIDE, TURBCORE;

		public String getName() {
			return this.name().toLowerCase(Locale.ENGLISH);
		}
	}

	private static final Map<Crafting, DeferredItem<Item>> CRAFTING = new EnumMap<>(Crafting.class);
	private static final Map<Raw, DeferredItem<Item>> RAW = new EnumMap<>(Raw.class);
	private static final Map<FluoriteTypes, DeferredItem<Item>> FLUORITE = new EnumMap<>(FluoriteTypes.class);
	private static final Map<Ingot, DeferredItem<Item>> INGOTS = new EnumMap<>(Ingot.class);

	static {
		for (Raw r : Raw.values())
			RAW.put(r, ITEMS.registerSimpleItem(r.getName()));
		for (FluoriteTypes f : FluoriteTypes.colorList)
			FLUORITE.put(f, ITEMS.registerSimpleItem(f.getColorName()+"_fluorite"));
		for (Ingot i : Ingot.values())
			INGOTS.put(i, ITEMS.registerSimpleItem(i.getName()+"_ingot"));
		//"canister" would clash with the fluid canisters; the crafting part is the fuel canister
		for (Crafting c : Crafting.values())
			CRAFTING.put(c, ITEMS.registerSimpleItem(c == Crafting.CANISTER ? "fuel_canister" : c.getName()));
	}

	/** Original ReactorNames.canNames order (metadata) and the fluid each canister holds. */
	private static final String[][] CANISTERS = {{"uf6", "uranium_hexafluoride"}, {"hf", "hydrofluoric_acid"}, {"nh3", "ammonia"},
			{"na", "sodium"}, {"h2", "deuterium"}, {"h3", "tritium"}, {"cl", "chlorine"}, {"o", "oxygen"}, {"co2", "co2"}, {"hco2", "hot_co2"},
			{"hna", "hot_sodium"}, {"li", "lithium"}, {"lifbe", "lifbe"}, {"hlifbe", "hot_lifbe"}, {"lifbefuel", "lifbe_fuel"}};
	public static final DeferredItem<reika.reactorcraft.content.item.CanisterItem> EMPTY_CANISTER = ITEMS.register("empty_canister",
			() -> new reika.reactorcraft.content.item.CanisterItem(new Item.Properties().stacksTo(1), null));
	private static final Map<String, DeferredItem<reika.reactorcraft.content.item.CanisterItem>> FILLED = new java.util.LinkedHashMap<>();

	static {
		for (String[] c : CANISTERS)
			FILLED.put(c[1], ITEMS.register(c[0]+"_canister", () -> new reika.reactorcraft.content.item.CanisterItem(new Item.Properties().stacksTo(1)
					.craftRemainder(EMPTY_CANISTER.get()), ReactorFluids.SOURCE.get(c[1]))));
	}

	/** Original ReactorItems.BUCKET: heavy water, stack size 1, leaves a bucket in crafting, cannot be poured out. */
	public static final DeferredItem<reika.rotarycraft.content.item.FluidBucketItem> HEAVY_WATER_BUCKET = ITEMS.register("heavy_water_bucket",
			() -> new reika.rotarycraft.content.item.FluidBucketItem(ReactorFluids.get("heavy_water"), new Item.Properties().stacksTo(1).craftRemainder(net.minecraft.world.item.Items.BUCKET)));

	/** Original ReactorItems FUEL (100 depletion steps), DEPLETED and WASTE. */
	public static final DeferredItem<reika.reactorcraft.content.item.FuelItem> FUEL = ITEMS.register("fuel", () -> new reika.reactorcraft.content.item.FuelItem(new Item.Properties(), 100));
	public static final DeferredItem<Item> DEPLETED = ITEMS.registerSimpleItem("depleted");
	/** Original PLUTONIUM (100 steps) and BREEDERFUEL (20 steps). */
	public static final DeferredItem<reika.reactorcraft.content.item.PlutoniumItem> PLUTONIUM = ITEMS.register("plutonium", () -> new reika.reactorcraft.content.item.PlutoniumItem(new Item.Properties(), 100));
	/** Original HAZ armour material ("RCHazmat": 0 durability, 0 protection, enchantability 0) with the original armour textures haz_1/haz_2. */
	public static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.world.item.ArmorMaterial> ARMOR_MATERIALS =
			net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.ARMOR_MATERIAL, reika.reactorcraft.ReactorCraft.MOD_ID);
	public static final net.minecraft.core.Holder<net.minecraft.world.item.ArmorMaterial> HAZMAT = ARMOR_MATERIALS.register("hazmat", () -> new net.minecraft.world.item.ArmorMaterial(
			java.util.Map.of(net.minecraft.world.item.ArmorItem.Type.BOOTS, 0, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, 0, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, 0,
					net.minecraft.world.item.ArmorItem.Type.HELMET, 0, net.minecraft.world.item.ArmorItem.Type.BODY, 0), 0,
			net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER, () -> net.minecraft.world.item.crafting.Ingredient.EMPTY,
			java.util.List.of(new net.minecraft.world.item.ArmorMaterial.Layer(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(reika.reactorcraft.ReactorCraft.MOD_ID, "haz"))), 0, 0));
	public static final DeferredItem<reika.reactorcraft.content.item.HazmatSuitItem> HAZ_HELMET = ITEMS.register("hazmat_helmet",
			() -> new reika.reactorcraft.content.item.HazmatSuitItem(HAZMAT, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
	public static final DeferredItem<reika.reactorcraft.content.item.HazmatSuitItem> HAZ_CHEST = ITEMS.register("hazmat_chestplate",
			() -> new reika.reactorcraft.content.item.HazmatSuitItem(HAZMAT, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
	public static final DeferredItem<reika.reactorcraft.content.item.HazmatSuitItem> HAZ_LEGS = ITEMS.register("hazmat_leggings",
			() -> new reika.reactorcraft.content.item.HazmatSuitItem(HAZMAT, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
	public static final DeferredItem<reika.reactorcraft.content.item.HazmatSuitItem> HAZ_BOOTS = ITEMS.register("hazmat_boots",
			() -> new reika.reactorcraft.content.item.HazmatSuitItem(HAZMAT, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));
	/** Original GOGGLES and GEIGER (ItemReactorTool). */
	public static final DeferredItem<reika.reactorcraft.content.item.RadiationGogglesItem> GOGGLES = ITEMS.register("goggles",
			() -> new reika.reactorcraft.content.item.RadiationGogglesItem(new Item.Properties()));
	public static final DeferredItem<reika.reactorcraft.content.item.GeigerCounterItem> GEIGER = ITEMS.register("geiger",
			() -> new reika.reactorcraft.content.item.GeigerCounterItem(new Item.Properties()));
	/** Original CLEANUP (ItemRadiationCleaner) and IRONFINDER (ItemIronFinder). */
	public static final DeferredItem<reika.reactorcraft.content.item.RadiationCleanerItem> CLEANER = ITEMS.register("cleaner",
			() -> new reika.reactorcraft.content.item.RadiationCleanerItem(new Item.Properties()));
	public static final DeferredItem<reika.reactorcraft.content.item.IronFinderItem> IRON_FINDER = ITEMS.register("ironfinder",
			() -> new reika.reactorcraft.content.item.IronFinderItem(new Item.Properties()));
	/** Original REMOTE (ItemRemoteControl, CPU remote control). */
	public static final DeferredItem<reika.reactorcraft.content.item.RemoteControlItem> REMOTE = ITEMS.register("remotecpu",
			() -> new reika.reactorcraft.content.item.RemoteControlItem(new Item.Properties()));
	/** Original PELLET (HTGR fuel pebble, 25 steps) and OLDPELLET (spent pebble). */
	public static final DeferredItem<reika.reactorcraft.content.item.FuelItem> PELLET = ITEMS.register("pellet", () -> new reika.reactorcraft.content.item.FuelItem(new Item.Properties(), 25));
	public static final DeferredItem<Item> OLD_PELLET = ITEMS.registerSimpleItem("depleted_pellet");
	public static final DeferredItem<reika.reactorcraft.content.item.BreederFuelItem> BREEDER_FUEL = ITEMS.register("breeder_fuel", () -> new reika.reactorcraft.content.item.BreederFuelItem(new Item.Properties(), 20));
	public static final DeferredItem<reika.reactorcraft.content.item.NuclearWasteItem> WASTE = ITEMS.register("waste", () -> new reika.reactorcraft.content.item.NuclearWasteItem(new Item.Properties()));

	private ReactorItems() {}

	public static java.util.Collection<DeferredItem<reika.reactorcraft.content.item.CanisterItem>> filledCanisters() {
		return FILLED.values();
	}

	/** The canister that holds this fluid, or null. */
	@javax.annotation.Nullable
	public static Item canisterFor(net.minecraft.world.level.material.Fluid f) {
		for (var e : FILLED.entrySet())
			if (ReactorFluids.get(e.getKey()).isSame(f))
				return e.getValue().get();
		return null;
	}

	public static DeferredItem<Item> crafting(Crafting c) {
		return CRAFTING.get(c);
	}

	/** Original MAGNET: permanent magnets, 8 strengths (magnet_0 .. magnet_7). */
	public static final java.util.List<DeferredItem<reika.reactorcraft.content.item.MagnetItem>> MAGNETS = new java.util.ArrayList<>();

	static {
		for (int i = 0; i < 8; i++) {
			int lvl = i;
			MAGNETS.add(ITEMS.register("magnet_"+i, () -> new reika.reactorcraft.content.item.MagnetItem(new Item.Properties(), lvl)));
		}
	}

	public static DeferredItem<Item> raw(Raw r) {
		return RAW.get(r);
	}

	public static DeferredItem<Item> fluorite(FluoriteTypes f) {
		return FLUORITE.get(f);
	}

	public static DeferredItem<Item> ingot(Ingot i) {
		return INGOTS.get(i);
	}
}
