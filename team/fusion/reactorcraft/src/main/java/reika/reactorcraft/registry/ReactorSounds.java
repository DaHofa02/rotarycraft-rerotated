/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.registry;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.registries.DeferredRegister;

import reika.reactorcraft.ReactorCraft;

/** Original: Reika.ReactorCraft.Registry.ReactorSounds (the sounds ported so far). Files unchanged; see assets/reactorcraft/sounds.json. */
public final class ReactorSounds {

	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ReactorCraft.MOD_ID);

	public static final Supplier<SoundEvent> CONTROL = sound("control");
	public static final Supplier<SoundEvent> SCRAM = sound("scram");
	public static final Supplier<SoundEvent> TURBINE = sound("turbine");
	public static final Supplier<SoundEvent> GENERATOR_RF = sound("gen_rf");
	public static final Supplier<SoundEvent> GENERATOR_ELC = sound("gen_elc");
	public static final Supplier<SoundEvent> FUSION = sound("fusion");

	private ReactorSounds() {}

	private static Supplier<SoundEvent> sound(String id) {
		return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, id)));
	}

	/** Original: ReactorSounds.playSoundAtBlock (server side; these sounds have no volume option). */
	public static void play(Level world, BlockPos pos, SoundEvent s, float vol, float pitch) {
		if (world == null || world.isClientSide)
			return;
		world.playSound(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, s, SoundSource.MASTER, vol, pitch);
	}
}
