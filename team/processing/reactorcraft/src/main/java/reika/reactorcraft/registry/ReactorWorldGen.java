/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import net.neoforged.neoforge.registries.DeferredRegister;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.world.ReactorOrePlacement;

/** Ore placement type; the features and biome modifiers are data (worldgen/*, neoforge/biome_modifier). */
public final class ReactorWorldGen {

	public static final DeferredRegister<PlacementModifierType<?>> PLACEMENTS = DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, ReactorCraft.MOD_ID);

	public static final Supplier<PlacementModifierType<ReactorOrePlacement>> ORE_PLACEMENT = PLACEMENTS.register("ore", () -> () -> ReactorOrePlacement.CODEC);

	private ReactorWorldGen() {}
}
