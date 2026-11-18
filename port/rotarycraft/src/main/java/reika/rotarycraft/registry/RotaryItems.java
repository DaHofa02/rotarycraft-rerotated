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
	public static final DeferredItem<reika.rotarycraft.content.item.CraftPatternItem> CRAFT_PATTERN = ITEMS.register("craft_pattern", () -> new reika.rotarycraft.content.item.CraftPatternItem(new Item.Properties()));

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
	public static final DeferredItem<EngineUpgradeItem> UPGRADE_MAGNET_1 = ITEMS.register("upgrade_magnet_1", () -> new EngineUpgradeItem(EngineUpgradeItem.Upgrades.MAGNET_1, new Item.Properties()));
	public static final DeferredItem<EngineUpgradeItem> UPGRADE_MAGNET_2 = ITEMS.register("upgrade_magnet_2", () -> new EngineUpgradeItem(EngineUpgradeItem.Upgrades.MAGNET_2, new Item.Properties()));
	public static final DeferredItem<EngineUpgradeItem> UPGRADE_MAGNET_3 = ITEMS.register("upgrade_magnet_3", () -> new EngineUpgradeItem(EngineUpgradeItem.Upgrades.MAGNET_3, new Item.Properties()));
	public static final DeferredItem<EngineUpgradeItem> UPGRADE_MAGNET_4 = ITEMS.register("upgrade_magnet_4", () -> new EngineUpgradeItem(EngineUpgradeItem.Upgrades.MAGNET_4, new Item.Properties()));
	public static final DeferredItem<EngineUpgradeItem> UPGRADE_MAGNET_5 = ITEMS.register("upgrade_magnet_5", () -> new EngineUpgradeItem(EngineUpgradeItem.Upgrades.MAGNET_5, new Item.Properties()));
	public static final DeferredItem<EngineUpgradeItem> UPGRADE_EFFICIENCY = ITEMS.register("upgrade_efficiency", () -> new EngineUpgradeItem(EngineUpgradeItem.Upgrades.EFFICIENCY, new Item.Properties()));
	public static final DeferredItem<EngineUpgradeItem> UPGRADE_FLUX = ITEMS.register("upgrade_flux", () -> new EngineUpgradeItem(EngineUpgradeItem.Upgrades.FLUX, new Item.Properties()));
	public static final DeferredItem<EngineUpgradeItem> UPGRADE_LODESTONE = ITEMS.register("upgrade_lodestone", () -> new EngineUpgradeItem(EngineUpgradeItem.Upgrades.LODESTONE, new Item.Properties()));
	/** Bedrock tools and armour (2026-11-18, own implementation; see BedrockTools). */
	public static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.world.item.ArmorMaterial> ARMOR_MATERIALS =
			net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.ARMOR_MATERIAL, reika.rotarycraft.RotaryCraft.MOD_ID);
	public static final net.minecraft.core.Holder<net.minecraft.world.item.ArmorMaterial> BEDROCK_ARMOR = ARMOR_MATERIALS.register("bedrock", () -> new net.minecraft.world.item.ArmorMaterial(
			java.util.Map.of(net.minecraft.world.item.ArmorItem.Type.BOOTS, 4, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, 7, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, 9,
					net.minecraft.world.item.ArmorItem.Type.HELMET, 4, net.minecraft.world.item.ArmorItem.Type.BODY, 12), 10,
			net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_NETHERITE, () -> net.minecraft.world.item.crafting.Ingredient.of(part(reika.rotarycraft.content.item.CraftingPart.BEDINGOT)),
			java.util.List.of(new net.minecraft.world.item.ArmorMaterial.Layer(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(reika.rotarycraft.RotaryCraft.MOD_ID, "bedrock"))), 4F, 0.2F));
	public static final DeferredItem<Item> BEDROCK_PICKAXE = ITEMS.register("bedrock_pickaxe", reika.rotarycraft.content.item.BedrockTools.Pickaxe::new);
	public static final DeferredItem<Item> BEDROCK_AXE = ITEMS.register("bedrock_axe", reika.rotarycraft.content.item.BedrockTools.Axe::new);
	public static final DeferredItem<Item> BEDROCK_SHOVEL = ITEMS.register("bedrock_shovel", reika.rotarycraft.content.item.BedrockTools.Shovel::new);
	public static final DeferredItem<Item> BEDROCK_SWORD = ITEMS.register("bedrock_sword", reika.rotarycraft.content.item.BedrockTools.Sword::new);
	public static final DeferredItem<Item> BEDROCK_HOE = ITEMS.register("bedrock_hoe", reika.rotarycraft.content.item.BedrockTools.Hoe::new);
	public static final DeferredItem<Item> BEDROCK_SHEARS = ITEMS.register("bedrock_shears", reika.rotarycraft.content.item.BedrockTools.Shears::new);
	public static final DeferredItem<Item> BEDROCK_SCYTHE = ITEMS.register("bedrock_scythe", reika.rotarycraft.content.item.BedrockTools.Scythe::new);
	public static final DeferredItem<Item> BEDROCK_PAXEL = ITEMS.register("bedrock_paxel", reika.rotarycraft.content.item.BedrockTools.Paxel::new);
	public static final DeferredItem<Item> BEDROCK_HELMET = ITEMS.register("bedrock_helmet", () -> new reika.rotarycraft.content.item.BedrockTools.Armour(BEDROCK_ARMOR, net.minecraft.world.item.ArmorItem.Type.HELMET));
	public static final DeferredItem<Item> BEDROCK_CHESTPLATE = ITEMS.register("bedrock_chestplate", () -> new reika.rotarycraft.content.item.BedrockTools.Armour(BEDROCK_ARMOR, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE));
	public static final DeferredItem<Item> BEDROCK_LEGGINGS = ITEMS.register("bedrock_leggings", () -> new reika.rotarycraft.content.item.BedrockTools.Armour(BEDROCK_ARMOR, net.minecraft.world.item.ArmorItem.Type.LEGGINGS));
	public static final DeferredItem<Item> BEDROCK_BOOTS = ITEMS.register("bedrock_boots", () -> new reika.rotarycraft.content.item.BedrockTools.Armour(BEDROCK_ARMOR, net.minecraft.world.item.ArmorItem.Type.BOOTS));

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
