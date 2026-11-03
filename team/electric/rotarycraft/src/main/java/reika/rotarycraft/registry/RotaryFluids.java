/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import reika.rotarycraft.RotaryCraft;

/**
 * Original: the EnhancedFluid definitions in Reika.RotaryCraft.RotaryCraft. Colours, densities, viscosities and temperatures are unchanged.
 * As in the original, the fluids have no world block; they only exist in tanks, pipes and buckets.
 */
public final class RotaryFluids {

	public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, RotaryCraft.MOD_ID);
	public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, RotaryCraft.MOD_ID);

	public record Def(String id, int color, int density, int viscosity, int temperature) {}

	public static final Def LUBRICANT_DEF = new Def("lubricant", 0xE4E18E, 750, 1200, 300);
	public static final Def ETHANOL_DEF = new Def("ethanol", 0x5CC5B2, 789, 950, 340);
	public static final Def JET_FUEL_DEF = new Def("jet_fuel", 0xFB5C90, 810, 800, 300);
	/** Original nitrogenFluid "rc liquid nitrogen", made by the refrigerator. */
	public static final Def LIQUID_NITROGEN_DEF = new Def("liquid_nitrogen", 0xB37ECC, 808, 158, 77);

	public static final Supplier<FluidType> LUBRICANT_TYPE = type(LUBRICANT_DEF);
	public static final Supplier<FluidType> ETHANOL_TYPE = type(ETHANOL_DEF);
	public static final Supplier<FluidType> JET_FUEL_TYPE = type(JET_FUEL_DEF);
	public static final Supplier<FluidType> LIQUID_NITROGEN_TYPE = type(LIQUID_NITROGEN_DEF);

	public static final Supplier<BaseFlowingFluid.Source> LUBRICANT = source(LUBRICANT_DEF, LUBRICANT_TYPE);
	public static final Supplier<BaseFlowingFluid.Source> ETHANOL = source(ETHANOL_DEF, ETHANOL_TYPE);
	public static final Supplier<BaseFlowingFluid.Source> JET_FUEL = source(JET_FUEL_DEF, JET_FUEL_TYPE);
	public static final Supplier<BaseFlowingFluid.Source> LIQUID_NITROGEN = source(LIQUID_NITROGEN_DEF, LIQUID_NITROGEN_TYPE);

	private RotaryFluids() {}

	private static Supplier<FluidType> type(Def d) {
		return TYPES.register(d.id(), () -> new FluidType(FluidType.Properties.create()
				.descriptionId("fluid_type.rotarycraft."+d.id()).density(d.density()).viscosity(d.viscosity()).temperature(d.temperature())));
	}

	@SuppressWarnings("unchecked")
	private static Supplier<BaseFlowingFluid.Source> source(Def d, Supplier<FluidType> type) {
		Supplier<BaseFlowingFluid.Flowing>[] flowing = new Supplier[1];
		Supplier<BaseFlowingFluid.Source>[] source = new Supplier[1];
		BaseFlowingFluid.Properties[] props = new BaseFlowingFluid.Properties[1];
		Supplier<BaseFlowingFluid.Properties> p = () -> {
			if (props[0] == null)
				props[0] = new BaseFlowingFluid.Properties(type, source[0], flowing[0]).bucket(() -> RotaryItems.bucketFor(d.id()));
			return props[0];
		};
		source[0] = FLUIDS.register(d.id(), () -> new BaseFlowingFluid.Source(p.get()));
		flowing[0] = FLUIDS.register("flowing_"+d.id(), () -> new BaseFlowingFluid.Flowing(p.get()));
		return source[0];
	}
}
