/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.fusion;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

import reika.reactorcraft.ReactorCraft;

/** Original: ReactorCraft.fusionDamage ("jumped in a Fusion Reactor"), dealt by plasma. */
public final class FusionDamage {

	public static final ResourceKey<DamageType> FUSION = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "fusion"));

	private FusionDamage() {}
}
