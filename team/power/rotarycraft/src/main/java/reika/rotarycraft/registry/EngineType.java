/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.registry;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import reika.rotarycraft.registry.GearboxTypes.GearPart;

/**
 * Original: Reika.RotaryCraft.Registry.EngineType. Speed and torque per engine are unchanged:
 * DC 256 rad/s @ 4 Nm, Wind 1024 @ 8, Steam 512 @ 32, Gas 512 @ 128, AC 256 @ 512, Sport 1024 @ 256, Hydro 32 @ 16384,
 * Microturbine 131072 @ 16, Gas Turbine 65536 @ 1024. Fluids from other mods (Forestry bioethanol, foreign oxygen/air) are not accepted.
 */
public enum EngineType implements StringRepresentable {
	DC(		256, 	4, 		EngineClass.ELECTRIC),
	WIND(	1024, 	8, 		EngineClass.KINETIC),
	STEAM(	512, 	32, 	EngineClass.THERMAL),
	GAS(	512, 	128, 	EngineClass.PISTON),
	AC(		256, 	512, 	EngineClass.ELECTRIC),
	SPORT(	1024, 	256, 	EngineClass.PISTON),
	HYDRO(	32, 	16384, 	EngineClass.KINETIC),
	MICRO(	131072, 16, 	EngineClass.TURBINE),
	JET(	65536, 	1024, 	EngineClass.TURBINE);

	private final int torque;
	private final int omega;
	public final EngineClass type;

	public static final EngineType[] engineList = values();

	EngineType(int rpm, int tq, EngineClass type) {
		omega = rpm;
		torque = tq;
		this.type = type;
	}

	@Override
	public String getSerializedName() {
		return this.name().toLowerCase(Locale.ROOT);
	}

	public int getSpeed() {
		return omega;
	}

	public int getTorque() {
		return torque;
	}

	public long getPower() {
		return (long)torque*(long)omega;
	}

	public boolean isJetFueled() {
		return this == JET || this == MICRO;
	}

	public boolean isEthanolFueled() {
		return this == GAS || this == SPORT;
	}

	public boolean isWaterPiped() {
		return this == STEAM || this == SPORT;
	}

	public boolean hasGui() {
		return this == STEAM || this == GAS || this == AC || this == SPORT || this == MICRO || this == JET;
	}

	public boolean burnsFuel() {
		return this == STEAM || this == GAS || this == SPORT || this == MICRO || this == JET;
	}

	public int getSoundLength() {
		if (this.carNoise())
			return 88;
		if (this.electricNoise())
			return 74;
		if (this.steamNoise())
			return 49;
		if (this.waterNoise())
			return 59;
		if (this.windNoise())
			return 105;
		if (this.jetNoise())
			return 79;
		if (this.turbineNoise())
			return 20;
		return 0;
	}

	public boolean isCooled() {
		return this == STEAM || this == SPORT;
	}

	public boolean isAirBreathing() {
		return this == GAS || this == SPORT || this == MICRO || this == JET;
	}

	public boolean electricNoise() {
		return type == EngineClass.ELECTRIC;
	}

	public boolean carNoise() {
		return this == GAS || this == SPORT;
	}

	public boolean waterNoise() {
		return this == HYDRO;
	}

	public boolean steamNoise() {
		return this == STEAM;
	}

	public boolean jetNoise() {
		return this == JET;
	}

	public boolean turbineNoise() {
		return this == JET || this == MICRO;
	}

	public boolean windNoise() {
		return this == WIND;
	}

	public boolean canHurtPlayer() {
		return this == JET || this == SPORT || this == WIND || this == HYDRO;
	}

	public boolean isValidFuel(ItemStack is) {
		if (this == STEAM)
			return is.is(Items.WATER_BUCKET);
		if (this == GAS)
			return is.is(RotaryItems.ethanol());
		if (this == SPORT)
			return is.is(RotaryItems.ethanol()) || this.isAdditive(is);
		if (this == AC)
			return is.is(RotaryItems.gearPart(GearboxTypes.STEEL, GearPart.SHAFTCORE)) || is.is(RotaryItems.gearPart(GearboxTypes.TUNGSTEN, GearPart.SHAFTCORE));
		return false;
	}

	public boolean isAdditive(ItemStack is) {
		if (this == SPORT)
			return is.is(Items.REDSTONE) || is.is(Items.GUNPOWDER) || is.is(Items.BLAZE_POWDER);
		return false;
	}

	/** Returns ticks */
	public int getFuelUnitDuration() {
		return switch(this) {
			case STEAM -> 18;
			case GAS -> 12;
			case AC -> 600;
			case SPORT -> 6;
			case MICRO -> 48;
			case JET -> 2;
			default -> 0;
		};
	}

	public boolean isEMPImmune() {
		return this == HYDRO || this == WIND;
	}

	public boolean isECUControllable() {
		return type.isECUControllable();
	}

	public boolean canReceiveFluid(Fluid fluid) {
		return switch(this) {
			case STEAM -> fluid == Fluids.WATER;
			case GAS -> fluid == RotaryFluids.ETHANOL.get();
			case SPORT -> fluid == Fluids.WATER || fluid == RotaryFluids.ETHANOL.get();
			case HYDRO -> fluid == RotaryFluids.LUBRICANT.get();
			case MICRO, JET -> fluid == RotaryFluids.JET_FUEL.get();
			default -> false;
		};
	}

	@Nullable
	public Fluid getFuelType() {
		return switch(this) {
			case GAS, SPORT -> RotaryFluids.ETHANOL.get();
			case MICRO, JET -> RotaryFluids.JET_FUEL.get();
			default -> null;
		};
	}

	public boolean usesFluids() {
		return this.burnsFuel() || this.isWaterPiped() || this.requiresLubricant();
	}

	public boolean requiresLubricant() {
		return this == HYDRO;
	}

	public int getSizeInventory() {
		return switch(this) {
			case STEAM, GAS, AC -> 1;
			case SPORT -> 2;
			default -> 0;
		};
	}

	public boolean allowInventoryStacking() {
		return this == GAS || this == SPORT;
	}

	public boolean usesAdditives() {
		return this == SPORT;
	}

	public boolean hasInventory() {
		return this.getSizeInventory() > 0;
	}

	public boolean needsWater() {
		return this == STEAM || this == SPORT;
	}

	public enum EngineClass {
		KINETIC,
		THERMAL,
		ELECTRIC,
		PISTON,
		TURBINE;

		public boolean isECUControllable() {
			return this == PISTON || this == TURBINE;
		}
	}
}
