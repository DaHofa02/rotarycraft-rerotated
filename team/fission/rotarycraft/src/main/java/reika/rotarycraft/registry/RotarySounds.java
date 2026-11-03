/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.registry;

import java.util.Locale;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.registries.DeferredRegister;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.RotaryCraft;

/** Original: Reika.RotaryCraft.Registry.SoundRegistry. Sound files and names are unchanged; see assets/rotarycraft/sounds.json. */
public final class RotarySounds {

	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, RotaryCraft.MOD_ID);

	public static final Supplier<SoundEvent> ELECTRIC = sound("ELECTRIC");
	public static final Supplier<SoundEvent> WIND = sound("WIND");
	public static final Supplier<SoundEvent> STEAM = sound("STEAM");
	public static final Supplier<SoundEvent> CAR = sound("CAR");
	public static final Supplier<SoundEvent> HYDRO = sound("HYDRO");
	public static final Supplier<SoundEvent> MICRO = sound("MICRO");
	public static final Supplier<SoundEvent> JET = sound("JET");
	public static final Supplier<SoundEvent> FLYWHEEL = sound("FLYWHEEL");
	public static final Supplier<SoundEvent> JETSTART = sound("JETSTART");
	public static final Supplier<SoundEvent> SHORTJET = sound("SHORTJET");
	public static final Supplier<SoundEvent> AFTERBURN = sound("AFTERBURN");
	public static final Supplier<SoundEvent> INGESTION = sound("INGESTION");
	public static final Supplier<SoundEvent> CRAFT = sound("CRAFT");
	public static final Supplier<SoundEvent> FRICTION = sound("FRICTION");
	public static final Supplier<SoundEvent> PUMP = sound("PUMP");
	public static final Supplier<SoundEvent> PULSEJET = sound("PULSEJET");
	public static final Supplier<SoundEvent> SPARK = sound("SPARK");
	public static final Supplier<SoundEvent> FRIDGE = sound("FRIDGE");

	private RotarySounds() {}

	private static Supplier<SoundEvent> sound(String name) {
		String id = name.toLowerCase(Locale.ROOT);
		return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, id)));
	}

	/** Whether a sound's volume follows the volume options ("#" prefix in the original SoundRegistry). All ported sounds so far are. */
	private static float getSoundVolume(SoundEvent s) {
		float vol = RotaryConfig.MACHINEVOLUME.get().floatValue();
		if (isEngineSound(s))
			vol *= RotaryConfig.ENGINEVOLUME.get().floatValue();
		return Math.max(0, Math.min(1, vol));
	}

	/** Original isEngineSound(): the sound file name ends with "engine". */
	private static boolean isEngineSound(SoundEvent s) {
		return s == ELECTRIC.get() || s == WIND.get() || s == STEAM.get() || s == CAR.get() || s == HYDRO.get() || s == MICRO.get() || s == JET.get();
	}

	/** Original: SoundRegistry.playSoundAtBlock (server side only). */
	public static void play(Level world, BlockPos pos, SoundEvent s, float vol, float pitch) {
		if (world.isClientSide)
			return;
		world.playSound(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, s, SoundSource.MASTER, vol*getSoundVolume(s), pitch);
	}
}
