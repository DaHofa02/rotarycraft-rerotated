package xyz.dahofa.rotarycraft.common.registry;

import xyz.dahofa.rotarycraft.api.lib.Names;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import xyz.dahofa.rotarycraft.common.item.gear.bedrock.*;
import xyz.dahofa.rotarycraft.common.item.gear.hslasteel.*;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Names.MOD_ID);
    public static final DeferredRegister.Items TOOLS = DeferredRegister.createItems(Names.MOD_ID);

    /* ------------- ITEMS ------------- */

    public static final DeferredItem<Item> BALL_BEARING = registerItems("ball_bearing");
    public static final DeferredItem<Item> BEDROCK_ALLOY_INGOT = registerItems("bedrock_alloy_ingot");
    public static final DeferredItem<Item> HSLA_STEEL_INGOT = registerItems("hsla_steel_ingot");
    public static final DeferredItem<Item> HSLA_STEEL_GEAR = registerItems("hsla_steel_gear");
    public static final DeferredItem<Item> HSLA_STEEL_2X_GEAR_UNIT = registerItems("hsla_steel_2x_gear_unit");
    public static final DeferredItem<Item> HSLA_STEEL_4X_GEAR_UNIT = registerItems("hsla_steel_4x_gear_unit");
    public static final DeferredItem<Item> HSLA_STEEL_8X_GEAR_UNIT = registerItems("hsla_steel_8x_gear_unit");
    public static final DeferredItem<Item> HSLA_STEEL_16X_GEAR_UNIT = registerItems("hsla_steel_16x_gear_unit");
    public static final DeferredItem<Item> HSLA_STEEL_ROD = registerItems("hsla_steel_rod");
    public static final DeferredItem<Item> HSLA_STEEL_SHAFT_BEARING = registerItems("hsla_steel_shaft_bearing");
    public static final DeferredItem<Item> HSLA_STEEL_SHAFT_CORE = registerItems("hsla_steel_shaft_core");

    /* ------------- TOOLS ------------- */

    public static final DeferredItem<Item> SCREWDRIVER = registerTools("screwdriver");

    public static final DeferredItem<Item> HSLA_STEEL_HELMET = registerTools("hsla_steel_helmet",
            () -> new HSLASteelArmorItem(ArmorItem.Type.HELMET));
    public static final DeferredItem<Item> HSLA_STEEL_CHESTPLATE = registerTools("hsla_steel_chestplate",
            () -> new HSLASteelArmorItem(ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<Item> HSLA_STEEL_LEGGINGS = registerTools("hsla_steel_leggings",
            () -> new HSLASteelArmorItem(ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<Item> HSLA_STEEL_BOOTS = registerTools("hsla_steel_boots",
            () -> new HSLASteelArmorItem(ArmorItem.Type.BOOTS));

    public static final DeferredItem<Item> HSLA_STEEL_SWORD = registerTools("hsla_steel_sword",
            () -> new HSLASteelSwordItem());
    public static final DeferredItem<Item> HSLA_STEEL_PICKAXE = registerTools("hsla_steel_pickaxe",
            () -> new HSLASteelPickaxeItem());
    public static final DeferredItem<Item> HSLA_STEEL_AXE = registerTools("hsla_steel_axe",
            () -> new HSLASteelAxeItem());
    public static final DeferredItem<Item> HSLA_STEEL_SHOVEL = registerTools("hsla_steel_shovel",
            () -> new HSLASteelShovelItem());
    public static final DeferredItem<Item> HSLA_STEEL_HOE = registerTools("hsla_steel_hoe",
            () -> new HSLASteelHoeItem());
    public static final DeferredItem<Item> HSLA_STEEL_SHEARS = registerTools("hsla_steel_shears",
            () -> new HSLASteelShearsItem());

    public static final DeferredItem<Item> BEDROCK_HELMET = registerTools("bedrock_helmet",
            () -> new BedrockArmorItem(ArmorItem.Type.HELMET));
    public static final DeferredItem<Item> BEDROCK_CHESTPLATE = registerTools("bedrock_chestplate",
            () -> new BedrockArmorItem(ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<Item> BEDROCK_LEGGINGS = registerTools("bedrock_leggings",
            () -> new BedrockArmorItem(ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<Item> BEDROCK_BOOTS = registerTools("bedrock_boots",
            () -> new BedrockArmorItem(ArmorItem.Type.BOOTS));

    public static final DeferredItem<Item> BEDROCK_SWORD = registerTools("bedrock_sword",
            () -> new BedrockSwordItem());
    public static final DeferredItem<Item> BEDROCK_PICKAXE = registerTools("bedrock_pickaxe",
            () -> new BedrockPickaxeItem());
    public static final DeferredItem<Item> BEDROCK_AXE = registerTools("bedrock_axe",
            () -> new BedrockAxeItem());
    public static final DeferredItem<Item> BEDROCK_SHOVEL = registerTools("bedrock_shovel",
            () -> new BedrockShovelItem());
    public static final DeferredItem<Item> BEDROCK_HOE = registerTools("bedrock_hoe",
            () -> new BedrockHoeItem());
    public static final DeferredItem<Item> BEDROCK_SHEARS = registerTools("bedrock_shears",
            () -> new BedrockShearsItem());


    /* --------------------------------- */

    public static Item.Properties defaultProps() {
        return new Item.Properties();
    }

    public static Item.Properties toolProps() {
        return defaultProps().stacksTo(1);
    }

    public static <T extends Item> DeferredItem<T> registerTools(final String name, final Supplier<T> sup) {
        return TOOLS.register(name, sup);
    }

    public static <T extends Item> DeferredItem<T> registerItems(final String name, final Supplier<T> sup) {
        return ITEMS.register(name, sup);
    }

    private static DeferredItem<Item> registerTools(final String name) {
        return registerTools(name, () -> new Item(ModItems.defaultProps()));
    }

    private static DeferredItem<Item> registerItems(final String name) {
        return registerItems(name, () -> new Item(ModItems.defaultProps()));
    }

    /*private static DeferredItem<BucketItem> registerBucket(String name, Supplier<? extends Fluid> sup) {
        return register(name, () -> new PneumaticCraftBucketItem(sup.get()));
    }*/

    /*private static DeferredItem<Item> registerFood(final String name, FoodProperties food) {
        return register(name, () -> new Item(defaultProps().food(food)));
    }*/
}
