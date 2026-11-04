/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.registry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import reika.reactorcraft.ReactorCraft;

/**
 * Original: the Fluid fields of ReactorCraft (addLiquids) with their density, viscosity, temperature (K) and luminosity. Like in the
 * original they have no world block; they live in tanks, pipes and containers (canisters, the heavy water bucket).
 */
public final class ReactorFluids {

	public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, ReactorCraft.MOD_ID);
	public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, ReactorCraft.MOD_ID);

	/** id, original name ("rc ..."), texture (null: vanilla water), density, viscosity, temperature, luminosity, gaseous */
	public record Def(String id, String orig, String texture, int density, int viscosity, int temperature, int light, boolean gas) {}

	public static final Map<String, Def> DEFS = new LinkedHashMap<>();
	public static final Map<String, Supplier<FluidType>> TYPE = new LinkedHashMap<>();
	public static final Map<String, Supplier<BaseFlowingFluid.Source>> SOURCE = new LinkedHashMap<>();

	static {
		def("heavy_water", "heavy water", "heavywater", 1100, 1050, 300, 0, false);
		def("hydrofluoric_acid", "hydrofluoric acid", "hf", 115, 10, 300, 0, true);
		def("uranium_hexafluoride", "uranium hexafluoride", "uf6", 15, 10, 300, 0, true);
		def("ammonia", "ammonia", "ammonia", 682, 600, 300, 0, false);
		def("sodium", "sodium", "sodium", 927, 700, 800, 0, false);
		def("chlorine", "chlorine", "chlorine", 320, 12, 300, 0, true);
		def("oxygen", "oxygen", "oxygen", 138, 20, 300, 0, true);
		def("liquid_oxygen", "liquid oxygen", "oxygen", 1141, 195, 90, 0, false);
		def("deuterium", "deuterium", "deuterium", -1, 10, 300, 0, true);
		def("tritium", "tritium", "tritium", -1, 10, 300, 0, true);
		def("fusion_plasma", "fusion plasma", "plasma", -1, 100, 150000000, 15, true);
		def("low_pressure_ammonia", "lowpammonia", "ammonia", 200, 600, 300, 0, false);
		def("low_pressure_water", "lowpwater", null, 800, 800, 300, 0, false);
		def("hot_sodium", "hotsodium", "sodiumhot", 720, 650, 2000, 8, false);
		def("warm_sodium", "warmsodium", "sodiumhot", 864, 650, 1100, 6, false);
		def("co2", "co2", "co2", 2, 7, 300, 0, true);
		def("hot_co2", "hot co2", "co2", 1, 5, 300, 2, true);
		def("corium", "corium", "slag_flow", 5000, 8000, 2173, 0, false);
		def("nuclear_waste", "nuclear waste", "slag_flow", 4000, 12000, 800, 0, false);
		def("lithium", "lithium", "lithium", 516, 645, 454, 6, false);
		def("lifbe", "lifbe", "lifbe", 6300, 2400, 773, 0, false);
		def("lifbe_fuel", "lifbe fuel", "lifbe_fuel", 6750, 2550, 473, 0, false);
		def("lifbe_fuel_preheat", "lifbe fuel preheat", "lifbe_fuel", 6700, 2125, 673, 0, false);
		def("hot_lifbe", "hot lifbe", "lifbe_hot", 6000, 2400, 1273, 8, false);
	}

	private ReactorFluids() {}

	private static void def(String id, String orig, String tex, int density, int viscosity, int temp, int light, boolean gas) {
		Def d = new Def(id, orig, tex, density, viscosity, temp, light, gas);
		DEFS.put(id, d);
		Supplier<FluidType> type = TYPES.register(id, () -> new FluidType(FluidType.Properties.create().descriptionId("fluid_type.reactorcraft."+id)
				.density(density).viscosity(viscosity).temperature(temp).lightLevel(light)));
		TYPE.put(id, type);
		SOURCE.put(id, source(id, type));
	}

	@SuppressWarnings("unchecked")
	private static Supplier<BaseFlowingFluid.Source> source(String id, Supplier<FluidType> type) {
		Supplier<BaseFlowingFluid.Flowing>[] flowing = new Supplier[1];
		Supplier<BaseFlowingFluid.Source>[] source = new Supplier[1];
		BaseFlowingFluid.Properties[] props = new BaseFlowingFluid.Properties[1];
		Supplier<BaseFlowingFluid.Properties> p = () -> {
			if (props[0] == null) {
				props[0] = new BaseFlowingFluid.Properties(type, source[0], flowing[0]);
				if (id.equals("heavy_water"))
					props[0].bucket(() -> (Item)ReactorItems.HEAVY_WATER_BUCKET.get());
				if (id.equals("corium")) //original BlockCoriumFlowing: a world fluid, Forge tick rate viscosity/200
					props[0].block(() -> (net.minecraft.world.level.block.LiquidBlock)ReactorBlocks.CORIUM.get()).tickRate(40).explosionResistance(reika.rotarycraft.lib.LegacyResistance.of(500F));
			}
			return props[0];
		};
		boolean corium = id.equals("corium");
		source[0] = FLUIDS.register(id, () -> corium ? new reika.reactorcraft.content.fluid.CoriumFluid.Source(p.get()) : new BaseFlowingFluid.Source(p.get()));
		flowing[0] = FLUIDS.register("flowing_"+id, () -> corium ? new reika.reactorcraft.content.fluid.CoriumFluid.Flowing(p.get()) : new BaseFlowingFluid.Flowing(p.get()));
		return source[0];
	}

	public static Fluid get(String id) {
		return SOURCE.get(id).get();
	}
}
