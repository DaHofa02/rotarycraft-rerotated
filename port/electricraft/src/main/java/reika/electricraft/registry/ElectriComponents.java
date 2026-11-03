/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.registry;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;

import net.neoforged.neoforge.registries.DeferredRegister;

import reika.electricraft.ElectriCraft;

/** Item data of ElectriCraft machines (original item NBT). */
public final class ElectriComponents {

	public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ElectriCraft.MOD_ID);

	/** Motor amplifier (original NBT "amp"). */
	public static final Supplier<DataComponentType<Integer>> MOTOR_AMP = COMPONENTS.registerComponentType("motor_amp",
			b -> b.persistent(Codec.intRange(1, 1000)).networkSynchronized(ByteBufCodecs.VAR_INT));

	/** Fuse current limit (original item NBT "currentlim"). */
	public static final Supplier<DataComponentType<Integer>> FUSE_LIMIT = COMPONENTS.registerComponentType("fuse_limit",
			b -> b.persistent(Codec.intRange(0, Integer.MAX_VALUE)).networkSynchronized(ByteBufCodecs.VAR_INT));

	/** Stored battery energy in J (original item NBT "nrg"). */
	public static final Supplier<DataComponentType<Long>> BATTERY_ENERGY = COMPONENTS.registerComponentType("battery_energy",
			b -> b.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

	private ElectriComponents() {}
}
