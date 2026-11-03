/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import reika.reactorcraft.registry.ReactorFluids;

/** Original: TileEntityWaterCell.LiquidStates - what a coolant cell holds (1000 mB). */
public enum LiquidStates {
	EMPTY(null),
	WATER(() -> Fluids.WATER),
	HEAVY(() -> ReactorFluids.get("heavy_water")),
	SODIUM(() -> ReactorFluids.get("sodium")),
	LITHIUM(() -> ReactorFluids.get("lifbe"));

	@Nullable
	private final Supplier<Fluid> fluid;

	public static final LiquidStates[] list = values();

	LiquidStates(@Nullable Supplier<Fluid> f) {
		fluid = f;
	}

	public boolean isWater() {
		return this == WATER || this == HEAVY;
	}

	@Nullable
	public Fluid getFluid() {
		return fluid != null ? fluid.get() : null;
	}

	@Nullable
	public static LiquidStates getState(Fluid f) {
		for (int i = 1; i < list.length; i++)
			if (list[i].getFluid().isSame(f))
				return list[i];
		return null;
	}
}
