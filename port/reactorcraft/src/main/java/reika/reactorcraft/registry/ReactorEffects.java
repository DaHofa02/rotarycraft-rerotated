/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.neoforged.neoforge.registries.DeferredRegister;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.effect.RadiationEffect;

/** Original: ReactorCraft.radiation (PotionRadiation). */
public final class ReactorEffects {

	public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ReactorCraft.MOD_ID);

	public static final Holder<MobEffect> RADIATION = EFFECTS.register("radiation", () -> new RadiationEffect()
			.addAttributeModifier(Attributes.MOVEMENT_SPEED, ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "radiation_speed"), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, lvl -> -0.25)
			.addAttributeModifier(Attributes.JUMP_STRENGTH, ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "radiation_jump"), AttributeModifier.Operation.ADD_VALUE, lvl -> -0.1));
	//constant whatever the level (NeoForge curve overload; the plain overload would scale with the level)

	private ReactorEffects() {}
}
