/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import net.neoforged.neoforge.registries.DeferredRegister;

import reika.electricraft.ElectriCraft;
import reika.electricraft.world.OreControlPlacement;

/** The ore placement (data: worldgen/configured_feature, placed_feature, neoforge/biome_modifier). */
public final class ElectriWorldGen {

	public static final DeferredRegister<PlacementModifierType<?>> PLACEMENTS = DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, ElectriCraft.MOD_ID);

	public static final Supplier<PlacementModifierType<OreControlPlacement>> ORE_CONTROL = PLACEMENTS.register("ore_control", () -> () -> OreControlPlacement.CODEC);

	private ElectriWorldGen() {}
}
