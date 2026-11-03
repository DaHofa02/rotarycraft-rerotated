/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryEntities;
import reika.rotarycraft.registry.RotaryComponents;
import reika.rotarycraft.registry.RotaryConditions;
import reika.rotarycraft.registry.RotaryCreativeTabs;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.network.RotaryNetwork;
import reika.rotarycraft.registry.RotaryItems;
import reika.rotarycraft.registry.RotaryMenus;
import reika.rotarycraft.registry.RotaryRecipeTypes;
import reika.rotarycraft.registry.RotarySounds;

/** Original: Reika.RotaryCraft.RotaryCraft (mod entry point). */
@Mod(RotaryCraft.MOD_ID)
public final class RotaryCraft {

	public static final String MOD_ID = "rotarycraft";
	public static final Logger LOGGER = LogUtils.getLogger();

	public RotaryCraft(IEventBus modBus, ModContainer container) {
		container.registerConfig(ModConfig.Type.COMMON, RotaryConfig.SPEC);

		RotaryComponents.COMPONENTS.register(modBus);
		RotaryFluids.TYPES.register(modBus);
		RotaryFluids.FLUIDS.register(modBus);
		RotaryBlocks.BLOCKS.register(modBus);
		RotaryItems.ITEMS.register(modBus);
		RotaryBlockEntities.TYPES.register(modBus);
		RotaryEntities.TYPES.register(modBus);
		RotarySounds.SOUNDS.register(modBus);
		RotaryCreativeTabs.TABS.register(modBus);
		RotaryConditions.CONDITIONS.register(modBus);
		reika.rotarycraft.content.loot.CanolaGrassSeedModifier.SERIALIZERS.register(modBus);

		RotaryMenus.MENUS.register(modBus);
		RotaryRecipeTypes.TYPES.register(modBus);
		RotaryRecipeTypes.SERIALIZERS.register(modBus);

		modBus.addListener(this::registerCapabilities);
		modBus.addListener(RotaryNetwork::register);
	}

	private void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.GEARBOX.get(), (te, side) -> te.getFluidHandler(side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.ECU.get(), (te, side) -> te.getFluidHandler(side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.BLAST_FURNACE.get(), (te, side) -> te.getAutomationHandler());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.WORKTABLE.get(), (te, side) -> te.getAutomationHandler());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.EXTRACTOR.get(), (te, side) -> te.getAutomationHandler());
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.EXTRACTOR.get(), (te, side) -> te.getFluidHandler(side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.GRINDER.get(), (te, side) -> te.getAutomationHandler());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.FERMENTER.get(), (te, side) -> te.getAutomationHandler());
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.FERMENTER.get(), (te, side) -> te.getFluidHandler(side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.CENTRIFUGE.get(), (te, side) -> te.getAutomationHandler());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.FRACTIONATOR.get(), (te, side) -> te.getAutomationHandler());
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.PUMP.get(), (te, side) -> te.getFluidHandler(side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.PULSE_JET.get(), (te, side) -> te.getFluidHandler(side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.PULSE_JET.get(), (te, side) -> te.getAutomationHandler());
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.RESERVOIR.get(), (te, side) -> te.getFluidHandler(side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.FRACTIONATOR.get(), (te, side) -> te.getFluidHandler(side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.FUEL_ENHANCER.get(), (te, side) -> te.getFluidHandler(side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.SOLAR_TOWER.get(), (te, side) -> te.getFluidHandler(side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.DRYING_BED.get(), (te, side) -> te.getFluidHandler(side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.DRYING_BED.get(), (te, side) -> te.getItemHandler(side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.WINDER.get(), (te, side) -> te.getItemHandler(side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.REFRIGERATOR.get(), (te, side) -> te.getItemHandler(side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.COMPACTOR.get(), (te, side) -> te.getItemHandler(side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.FILLING_STATION.get(), (te, side) -> te.getItemHandler(side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.FILLING_STATION.get(), (te, side) -> te.getFluidHandler(side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.REFRIGERATOR.get(), (te, side) -> te.getFluidHandler(side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.FUEL_ENHANCER.get(), (te, side) -> te.getItemHandler(side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.CENTRIFUGE.get(), (te, side) -> te.getFluidHandler(side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.GRINDER.get(), (te, side) -> te.getFluidHandler(side));
		//Original: FluidContainerRegistry.registerFluidContainer for the RotaryCraft buckets
		event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> new net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper(stack),
				RotaryItems.LUBRICANT_BUCKET.get(), RotaryItems.JET_FUEL_BUCKET.get(), RotaryItems.ETHANOL_BUCKET.get());
		//Original: TileEntityPiping is no IFluidHandler - pipes push and pull themselves, other mods cannot access them
		//Original: InertIInv, no insertion or extraction (canExtractItem false); the breaker pushes dust into neighbours itself
		for (var type : RotaryBlockEntities.engines()) {
			event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type.get(), (te, side) -> te.getEngineType().usesFluids() ? te.getFluidHandler(side) : null);
			event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type.get(), (te, side) -> te.getEngineType().hasInventory() ? te.getInventory() : null);
		}
	}
}
