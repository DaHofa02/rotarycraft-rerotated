/*******************************************************************************
 * Optional Just Enough Items integration of the private NeoForge 1.21.1 port of ReactorCraft (by Reika Kalseki); the original listed
 * these machines in NEI. Only loaded with JEI.
 ******************************************************************************/
package reika.reactorcraft.compat.jei;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.JeiPlugin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.blockentity.SynthesizerBlockEntity;
import reika.reactorcraft.content.blockentity.UProcessorBlockEntity;
import reika.reactorcraft.fusion.FusionHeaterBlockEntity;
import reika.reactorcraft.reactor.TritizerBlockEntity;
import reika.reactorcraft.registry.FluoriteTypes;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorFluids;
import reika.reactorcraft.registry.ReactorItems;
import reika.rotarycraft.compat.jei.MachinePlugin;
import reika.rotarycraft.compat.jei.MachineRecipe;
import reika.rotarycraft.compat.jei.RotaryJeiPlugin;
import reika.rotarycraft.content.item.CraftingPart;

@JeiPlugin
public class ReactorJeiPlugin extends MachinePlugin {

	private static ItemStack raw(ReactorItems.Raw r) {
		return new ItemStack(ReactorItems.raw(r).get());
	}

	private static FluidStack fl(String id, int amt) {
		return new FluidStack(ReactorFluids.get(id), amt);
	}

	public ReactorJeiPlugin() {
		category(ReactorCraft.MOD_ID, "processor", () -> new ItemStack(ReactorBlocks.PROCESSOR.get()), 2, () -> {
			List<MachineRecipe> li = new ArrayList<>();
			List<ItemStack> fluorite = new ArrayList<>();
			for (FluoriteTypes f : FluoriteTypes.colorList)
				fluorite.add(new ItemStack(ReactorItems.fluorite(f).get()));
			for (UProcessorBlockEntity.Process p : UProcessorBlockEntity.Process.values()) {
				li.add(MachineRecipe.builder().in(fluorite).fluidIn(new FluidStack(p.inputFluid(), p.inputFluidConsumed))
						.fluidOut(new FluidStack(p.intermediateFluid(), p.intermediateFluidProduced)).line("Step 1: "+p.intermediateTime+" ticks, no power").build());
				List<ItemStack> items = new ArrayList<>();
				if (p == UProcessorBlockEntity.Process.UF6)
					items.add(new ItemStack(ReactorItems.ingot(ReactorItems.Ingot.URANIUM).get()));
				else
					items.add(raw(ReactorItems.Raw.EMERALDDUST));
				li.add(MachineRecipe.builder().in(items).fluidIn(new FluidStack(p.intermediateFluid(), p.intermediateFluidConsumed))
						.fluidOut(new FluidStack(p.outputFluid(), p.outputFluidProduced)).line("Step 2: "+p.outputTime+" ticks, no power").build());
			}
			return li;
		});
		category(ReactorCraft.MOD_ID, "synthesizer", () -> new ItemStack(ReactorBlocks.SYNTHESIZER.get()), 2, () -> List.of(
				MachineRecipe.builder().in(raw(ReactorItems.Raw.LIME)).in(raw(ReactorItems.Raw.AMMONIUM)).fluidIn(new FluidStack(Fluids.WATER, 250))
						.fluidOut(fl("ammonia", 1000)).line("From "+SynthesizerBlockEntity.AMMONIATEMP+" °C, 50 ticks").line("Heat: surroundings or friction heater").build(),
				MachineRecipe.builder().fluidIn(fl("lifbe_fuel", 50)).fluidOut(fl("lifbe_fuel_preheat", 50)).line("From 350 °C, 100 ticks (faster when hotter)").build()));
		category(ReactorCraft.MOD_ID, "isocentrifuge", () -> new ItemStack(ReactorBlocks.ISO_CENTRIFUGE.get()), 2, () -> List.of(
				MachineRecipe.builder().fluidIn(fl("uranium_hexafluoride", 50)).out(raw(ReactorItems.Raw.FUELDUST), "9%").out(raw(ReactorItems.Raw.DEPDUST), "91%")
						.line("900 ticks (8 from 67108864 rad/s)").line(RotaryJeiPlugin.power(1, 262144, 1)+", from below").build()));
		category(ReactorCraft.MOD_ID, "electrolyzer", () -> new ItemStack(ReactorBlocks.ELECTROLYZER.get()), 2, () -> List.of(
				MachineRecipe.builder().in(RotaryJeiPlugin.part(CraftingPart.SALT)).fluidOut(fl("chlorine", 100)).fluidOut(fl("sodium", 100))
						.line("From 801 °C; Van de Graaff discharges").line("(4096 C or more) drive it").build(),
				MachineRecipe.builder().fluidIn(fl("heavy_water", 100)).fluidOut(fl("deuterium", 100)).fluidOut(fl("oxygen", 50))
						.line("Van de Graaff discharges (4096 C or more)").line("drive it").build()));
		category(ReactorCraft.MOD_ID, "tritizer", () -> new ItemStack(ReactorBlocks.TRITIZER.get()), 1, () -> {
			List<MachineRecipe> li = new ArrayList<>();
			for (TritizerBlockEntity.Reactions r : TritizerBlockEntity.Reactions.values())
				li.add(MachineRecipe.builder().fluidIn(new FluidStack(r.getInput(), r.amount)).fluidOut(new FluidStack(r.getOutput(), r.amount))
						.line("Per neutron: "+r.chance+" % chance").build());
			return li;
		});
		category(ReactorCraft.MOD_ID, "fluid_extractor", () -> new ItemStack(ReactorBlocks.FLUID_EXTRACTOR.get()), 2, () -> List.of(
				MachineRecipe.builder().fluidIn(new FluidStack(Fluids.WATER, 1000)).fluidOut(fl("heavy_water", 200))
						.line("Ocean floor below y=45, 16 deep").line(RotaryJeiPlugin.power(512, 1, 65536)).build(),
				MachineRecipe.builder().fluidIn(new FluidStack(Fluids.LAVA, 1000)).fluidOut(fl("lithium", 40))
						.line("10-40 mB at the lava surface (y=-54, nether 31)").line(RotaryJeiPlugin.power(512, 1, 65536)).build()));
		category(ReactorCraft.MOD_ID, "fusion_heater", () -> new ItemStack(ReactorBlocks.FUSION_HEATER.get()), 2, () -> List.of(
				MachineRecipe.builder().fluidIn(fl("deuterium", 50)).fluidIn(fl("tritium", 50)).fluidOut(fl("fusion_plasma", 100))
						.line("In its multiblock, from "+FusionHeaterBlockEntity.PLASMA_TEMP/1000000+" million degrees").line("Heated by a heat ray through the lens").build()));
	}

	@Override
	public void registerCategories(mezz.jei.api.registration.IRecipeCategoryRegistration reg) {
		super.registerCategories(reg);
		reg.addRecipeCategories(new MultiblockCategory(reg.getJeiHelpers().getGuiHelper()));
	}

	@Override
	public void registerRecipes(mezz.jei.api.registration.IRecipeRegistration reg) {
		super.registerRecipes(reg);
		reg.addRecipes(MultiblockCategory.TYPE, reika.reactorcraft.structure.Blueprints.all());
	}

	@Override
	public void registerRecipeCatalysts(mezz.jei.api.registration.IRecipeCatalystRegistration reg) {
		super.registerRecipeCatalysts(reg);
		for (var bp : reika.reactorcraft.structure.Blueprints.all())
			reg.addRecipeCatalyst(new ItemStack(bp.machine()), MultiblockCategory.TYPE);
	}

	@Override
	public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime rt) {
		JeiShowcase.runtime = rt;
		if (JeiShowcase.ACTIVE)
			net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post e) -> JeiShowcase.tick());
	}

	@Override
	public ResourceLocation getPluginUid() {
		return ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "jei");
	}
}
