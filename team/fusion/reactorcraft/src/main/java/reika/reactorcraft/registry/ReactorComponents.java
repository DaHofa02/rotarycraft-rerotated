/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.registry;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;

import net.neoforged.neoforge.registries.DeferredRegister;

import reika.reactorcraft.ReactorCraft;

/** Item data that was metadata in 1.7.10. */
public final class ReactorComponents {

	public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ReactorCraft.MOD_ID);

	/** Fuel depletion steps (original FUEL/PLUTONIUM metadata 0..99); absent = fresh. */
	public static final Supplier<DataComponentType<Integer>> DEPLETION = COMPONENTS.registerComponentType("depletion",
			b -> b.persistent(Codec.intRange(0, 1000)).networkSynchronized(ByteBufCodecs.VAR_INT));
	/** Nuclear waste isotope (original WASTE metadata: Isotopes ordinal, or 1000 + element group for mixed waste). */
	public static final Supplier<DataComponentType<Integer>> ISOTOPE = COMPONENTS.registerComponentType("isotope",
			b -> b.persistent(Codec.intRange(0, 2000)).networkSynchronized(ByteBufCodecs.VAR_INT));

	/** Stored charge of chargeable tools in kJ (original item damage of the Geiger counter). */
	public static final Supplier<DataComponentType<Integer>> CHARGE = COMPONENTS.registerComponentType("charge",
			b -> b.persistent(Codec.intRange(0, Integer.MAX_VALUE)).networkSynchronized(ByteBufCodecs.VAR_INT));

	/** Water in the radiation cleaner in mB (original NBT "water"). */
	public static final Supplier<DataComponentType<Integer>> WATER = COMPONENTS.registerComponentType("water",
			b -> b.persistent(Codec.intRange(0, Integer.MAX_VALUE)).networkSynchronized(ByteBufCodecs.VAR_INT));

	/** CPU linked to a CPU remote control (original NBT cx/cy/cz and dimension id). */
	public static final Supplier<DataComponentType<net.minecraft.core.GlobalPos>> LINKED_CPU = COMPONENTS.registerComponentType("linked_cpu",
			b -> b.persistent(net.minecraft.core.GlobalPos.CODEC).networkSynchronized(net.minecraft.core.GlobalPos.STREAM_CODEC));

	private ReactorComponents() {}
}
