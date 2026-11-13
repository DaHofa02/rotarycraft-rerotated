/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.registries.DeferredRegister;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.block.FlywheelBlock;
import reika.rotarycraft.content.block.GearboxBlock;
import reika.rotarycraft.content.block.ShaftBlock;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.GearboxTypes.GearPart;

/** Original: the RotaryCraft creative tabs (machines, items, parts). Sub-variants are listed like the original getSubItems. */
public final class RotaryCreativeTabs {

	public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RotaryCraft.MOD_ID);

	public static final Supplier<CreativeModeTab> POWER = TABS.register("power", () -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup.rotarycraft.power"))
			.icon(() -> GearboxBlock.gearboxItem(GearboxTypes.STEEL, 8))
			.displayItems((params, out) -> {
				for (MaterialRegistry m : MaterialRegistry.matList)
					out.accept(ShaftBlock.shaftItem(m, false));
				out.accept(ShaftBlock.shaftItem(MaterialRegistry.STEEL, true));
				for (GearboxTypes g : GearboxTypes.typeList)
					for (int r = 2; r <= 16; r *= 2)
						out.accept(GearboxBlock.gearboxItem(g, r));
				for (Flywheels f : Flywheels.list)
					out.accept(FlywheelBlock.flywheelItem(f));
				out.accept(RotaryBlocks.BEVEL_GEARS.get());
				out.accept(RotaryBlocks.CLUTCH.get());
				out.accept(RotaryBlocks.DYNAMOMETER.get());
				out.accept(RotaryBlocks.COOLING_FIN.get());
				out.accept(RotaryBlocks.FUEL_ENHANCER.get());
				out.accept(RotaryBlocks.MIRROR.get());
				out.accept(RotaryBlocks.SOLAR_TOWER.get());
				out.accept(RotaryBlocks.PIPE_PUMP.get());
				out.accept(RotaryBlocks.VAN_DE_GRAFF.get());
				out.accept(RotaryBlocks.DRYING_BED.get());
				out.accept(RotaryBlocks.WINDER.get());
				out.accept(RotaryBlocks.REFRIGERATOR.get());
				out.accept(RotaryBlocks.HEAT_RAY.get());
				out.accept(RotaryBlocks.COMPACTOR.get());
				out.accept(RotaryBlocks.FILLING_STATION.get());
				for (int i = 0; i < reika.rotarycraft.registry.RotaryItems.GEAR_UPGRADES.size(); i++) { //original getSubItems: empty, and filled with lubricant or nitrogen
					var item = reika.rotarycraft.registry.RotaryItems.GEAR_UPGRADES.get(i).get();
					out.accept(item);
					if (i > 0) {
						out.accept(reika.rotarycraft.content.item.IntegratedGearboxItem.getIntegratedGearItem(1 << i, reika.rotarycraft.registry.RotaryFluids.LUBRICANT.get()));
						out.accept(reika.rotarycraft.content.item.IntegratedGearboxItem.getIntegratedGearItem(1 << i, reika.rotarycraft.registry.RotaryFluids.LIQUID_NITROGEN.get()));
					}
				}
				out.accept(RotaryBlocks.SHAFT_JUNCTION.get());
				for (var e : java.util.List.of(RotaryBlocks.DC_ENGINE, RotaryBlocks.WIND_ENGINE, RotaryBlocks.STEAM_ENGINE, RotaryBlocks.GAS_ENGINE, RotaryBlocks.AC_ENGINE, RotaryBlocks.SPORT_ENGINE, RotaryBlocks.MICRO_ENGINE, RotaryBlocks.HYDRO_ENGINE, RotaryBlocks.JET_ENGINE))
					out.accept(e.get());
				out.accept(RotaryBlocks.ECU.get());
				out.accept(RotaryBlocks.BLAST_FURNACE.get());
				out.accept(RotaryBlocks.WORKTABLE.get());
				out.accept(RotaryBlocks.FRICTION_HEATER.get());
				out.accept(RotaryBlocks.BEDROCK_BREAKER.get());
				out.accept(RotaryBlocks.BORER.get());
				out.accept(RotaryBlocks.EXTRACTOR.get());
				out.accept(RotaryBlocks.GRINDER.get());
				out.accept(RotaryBlocks.FERMENTER.get());
				out.accept(RotaryBlocks.FAN.get());
				out.accept(RotaryBlocks.MOB_HARVESTER.get());
				out.accept(RotaryBlocks.AGGREGATOR.get());
				out.accept(RotaryBlocks.SPILLER.get());
				out.accept(RotaryBlocks.FILLER.get());
				out.accept(RotaryBlocks.SMOKE_DETECTOR.get());
				out.accept(RotaryBlocks.PLAYER_DETECTOR.get());
				out.accept(RotaryBlocks.MOB_RADAR.get());
				out.accept(RotaryBlocks.LAVA_MAKER.get());
				out.accept(RotaryBlocks.HEATER.get());
				out.accept(RotaryBlocks.COMPOSTER.get());
				out.accept(RotaryBlocks.CRYSTALLIZER.get());
				out.accept(RotaryBlocks.LAMP.get());
				out.accept(RotaryBlocks.HYDRATOR.get());
				out.accept(RotaryBlocks.WETTER.get());
				out.accept(RotaryBlocks.PARTICLE_EMITTER.get());
				out.accept(RotaryBlocks.MULTI_CLUTCH.get());
				out.accept(RotaryBlocks.BIG_FURNACE.get());
				out.accept(RotaryBlocks.SPILLWAY.get());
				out.accept(RotaryBlocks.FLUID_COMPRESSOR.get());
				out.accept(RotaryBlocks.LINE_BUILDER.get());
				out.accept(RotaryBlocks.MAGNETIZER.get());
				out.accept(RotaryBlocks.BEAM_MIRROR.get());
				out.accept(RotaryBlocks.ITEM_REFRESHER.get());
				out.accept(RotaryBlocks.GRINDSTONE.get());
				out.accept(RotaryBlocks.BUCKET_FILLER.get());
				out.accept(RotaryBlocks.SORTING.get());
				out.accept(RotaryBlocks.FERTILIZER.get());
				out.accept(RotaryBlocks.LAWN_SPRINKLER.get());
				out.accept(RotaryBlocks.CENTRIFUGE.get());
				out.accept(RotaryBlocks.FRACTIONATOR.get());
				out.accept(RotaryBlocks.PUMP.get());
				out.accept(RotaryBlocks.RESERVOIR.get());
				out.accept(RotaryBlocks.PULSE_JET.get());
				out.accept(RotaryBlocks.BLAST_GLASS.get());
				//Original BlockDeco (tabRotary, metadata order)
				for (var b : java.util.List.of(RotaryBlocks.STEEL_BLOCK, RotaryBlocks.ANTHRACITE_BLOCK, RotaryBlocks.LONSDALEITE_BLOCK, RotaryBlocks.SHIELD_BLOCK,
						RotaryBlocks.BEDROCK_INGOT_BLOCK, RotaryBlocks.COKE_BLOCK))
					out.accept(b.get());
				out.accept(RotaryBlocks.HOSE.get());
				out.accept(RotaryBlocks.PIPE.get());
				out.accept(RotaryBlocks.FUEL_LINE.get());
				out.accept(RotaryBlocks.BEDROCK_PIPE.get());
				out.accept(RotaryBlocks.VALVE.get());
				out.accept(RotaryBlocks.BYPASS.get());
				out.accept(RotaryBlocks.SEPARATION.get());
				out.accept(RotaryBlocks.SUCTION.get());
				ItemStack bedrockJunction = new ItemStack(RotaryBlocks.SHAFT_JUNCTION.get());
				bedrockJunction.set(RotaryComponents.BEDROCK_JUNCTION.get(), true);
				out.accept(bedrockJunction);
			}).build());

	public static final Supplier<CreativeModeTab> PARTS = TABS.register("parts", () -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup.rotarycraft.parts"))
			.icon(() -> new ItemStack(RotaryItems.gearPart(GearboxTypes.STEEL, GearPart.GEAR)))
			.displayItems((params, out) -> {
				for (CraftingPart p : CraftingPart.values())
					out.accept(RotaryItems.part(p));
				for (GearboxTypes g : GearboxTypes.typeList)
					for (GearPart p : GearPart.list)
						out.accept(RotaryItems.gearPart(g, p));
				for (Flywheels f : Flywheels.list)
					out.accept(RotaryItems.flywheelCore(f));
				out.accept(RotaryItems.YEAST.get());
				out.accept(RotaryItems.ETHANOL.get());
				out.accept(RotaryItems.CANOLA_SEEDS.get());
				out.accept(RotaryItems.DENSE_CANOLA_SEEDS.get());
				out.accept(RotaryItems.CANOLA_HUSKS.get());
			}).build());

	public static final Supplier<CreativeModeTab> TOOLS = TABS.register("tools", () -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup.rotarycraft.tools"))
			.icon(() -> new ItemStack(RotaryItems.SCREWDRIVER.get()))
			.displayItems((params, out) -> {
				out.accept(RotaryItems.SCREWDRIVER.get());
				//original ItemCoil.getSubItems: empty, every power of two up to 32000, and 30000 kJ
				for (var coil : new net.neoforged.neoforge.registries.DeferredItem[] {RotaryItems.SPRING, RotaryItems.STRONGCOIL})
					for (int i = 0; i <= 32000; i++)
						if (i == 0 || i == 30000 || Integer.bitCount(i) == 1)
							out.accept(reika.rotarycraft.content.item.CoilItem.withCharge(new ItemStack((net.minecraft.world.item.Item)coil.get()), i));
				out.accept(RotaryItems.METER.get());
				out.accept(RotaryItems.UPGRADE_PERFORMANCE.get());
				out.accept(RotaryItems.UPGRADE_AFTERBURNER.get());
				out.accept(RotaryItems.UPGRADE_REDSTONE.get());
				out.accept(RotaryItems.LUBRICANT_BUCKET.get());
				out.accept(RotaryItems.JET_FUEL_BUCKET.get());
				out.accept(RotaryItems.ETHANOL_BUCKET.get());
			}).build());

	private RotaryCreativeTabs() {}
}
