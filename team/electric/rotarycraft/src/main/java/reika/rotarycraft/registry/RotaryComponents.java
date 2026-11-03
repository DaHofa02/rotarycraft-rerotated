/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.registry;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import net.neoforged.neoforge.registries.DeferredRegister;

import io.netty.buffer.ByteBuf;
import reika.rotarycraft.RotaryCraft;

/** Item data that the original kept in item damage values and stack NBT. */
public final class RotaryComponents {

	public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, RotaryCraft.MOD_ID);

	/** Shaft material (original: shaft item NBT "material"). */
	public static final Supplier<DataComponentType<MaterialRegistry>> SHAFT_MATERIAL = enumComponent("shaft_material", MaterialRegistry.values());
	/** Shaft cross variant (original: shaft item NBT "cross"). */
	public static final Supplier<DataComponentType<Boolean>> SHAFT_CROSS = COMPONENTS.registerComponentType("shaft_cross",
			b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
	/** Gearbox material (original: gearbox item NBT "type"). */
	public static final Supplier<DataComponentType<GearboxTypes>> GEARBOX_TYPE = enumComponent("gearbox_type", GearboxTypes.values());
	/** Gearbox ratio 2/4/8/16 (original: gearbox item damage = log2(ratio)-1). */
	public static final Supplier<DataComponentType<Integer>> GEAR_RATIO = COMPONENTS.registerComponentType("gear_ratio",
			b -> b.persistent(Codec.intRange(2, 16)).networkSynchronized(ByteBufCodecs.VAR_INT));
	/** Gearbox wear, lubricant and bearing (original: NBTMachine tags "damage", "lube", "bearing"). */
	public static final Supplier<DataComponentType<GearboxData>> GEARBOX_DATA = COMPONENTS.registerComponentType("gearbox_data",
			b -> b.persistent(GearboxData.CODEC).networkSynchronized(GearboxData.STREAM_CODEC));
	/** Flywheel type (original: flywheel item damage). */
	public static final Supplier<DataComponentType<Flywheels>> FLYWHEEL_TYPE = enumComponent("flywheel_type", Flywheels.values());
	/** Bedrock shaft junction (original: splitter item NBT "bedrock"). */
	public static final Supplier<DataComponentType<Boolean>> BEDROCK_JUNCTION = COMPONENTS.registerComponentType("bedrock_junction",
			b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

	/** Fluid in an integrated gearbox upgrade (original: item NBT "lvl" and the fluid name). */
	public static final Supplier<DataComponentType<net.neoforged.neoforge.fluids.SimpleFluidContent>> GEAR_FLUID = COMPONENTS.registerComponentType("gear_fluid",
			b -> b.persistent(net.neoforged.neoforge.fluids.SimpleFluidContent.CODEC).networkSynchronized(net.neoforged.neoforge.fluids.SimpleFluidContent.STREAM_CODEC));

	/** Stored energy of a wound coil in kJ, 0-32000 (original: spring item damage). */
	public static final Supplier<DataComponentType<Integer>> CHARGE = COMPONENTS.registerComponentType("charge",
			b -> b.persistent(Codec.intRange(0, Integer.MAX_VALUE)).networkSynchronized(ByteBufCodecs.VAR_INT));

	/** Shaft core magnetization for the AC engine (original: item NBT "magnet"). */
	public static final Supplier<DataComponentType<Integer>> MAGNETIZATION = COMPONENTS.registerComponentType("magnetization",
			b -> b.persistent(Codec.intRange(0, Integer.MAX_VALUE)).networkSynchronized(ByteBufCodecs.VAR_INT));

	/** Bedrock-reinforced hydrokinetic engine (original: engine item NBT "bed"). */
	public static final Supplier<DataComponentType<Boolean>> BEDROCK_HYDRO = COMPONENTS.registerComponentType("bedrock_hydro",
			b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

	/** Gas turbine foreign object damage 0-8 (original: engine item NBT "damage"). */
	public static final Supplier<DataComponentType<Integer>> JET_DAMAGE = COMPONENTS.registerComponentType("jet_damage",
			b -> b.persistent(Codec.intRange(0, 8)).networkSynchronized(ByteBufCodecs.VAR_INT));

	/** Reservoir contents and glass cover (original: item NBT "fluid", "lvl", "cover"). */
	public static final Supplier<DataComponentType<ReservoirData>> RESERVOIR_DATA = COMPONENTS.registerComponentType("reservoir_data",
			b -> b.persistent(ReservoirData.CODEC).networkSynchronized(ReservoirData.STREAM_CODEC));

	public record ReservoirData(net.neoforged.neoforge.fluids.FluidStack fluid, boolean cover) {

		public static final Codec<ReservoirData> CODEC = RecordCodecBuilder.create(i -> i.group(
				net.neoforged.neoforge.fluids.FluidStack.OPTIONAL_CODEC.optionalFieldOf("fluid", net.neoforged.neoforge.fluids.FluidStack.EMPTY).forGetter(ReservoirData::fluid),
				Codec.BOOL.optionalFieldOf("cover", false).forGetter(ReservoirData::cover)).apply(i, ReservoirData::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, ReservoirData> STREAM_CODEC = StreamCodec.composite(
				net.neoforged.neoforge.fluids.FluidStack.OPTIONAL_STREAM_CODEC, ReservoirData::fluid,
				ByteBufCodecs.BOOL, ReservoirData::cover,
				ReservoirData::new);

		@Override
		public boolean equals(Object o) {
			return o instanceof ReservoirData d && d.cover == cover && net.neoforged.neoforge.fluids.FluidStack.matches(d.fluid, fluid);
		}

		@Override
		public int hashCode() {
			return net.neoforged.neoforge.fluids.FluidStack.hashFluidAndComponents(fluid)*31+fluid.getAmount()*2+(cover ? 1 : 0);
		}
	}

	private RotaryComponents() {}

	private static <E extends Enum<E> & StringRepresentable> Supplier<DataComponentType<E>> enumComponent(String name, E[] values) {
		Codec<E> codec = StringRepresentable.fromEnum(() -> values);
		StreamCodec<ByteBuf, E> stream = ByteBufCodecs.idMapper(i -> values[i], Enum::ordinal);
		return COMPONENTS.registerComponentType(name, b -> b.persistent(codec).networkSynchronized(stream));
	}

	public record GearboxData(int damage, int lubricant, GearboxTypes bearing) {

		public static final Codec<GearboxData> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("damage").forGetter(GearboxData::damage),
				Codec.INT.fieldOf("lube").forGetter(GearboxData::lubricant),
				StringRepresentable.fromEnum(GearboxTypes::values).fieldOf("bearing").forGetter(GearboxData::bearing)
				).apply(i, GearboxData::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, GearboxData> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, GearboxData::damage,
				ByteBufCodecs.VAR_INT, GearboxData::lubricant,
				ByteBufCodecs.idMapper(i -> GearboxTypes.values()[i], GearboxTypes::ordinal), GearboxData::bearing,
				GearboxData::new);
	}
}
