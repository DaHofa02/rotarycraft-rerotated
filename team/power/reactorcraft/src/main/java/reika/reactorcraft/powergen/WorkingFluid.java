/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.powergen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import reika.reactorcraft.registry.ReactorFluids;

/** Original: Reika.ReactorCraft.Registry.WorkingFluid - what a boiler makes steam from; heavy water counts as water. */
public enum WorkingFluid {

	EMPTY(0, 0),
	WATER(1F, 100),
	AMMONIA(2, -33);

	public final float efficiency;
	public final int boilingTemp;

	public static final WorkingFluid[] list = values();

	WorkingFluid(float e, int boil) {
		efficiency = e;
		boilingTemp = boil;
	}

	public Fluid getFluid() {
		return switch (this) {
			case WATER -> Fluids.WATER;
			case AMMONIA -> ReactorFluids.get("ammonia");
			default -> Fluids.EMPTY;
		};
	}

	public static WorkingFluid getFromNBT(CompoundTag NBT) {
		int val = NBT.getInt("workingfluid");
		if (val >= 0 && val < list.length)
			return list[val];
		return EMPTY;
	}

	public void saveToNBT(CompoundTag NBT) {
		NBT.putInt("workingfluid", this.ordinal());
	}

	/** Null if the fluid cannot be boiled. */
	public static WorkingFluid getWorkingFluid(Fluid f) {
		if (f == null || f == Fluids.EMPTY)
			return null;
		if (f.isSame(ReactorFluids.get("heavy_water")))
			return WATER;
		for (WorkingFluid w : list)
			if (w != EMPTY && f.isSame(w.getFluid()))
				return w;
		return null;
	}
}
