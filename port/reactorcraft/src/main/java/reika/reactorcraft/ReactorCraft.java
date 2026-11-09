package reika.reactorcraft;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.reactorcraft.registry.FluoriteTypes;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorItems;
import reika.reactorcraft.registry.ReactorOres;
import reika.reactorcraft.registry.ReactorWorldGen;

/** Original: Reika.ReactorCraft.ReactorCraft. Private NeoForge 1.21.1 port by way of the RotaryCraft port; not for distribution. */
@Mod(ReactorCraft.MOD_ID)
public final class ReactorCraft {

	public static final String MOD_ID = "reactorcraft";
	public static final Logger LOGGER = LogUtils.getLogger();

	public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

	/** Original tabRctrItems ("ReactorCraft Items"; its icon, nuclear waste, is not ported yet). */
	static {
		TABS.register("items", () -> CreativeModeTab.builder()
				.title(Component.translatable("itemGroup.reactorcraft_items"))
				.icon(() -> new ItemStack(ReactorItems.ingot(ReactorItems.Ingot.URANIUM).get()))
				.displayItems((params, out) -> {
					out.accept(ReactorBlocks.FUEL_ROD.get());
					out.accept(ReactorBlocks.WATER_CELL.get());
					out.accept(ReactorBlocks.CONTROL_ROD.get());
					out.accept(ReactorBlocks.CPU.get());
					out.accept(ReactorBlocks.BOILER.get());
					out.accept(ReactorBlocks.BREEDER_CORE.get());
					out.accept(ReactorBlocks.SODIUM_HEATER.get());
					out.accept(ReactorBlocks.PEBBLE_BED.get());
					out.accept(ReactorBlocks.CO2_HEATER.get());
					out.accept(ReactorBlocks.THORIUM_CORE.get());
					out.accept(ReactorBlocks.FUEL_DUMP.get());
					out.accept(ReactorBlocks.WASTE_PIPE.get());
					out.accept(ReactorBlocks.THORIUM_FUEL.get());
					out.accept(ReactorBlocks.STEAM_LINE.get());
					out.accept(ReactorBlocks.HEAT_PIPE.get());
					out.accept(ReactorBlocks.STEAM_GRATE.get());
					out.accept(ReactorBlocks.TURBINE_CORE.get());
					out.accept(ReactorBlocks.CONDENSER.get());
					out.accept(ReactorBlocks.HIP_TURBINE.get());
					out.accept(ReactorBlocks.TURBINE_METER.get());
					out.accept(ReactorBlocks.STEAM_DIFFUSER.get());
					out.accept(ReactorBlocks.PRESSURIZER.get());
					out.accept(ReactorBlocks.TURBINE_BLADES.get());
					out.accept(ReactorBlocks.TURBINE_HOUSING.get());
					out.accept(ReactorBlocks.STEAM_INJECTOR.get());
					out.accept(ReactorBlocks.TRITIZER.get());
					out.accept(ReactorBlocks.SOLAR_EXCHANGER.get());
					out.accept(ReactorBlocks.SOLAR_TOP.get());
					out.accept(ReactorBlocks.GENERATOR.get());
					out.accept(ReactorBlocks.GENERATOR_CORE.get());
					out.accept(ReactorBlocks.GENERATOR_WINDING.get());
					out.accept(ReactorBlocks.GENERATOR_HOUSING.get());
					out.accept(ReactorBlocks.STEAM_BYPASS.get());
					out.accept(ReactorBlocks.FLYWHEEL.get());
					out.accept(ReactorBlocks.FLYWHEEL_CORE.get());
					out.accept(ReactorBlocks.FLYWHEEL_DAMPENER.get());
					out.accept(ReactorBlocks.FLYWHEEL_FRAME.get());
					out.accept(ReactorBlocks.HEAT_EXCHANGER.get());
					out.accept(ReactorBlocks.WASTE_CONTAINER.get());
					out.accept(ReactorBlocks.WASTE_STORAGE.get());
					out.accept(ReactorBlocks.WASTE_DECAYER.get());
					out.accept(ReactorBlocks.FLUID_EXTRACTOR.get());
					out.accept(ReactorBlocks.PROCESSOR.get());
					out.accept(ReactorBlocks.SYNTHESIZER.get());
					out.accept(ReactorBlocks.ELECTROLYZER.get());
					out.accept(ReactorBlocks.ISO_CENTRIFUGE.get());
					out.accept(ReactorBlocks.TOROID_MAGNET.get());
					out.accept(ReactorBlocks.SOLENOID.get());
					out.accept(ReactorBlocks.FUSION_INJECTOR.get());
					out.accept(ReactorBlocks.FUSION_HEATER.get());
					out.accept(ReactorBlocks.NEUTRON_ABSORBER.get());
					out.accept(ReactorBlocks.NEUTRON_REFLECTOR.get());
					out.accept(ReactorBlocks.MAGNETIC_PIPE.get());
					out.accept(ReactorBlocks.GAS_DUCT.get());
					out.accept(ReactorBlocks.GAS_COLLECTOR.get());
					out.accept(ReactorBlocks.FUSION_MARKER.get());
					for (var b : ReactorBlocks.SOLENOID_MULTI)
						out.accept(b.get());
					for (var b : ReactorBlocks.INJECTOR_MULTI)
						out.accept(b.get());
					for (var b : ReactorBlocks.HEATER_MULTI)
						out.accept(b.get());
					for (ReactorOres ore : ReactorOres.oreList)
						if (ore != ReactorOres.FLUORITE)
							out.accept(ReactorBlocks.ore(ore).get());
					for (FluoriteTypes f : FluoriteTypes.colorList)
						out.accept(ReactorBlocks.fluoriteOre(f).get());
					for (FluoriteTypes f : FluoriteTypes.colorList)
						out.accept(ReactorBlocks.fluoriteBlock(f).get());
					out.accept(ReactorBlocks.CALCITE_BLOCK.get());
					out.accept(ReactorBlocks.CONCRETE.get());
					out.accept(ReactorBlocks.GRAPHITE_BLOCK.get());
					for (ReactorItems.Crafting c : ReactorItems.Crafting.values())
						out.accept(ReactorItems.crafting(c).get());
					for (ReactorItems.Raw r : ReactorItems.Raw.values())
						out.accept(ReactorItems.raw(r).get());
					for (FluoriteTypes f : FluoriteTypes.colorList)
						out.accept(ReactorItems.fluorite(f).get());
					for (ReactorItems.Ingot i : ReactorItems.Ingot.values())
						out.accept(ReactorItems.ingot(i).get());
					out.accept(ReactorItems.HEAVY_WATER_BUCKET.get());
					out.accept(ReactorItems.FUEL.get());
					out.accept(ReactorItems.DEPLETED.get());
					out.accept(ReactorItems.PLUTONIUM.get());
					out.accept(ReactorItems.BREEDER_FUEL.get());
					out.accept(ReactorItems.GOGGLES.get());
					out.accept(ReactorItems.HAZ_HELMET.get());
					out.accept(ReactorItems.HAZ_CHEST.get());
					out.accept(ReactorItems.HAZ_LEGS.get());
					out.accept(ReactorItems.HAZ_BOOTS.get());
					out.accept(ReactorItems.GEIGER.get()); //original getSubItems: uncharged and charged (32000 kJ)
					out.accept(reika.reactorcraft.content.item.GeigerCounterItem.charged(ReactorItems.GEIGER.get(), reika.reactorcraft.content.item.GeigerCounterItem.CREATIVE_CHARGE));
					out.accept(ReactorItems.CLEANER.get()); //original getSubItems: empty and charged with full water
					out.accept(reika.reactorcraft.content.item.RadiationCleanerItem.charged(ReactorItems.CLEANER.get()));
					out.accept(ReactorItems.IRON_FINDER.get());
					out.accept(ReactorItems.REMOTE.get()); //original getSubItems: uncharged and charged (32000 kJ)
					out.accept(reika.reactorcraft.content.item.GeigerCounterItem.charged(ReactorItems.REMOTE.get(), reika.reactorcraft.content.item.RemoteControlItem.CREATIVE_CHARGE));
					out.accept(ReactorItems.PELLET.get());
					out.accept(ReactorItems.OLD_PELLET.get());
					for (var iso : reika.reactorcraft.radiation.WasteManager.getWasteList()) //original getSubItems: the waste isotopes
						out.accept(reika.reactorcraft.content.item.NuclearWasteItem.of(iso));
					for (var m : ReactorItems.MAGNETS)
						out.accept(m.get());
					out.accept(ReactorItems.EMPTY_CANISTER.get());
					for (var c : ReactorItems.filledCanisters())
						out.accept(c.get());
				}).build());
	}

	public ReactorCraft(IEventBus modBus, ModContainer container) {
		ReactorBlocks.BLOCKS.register(modBus);
		ReactorItems.ITEMS.register(modBus);
		ReactorItems.ARMOR_MATERIALS.register(modBus);
		ReactorWorldGen.PLACEMENTS.register(modBus);
		reika.reactorcraft.registry.ReactorBlockEntities.TYPES.register(modBus);
		reika.reactorcraft.registry.ReactorComponents.COMPONENTS.register(modBus);
		reika.reactorcraft.registry.ReactorEffects.EFFECTS.register(modBus);
		reika.reactorcraft.registry.ReactorEntities.TYPES.register(modBus);
		reika.reactorcraft.registry.ReactorMenus.MENUS.register(modBus);
		reika.reactorcraft.registry.ReactorSounds.SOUNDS.register(modBus);
		modBus.addListener(reika.reactorcraft.network.ReactorPayloads::register);
		reika.reactorcraft.registry.ReactorFluids.TYPES.register(modBus);
		reika.reactorcraft.registry.ReactorFluids.FLUIDS.register(modBus);
		modBus.addListener(ReactorCraft::registerCapabilities);
		modBus.addListener(ReactorCraft::commonSetup);
		modBus.addListener(reika.reactorcraft.reactor.ReactorChunkLoading::register);
		TABS.register(modBus);
		container.registerConfig(ModConfig.Type.COMMON, ReactorConfig.SPEC);
		//original ReactorRecipes: TileEntityFuelConverter.addRecipe("rc lifbe", "rc lifbe fuel", 10, 1, 0.1, new ItemMatch("dustThorium"))
		reika.rotarycraft.content.blockentity.FuelEnhancerBlockEntity.addRecipe(() -> reika.reactorcraft.registry.ReactorFluids.get("lifbe"), () -> reika.reactorcraft.registry.ReactorFluids.get("lifbe_fuel"), 10, 1, 0.1,
				net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "dusts/thorium"))));
	}

	/** Original ReactorRecipes: RecipesCompactor lodestone -> 2 magnets (5000 kPa, 100 C), each magnet -> 2 of the next (10000*(1+level) kPa). */
	private static void commonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent e) {
		e.enqueueWork(() -> {
			reika.rotarycraft.content.blockentity.CompactorRecipes.add(() -> new ItemStack(ReactorItems.raw(ReactorItems.Raw.MAGNETITE).get()),
					() -> new ItemStack(ReactorItems.MAGNETS.get(0).get(), 2), 5000, 100);
			for (int i = 0; i < ReactorItems.MAGNETS.size()-1; i++) {
				int lvl = i;
				reika.rotarycraft.content.blockentity.CompactorRecipes.add(() -> new ItemStack(ReactorItems.MAGNETS.get(lvl).get()),
						() -> new ItemStack(ReactorItems.MAGNETS.get(lvl+1).get(), 2), 10000*(1+lvl), 100);
			}
		});
	}

	private static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent e) {
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.TOROID_MAGNET.get(),
				(te, side) -> te.getFluidHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.FUSION_HEATER.get(),
				(te, side) -> te.getFluidHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.GAS_COLLECTOR.get(),
				(te, side) -> te.getFluidHandler(side));
		e.registerItem(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM, (stack, ctx) -> new reika.reactorcraft.content.item.CanisterItem.Handler(stack),
				ReactorItems.EMPTY_CANISTER.get());
		for (var c : ReactorItems.filledCanisters())
			e.registerItem(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM, (stack, ctx) -> new reika.reactorcraft.content.item.CanisterItem.Handler(stack), c.get());
		e.registerItem(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM,
				(stack, ctx) -> new net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper(stack), ReactorItems.HEAVY_WATER_BUCKET.get());
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.THORIUM_CORE.get(),
				(te, side) -> te.getFluidHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.CO2_HEATER.get(),
				(te, side) -> te.getFluidHandler(side));
		//original TileEntityFuelDump is a fluid handler (TileEntityTankedReactorMachine) whose drains reach the core above; without the
		//capability RotaryCraft pipes never interact with it
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.FUEL_DUMP.get(),
				(te, side) -> te.getFluidHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.PEBBLE_BED.get(),
				(te, side) -> te.getItemHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.SODIUM_HEATER.get(),
				(te, side) -> te.getFluidHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.BREEDER_CORE.get(),
				(te, side) -> te.getItemHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.HEAT_EXCHANGER.get(),
				(te, side) -> te.getFluidHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.CONDENSER.get(),
				(te, side) -> te.getFluidHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.STEAM_DIFFUSER.get(),
				(te, side) -> te.getFluidHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.PRESSURIZER.get(),
				(te, side) -> te.getFluidHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.BOILER.get(),
				(te, side) -> te.getFluidHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.SOLAR_EXCHANGER.get(),
				(te, side) -> te.getFluidHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.TRITIZER.get(),
				(te, side) -> te.getFluidHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.GENERATOR.get(),
				(te, side) -> te.getEnergyStorage(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.HIP_TURBINE.get(),
				(te, side) -> te.getFluidHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.STEAM_INJECTOR.get(),
				(te, side) -> te.getFluidHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.TURBINE_CORE.get(),
				(te, side) -> te.getFluidHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.HEAVY_PUMP.get(),
				(te, side) -> te.getFluidHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.PROCESSOR.get(),
				(te, side) -> te.getFluidHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.WASTE_CONTAINER.get(),
				(te, side) -> te.getItemHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.WASTE_STORAGE.get(),
				(te, side) -> te.getItemHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.WASTE_DECAYER.get(),
				(te, side) -> te.getItemHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.PROCESSOR.get(),
				(te, side) -> te.getItemHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.ISO_CENTRIFUGE.get(),
				(te, side) -> te.getFluidHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.ISO_CENTRIFUGE.get(),
				(te, side) -> te.getItemHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.FUEL_ROD.get(),
				(te, side) -> te.getItemHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.ELECTROLYZER.get(),
				(te, side) -> te.getFluidHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.ELECTROLYZER.get(),
				(te, side) -> te.getItemHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.SYNTHESIZER.get(),
				(te, side) -> te.getFluidHandler(side));
		e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, reika.reactorcraft.registry.ReactorBlockEntities.SYNTHESIZER.get(),
				(te, side) -> te.getItemHandler(side));
	}
}
