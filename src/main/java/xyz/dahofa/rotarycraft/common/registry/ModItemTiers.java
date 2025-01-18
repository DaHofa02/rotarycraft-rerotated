package xyz.dahofa.rotarycraft.common.registry;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.common.Tags;

public class ModItemTiers {
    public static final Tier BEDROCK_TIER = new SimpleTier(
            // The tag that determines what blocks this tool cannot break. See below for more information.
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            // Determines the durability of the tier.
            // Stone is 131, iron is 250.
            2147483633,
            // Determines the mining speed of the tier. Unused by swords.
            // Stone uses 4, iron uses 6.
            10f,
            // Determines the attack damage bonus. Different tools use this differently. For example, swords do (getAttackDamageBonus() + 4) damage.
            // Stone uses 1, iron uses 2, corresponding to 5 and 6 attack damage for swords, respectively; our sword does 5.5 damage now.
            10f,
            // Determines the enchantability of the tier. This represents how good the enchantments on this tool will be.
            // Gold uses 22, we put copper slightly below that.
            100,
            // Determines the repair ingredient of the tier. Use a supplier for lazy initializing.
            () -> Ingredient.of(Tags.Items.INGOTS_COPPER)
    );

    public static final Tier HSLA_STEEL_TIER = new SimpleTier(
            // The tag that determines what blocks this tool cannot break. See below for more information.
            BlockTags.INCORRECT_FOR_IRON_TOOL,
            // Determines the durability of the tier.
            // Stone is 131, iron is 250.
            600,
            // Determines the mining speed of the tier. Unused by swords.
            // Stone uses 4, iron uses 6.
            8f,
            // Determines the attack damage bonus. Different tools use this differently. For example, swords do (getAttackDamageBonus() + 4) damage.
            // Stone uses 1, iron uses 2, corresponding to 5 and 6 attack damage for swords, respectively; our sword does 5.5 damage now.
            100f,
            // Determines the enchantability of the tier. This represents how good the enchantments on this tool will be.
            // Gold uses 22, we put copper slightly below that.
            100,
            // Determines the repair ingredient of the tier. Use a supplier for lazy initializing.
            () -> Ingredient.of(Tags.Items.INGOTS_COPPER)
    );
}
