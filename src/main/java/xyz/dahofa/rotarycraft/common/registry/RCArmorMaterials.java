package xyz.dahofa.rotarycraft.common.registry;

import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import xyz.dahofa.rotarycraft.api.lib.Names;

import java.util.EnumMap;
import java.util.List;

import static xyz.dahofa.rotarycraft.api.RotaryCraftRegistry.RL;

public class RCArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Names.MOD_ID);

    public static final ResourceLocation BEDROCK_ARMOR = RL("bedrock");
    public static final ResourceLocation HSLA_STEEL_ARMOR = RL("hsla_steel");

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> BEDROCK =
            ARMOR_MATERIALS.register(BEDROCK_ARMOR.getPath(), () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 5);
                        map.put(ArmorItem.Type.LEGGINGS, 10);
                        map.put(ArmorItem.Type.CHESTPLATE, 12);
                        map.put(ArmorItem.Type.HELMET, 6);
                        map.put(ArmorItem.Type.BODY, 4);
                    }),
                    200,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(RCItems.BEDROCK_ALLOY_INGOT.get()),
                    List.of(
                            new ArmorMaterial.Layer(BEDROCK_ARMOR)
                    ),
                    1.0f,
                    0.075f
            ));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> HSLA_STEEL =
            ARMOR_MATERIALS.register(HSLA_STEEL_ARMOR.getPath(), () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 3);
                        map.put(ArmorItem.Type.LEGGINGS, 5);
                        map.put(ArmorItem.Type.CHESTPLATE, 7);
                        map.put(ArmorItem.Type.HELMET, 3);
                        map.put(ArmorItem.Type.BODY, 4);
                    }),
                    200,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(RCItems.HSLA_STEEL_INGOT.get()),
                    List.of(
                            new ArmorMaterial.Layer(HSLA_STEEL_ARMOR)
                    ),
                    1.0f,
                    0.075f
            ));
}
