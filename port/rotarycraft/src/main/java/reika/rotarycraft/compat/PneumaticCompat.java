/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.compat;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.pressure.PressureTier;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Everything that touches PneumaticCraft's API. Callers check {@link OptionalMods#pneumaticCraft()} first; without PneumaticCraft this class is never loaded,
 * and air handlers travel as plain Objects.
 */
public final class PneumaticCompat {

	private PneumaticCompat() {}

	/** A tier-two (20 bar) air handler of the given volume (mL), connectable only on that side. */
	public static Object createHandler(int volume, Direction side) {
		IAirHandlerMachine h = PneumaticRegistry.getInstance().getAirHandlerMachineFactory().createAirHandler(PressureTier.TIER_TWO, volume);
		h.setConnectableFaces(List.of(side));
		return h;
	}

	public static void setSide(Object handler, Direction side) {
		((IAirHandlerMachine)handler).setConnectableFaces(List.of(side));
	}

	public static void tick(Object handler, BlockEntity owner) {
		((IAirHandlerMachine)handler).tick(owner);
	}

	public static int getAir(Object handler) {
		return ((IAirHandlerMachine)handler).getAir();
	}

	public static float getPressure(Object handler) {
		return ((IAirHandlerMachine)handler).getPressure();
	}

	public static void addAir(Object handler, int amount) {
		((IAirHandlerMachine)handler).addAir(amount);
	}

	public static void save(Object handler, CompoundTag tag) {
		tag.put("air", ((IAirHandlerMachine)handler).serializeNBT());
	}

	public static void load(Object handler, CompoundTag tag) {
		if (tag.contains("air"))
			((IAirHandlerMachine)handler).deserializeNBT(tag.getCompound("air"));
	}

	/** Puts air into the PneumaticCraft machine at pos (its face towards us); false if there is none. */
	public static boolean pushAir(Level world, BlockPos pos, Direction face, int amount) {
		IAirHandlerMachine h = world.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE, pos, face);
		if (h == null)
			return false;
		if (amount > 0)
			h.addAir(amount);
		return true;
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent e) {
		e.registerBlockEntity(PNCCapabilities.AIR_HANDLER_MACHINE, RotaryBlockEntities.PNEUMATIC_ENGINE.get(),
				(te, side) -> te.getAirHandler() instanceof IAirHandlerMachine h && (side == null || side == te.getAirSide()) ? h : null);
	}
}
