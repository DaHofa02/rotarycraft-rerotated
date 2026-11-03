/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.registry;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.RotaryCraft;

/** Recipe conditions for config-dependent recipes (the original decided these in code while registering recipes). */
public final class RotaryConditions {

	public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS = DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, RotaryCraft.MOD_ID);

	public static final Supplier<MapCodec<DifficultyCondition>> DIFFICULTY = CONDITIONS.register("difficulty", () -> DifficultyCondition.CODEC);
	public static final Supplier<MapCodec<ConfigCondition>> CONFIG = CONDITIONS.register("config", () -> ConfigCondition.CODEC);

	private RotaryConditions() {}

	/** True when the "Difficulty Control" option equals the value (DifficultyEffects-dependent output amounts). */
	public record DifficultyCondition(int value) implements ICondition {

		public static final MapCodec<DifficultyCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.intRange(1, 3).fieldOf("value").forGetter(DifficultyCondition::value)).apply(i, DifficultyCondition::new));

		@Override
		public boolean test(IContext context) {
			return DifficultyEffects.getDifficulty().ordinal()+1 == value;
		}

		@Override
		public MapCodec<? extends ICondition> codec() {
			return CODEC;
		}
	}

	/** True when a boolean RotaryCraft option is on. Known options: table_machines ("Crafting Table can Make Machines"), rotate_hose ("Rotate Hose/Pipe/Fuel Line Recipes"). */
	public record ConfigCondition(String option) implements ICondition {

		public static final MapCodec<ConfigCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.STRING.fieldOf("option").forGetter(ConfigCondition::option)).apply(i, ConfigCondition::new));

		@Override
		public boolean test(IContext context) {
			return switch (option) {
				case "table_machines" -> RotaryConfig.TABLEMACHINES.get();
				case "rotate_hose" -> RotaryConfig.ROTATEHOSE.get();
				default -> false;
			};
		}

		@Override
		public MapCodec<? extends ICondition> codec() {
			return CODEC;
		}
	}
}
