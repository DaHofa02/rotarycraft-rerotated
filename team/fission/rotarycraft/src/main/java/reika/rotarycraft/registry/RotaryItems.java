/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.registry;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.content.item.EngineUpgradeItem;
import reika.rotarycraft.content.item.MeterItem;
import reika.rotarycraft.content.item.ScrewdriverItem;
import reika.rotarycraft.registry.GearboxTypes.GearPart;

/** Original: Reika.RotaryCraft.Registry.ItemRegistry, flattened: one item per former metadata value. */
public final class RotaryItems {

	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RotaryCraft.MOD_ID);

	private static final Map<CraftingPart, DeferredItem<Item>> PARTS = new EnumMap<>(CraftingPart.class);
	private static final Map<GearboxTypes, Map<GearPart, DeferredItem<Item>>> GEAR_PARTS = new EnumMap<>(GearboxTypes.class);
	private static final Map<Flywheels, DeferredItem<Item>> FLYWHEEL_CORES = new EnumMap<>(Flywheels.class);

	static {
		for (CraftingPart p : CraftingPart.values()) {
			PARTS.put(p, ITEMS.registerSimpleItem(p.id));
		}
		for (GearboxTypes g : GearboxTypes.typeList) {
			Map<GearPart, DeferredItem<Item>> m = new EnumMap<>(GearPart.class);
			for (GearPart p : GearPart.list) {
				m.put(p, ITEMS.registerSimpleItem(g.getSerializedName()+"_"+p.getSerializedName()));
			}
			GEAR_PARTS.put(g, m);
		}
		for (Flywheels f : Flywheels.list) {
			FLYWHEEL_CORES.put(f, ITEMS.registerSimpleItem(f.getSerializedName()+"_flywheel_core"));
		}
	}

	/** Original: ItemRegistry.YEAST / ETHANOL (ItemBasic). */
	public static final DeferredItem<Item> YEAST = ITEMS.registerSimpleItem("yeast");
	public static final DeferredItem<Item> ETHANOL = ITEMS.registerSimpleItem("ethanol");

	/** Original: ItemRegistry.SCREWDRIVER / METER (Angular Transducer). */
	/** Original SPRING (wind spring) and STRONGCOIL (high-strength spring), ItemCoil. */
	public static final DeferredItem<reika.rotarycraft.content.item.CoilItem> SPRING = ITEMS.register("spring",
			() -> new reika.rotarycraft.content.item.CoilItem(new Item.Properties(), false));
	public static final DeferredItem<reika.rotarycraft.content.item.CoilItem> STRONGCOIL = ITEMS.register("strongcoil",
			() -> new reika.rotarycraft.content.item.CoilItem(new Item.Properties(), true));
	public static final DeferredItem<ScrewdriverItem> SCREWDRIVER = ITEMS.register("screwdriver", () -> new ScrewdriverItem(new Item.Properties().stacksTo(1)));
	public static final DeferredItem<MeterItem> METER = ITEMS.register("meter", () -> new MeterItem(new Item.Properties().stacksTo(1)));

	/** Original: ItemRegistry.UPGRADE (ItemEngineUpgrade), the ported variants. */
	public static final DeferredItem<EngineUpgradeItem> UPGRADE_PERFORMANCE = ITEMS.register("upgrade_performance", () -> new EngineUpgradeItem(EngineUpgradeItem.Upgrades.PERFORMANCE, new Item.Properties()));
	public static final DeferredItem<EngineUpgradeItem> UPGRADE_AFTERBURNER = ITEMS.register("upgrade_afterburner", () -> new EngineUpgradeItem(EngineUpgradeItem.Upgrades.AFTERBURNER, new Item.Properties()));
	/** Original GEARUPGRADE: the integrated gearbox frame (gear_upgrade) and the 2x/4x/8x/16x gears. */
	public static final java.util.List<DeferredItem<reika.rotarycraft.content.item.IntegratedGearboxItem>> GEAR_UPGRADES = new java.util.ArrayList<>();

	static {
		for (int i = 0; i <= 4; i++) {
			int lvl = i;
			GEAR_UPGRADES.add(ITEMS.register(i == 0 ? "gear_upgrade" : "gear_upgrade_"+(1 << i), () -> new reika.rotarycraft.content.item.IntegratedGearboxItem(new Item.Properties(), lvl)));
		}
	}

	public static final DeferredItem<EngineUpgradeItem> UPGRADE_REDSTONE = ITEMS.register("upgrade_redstone", () -> new EngineUpgradeItem(EngineUpgradeItem.Upgrades.REDSTONE, new Item.Properties()));

	/** Original: ItemRegistry.CANOLA (ItemCanolaSeed): seeds, dense seeds (plant 3x3), husks (from the Grinder). */
	public static final DeferredItem<reika.rotarycraft.content.item.CanolaSeedItem> CANOLA_SEEDS = ITEMS.register("canola_seeds", () -> new reika.rotarycraft.content.item.CanolaSeedItem(false, new Item.Properties()));
	public static final DeferredItem<reika.rotarycraft.content.item.CanolaSeedItem> DENSE_CANOLA_SEEDS = ITEMS.register("dense_canola_seeds", () -> new reika.rotarycraft.content.item.CanolaSeedItem(true, new Item.Properties()));
	public static final DeferredItem<Item> CANOLA_HUSKS = ITEMS.registerSimpleItem("canola_husks");

	/** Original: ItemRegistry.BUCKET (ItemFuelLubeBucket) 0-2; liquid nitrogen and molten HSLA come with their machines. */
	public static final DeferredItem<reika.rotarycraft.content.item.FluidBucketItem> LUBRICANT_BUCKET = bucket("lubricant_bucket", RotaryFluids.LUBRICANT);
	public static final DeferredItem<reika.rotarycraft.content.item.FluidBucketItem> JET_FUEL_BUCKET = bucket("jet_fuel_bucket", RotaryFluids.JET_FUEL);
	public static final DeferredItem<reika.rotarycraft.content.item.FluidBucketItem> ETHANOL_BUCKET = bucket("ethanol_bucket", RotaryFluids.ETHANOL);
	public static final DeferredItem<reika.rotarycraft.content.item.FluidBucketItem> LIQUID_NITROGEN_BUCKET = bucket("liquid_nitrogen_bucket", RotaryFluids.LIQUID_NITROGEN);

	private RotaryItems() {}

	private static DeferredItem<reika.rotarycraft.content.item.FluidBucketItem> bucket(String name, Supplier<? extends net.minecraft.world.level.material.Fluid> fluid) {
		return ITEMS.register(name, () -> new reika.rotarycraft.content.item.FluidBucketItem(fluid.get(), new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
	}

	/** The bucket of a RotaryCraft fluid (BaseFlowingFluid.Properties.bucket, used to fill empty buckets from tanks). */
	public static Item bucketFor(String fluidId) {
		return switch (fluidId) {
			case "lubricant" -> LUBRICANT_BUCKET.get();
			case "jet_fuel" -> JET_FUEL_BUCKET.get();
			case "ethanol" -> ETHANOL_BUCKET.get();
			case "liquid_nitrogen" -> LIQUID_NITROGEN_BUCKET.get();
			default -> Items.AIR;
		};
	}

	public static Item ethanol() {
		return ETHANOL.get();
	}

	public static Item part(CraftingPart p) {
		return PARTS.get(p).get();
	}

	public static Item gearPart(GearboxTypes g, GearPart p) {
		return GEAR_PARTS.get(g).get(p).get();
	}

	public static Item flywheelCore(Flywheels f) {
		return FLYWHEEL_CORES.get(f).get();
	}

	/** Original: MaterialRegistry.getShaftUnitItem() - sticks for wood, otherwise the material's shaft part. */
	public static Item shaftUnitItem(MaterialRegistry mat) {
		if (mat == MaterialRegistry.WOOD)
			return Items.STICK;
		return gearPart(GearboxTypes.getFromMaterial(mat), GearPart.SHAFT);
	}

	public static Iterable<? extends Supplier<? extends Item>> all() {
		return ITEMS.getEntries();
	}
}
