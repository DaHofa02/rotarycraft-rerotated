/*******************************************************************************
 * Optional Just Enough Items integration of the private NeoForge 1.21.1 port of RotaryCraft (by Reika Kalseki). Only loaded with JEI
 * (the original used NEI handlers such as FridgeHandler, FillingStationHandler, CompactorHandler; the values shown are the port's).
 ******************************************************************************/
package reika.rotarycraft.compat.jei;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.CompactorBlockEntity;
import reika.rotarycraft.content.blockentity.CompactorRecipes;
import reika.rotarycraft.content.blockentity.ExtractorBlockEntity;
import reika.rotarycraft.content.blockentity.RefrigeratorBlockEntity;
import reika.rotarycraft.content.extractor.ExtractorOres;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.content.recipe.BlastCraftingRecipe;
import reika.rotarycraft.content.recipe.BlastFurnaceRecipe;
import reika.rotarycraft.content.recipe.CentrifugeRecipe;
import reika.rotarycraft.content.recipe.FrictionHeaterRecipe;
import reika.rotarycraft.content.recipe.GrinderRecipe;
import reika.rotarycraft.content.recipe.PulseJetRecipe;
import reika.rotarycraft.content.recipe.WorktableRecipe;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryDataMaps;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;
import reika.rotarycraft.registry.RotaryRecipeTypes;

@JeiPlugin
public class RotaryJeiPlugin extends MachinePlugin {

	public static ItemStack st(ItemLike i) {
		return new ItemStack(i);
	}

	public static ItemStack part(CraftingPart p) {
		return new ItemStack(RotaryItems.part(p));
	}

	public static String power(int torque, int speed, long power) {
		StringBuilder sb = new StringBuilder("Min:");
		if (torque > 1)
			sb.append(" ").append(torque).append(" Nm");
		if (speed > 1)
			sb.append(" ").append(speed).append(" rad/s");
		if (power > 1)
			sb.append(" ").append(power >= 1000000 ? String.format("%.2f MW", power/1e6) : power >= 1000 ? String.format("%.1f kW", power/1e3) : power+" W");
		return sb.toString();
	}

	private static RecipeManager rm() {
		return Minecraft.getInstance().level.getRecipeManager();
	}

	private static RegistryAccess ra() {
		return Minecraft.getInstance().level.registryAccess();
	}

	public RotaryJeiPlugin() {
		category(RotaryCraft.MOD_ID, "worktable", () -> st(RotaryBlocks.WORKTABLE.get()), 1, () -> {
			List<MachineRecipe> li = new ArrayList<>();
			for (RecipeHolder<WorktableRecipe> h : rm().getAllRecipesFor(RotaryRecipeTypes.WORKTABLE.get())) {
				WorktableRecipe r = h.value();
				MachineRecipe.Builder b = MachineRecipe.builder().grid(r.getGridLayout());
				if (r instanceof WorktableRecipe.Recycling rc) {
					for (ItemStack is : rc.getSplitOutput())
						b.out(is);
					b.line("Recycling");
				}
				else
					b.out(r.getResultItem(ra()));
				li.add(b.build());
			}
			return li;
		});
		category(RotaryCraft.MOD_ID, "blast_furnace", () -> st(RotaryBlocks.BLAST_FURNACE.get()), 2, () -> {
			List<MachineRecipe> li = new ArrayList<>();
			for (RecipeHolder<BlastFurnaceRecipe> h : rm().getAllRecipesFor(RotaryRecipeTypes.BLAST_FURNACE.get())) {
				BlastFurnaceRecipe r = h.value();
				MachineRecipe.Builder b = MachineRecipe.builder().in(r.main(), r.mainCount());
				for (var a : List.of(r.primary(), r.secondary(), r.tertiary()))
					a.ifPresent(ad -> b.in(ad.ingredient(), ad.count()));
				for (ItemStack is : r.results())
					b.out(is);
				b.line(r.temperature()+" °C"+(r.exact() ? ", exactly "+r.mainCount()+" main items" : ""));
				StringBuilder add = new StringBuilder();
				for (var a : List.of(r.primary(), r.secondary(), r.tertiary()))
					a.ifPresent(ad -> add.append(add.length() == 0 ? "Additives used: " : ", ").append((int)ad.chance()).append(" %"));
				if (add.length() > 0)
					b.line(add.toString());
				li.add(b.build());
			}
			return li;
		});
		category(RotaryCraft.MOD_ID, "blast_crafting", () -> st(RotaryBlocks.BLAST_FURNACE.get()), () -> Component.literal("Blast Furnace Crafting"), 1, () -> {
			List<MachineRecipe> li = new ArrayList<>();
			for (RecipeHolder<BlastCraftingRecipe> h : rm().getAllRecipesFor(RotaryRecipeTypes.BLAST_CRAFTING.get())) {
				BlastCraftingRecipe r = h.value();
				List<Ingredient> layout = new ArrayList<>();
				if (r.recipe() instanceof ShapedRecipe s) {
					for (int y = 0; y < 3; y++)
						for (int x = 0; x < 3; x++)
							layout.add(x < s.getWidth() && y < s.getHeight() ? s.getIngredients().get(x+y*s.getWidth()) : Ingredient.EMPTY);
				}
				else
					layout.addAll(r.recipe().getIngredients());
				li.add(MachineRecipe.builder().grid(layout).out(r.recipe().getResultItem(ra())).line(r.temperature()+" °C"+(r.speed() > 1 ? ", speed "+r.speed() : "")).build());
			}
			return li;
		});
		category(RotaryCraft.MOD_ID, "friction_heater", () -> st(RotaryBlocks.FRICTION_HEATER.get()), 1, () -> {
			List<MachineRecipe> li = new ArrayList<>();
			for (RecipeHolder<FrictionHeaterRecipe> h : rm().getAllRecipesFor(RotaryRecipeTypes.FRICTION.get()))
				li.add(MachineRecipe.builder().in(h.value().ingredient()).out(h.value().result()).line(h.value().temperature()+" °C, "+h.value().duration()+" ticks").build());
			return li;
		});
		category(RotaryCraft.MOD_ID, "grinder", () -> st(RotaryBlocks.GRINDER.get()), 1, () -> {
			List<MachineRecipe> li = new ArrayList<>();
			for (RecipeHolder<GrinderRecipe> h : rm().getAllRecipesFor(RotaryRecipeTypes.GRINDER.get())) {
				MachineRecipe.Builder b = MachineRecipe.builder().in(h.value().ingredient()).out(h.value().result()).line(power(128, 1, 4096));
				if (h.value().ingredient().test(st(RotaryItems.CANOLA_SEEDS.get())))
					b.fluidOut(new FluidStack(RotaryFluids.LUBRICANT.get(), 160));
				li.add(b.build());
			}
			return li;
		});
		category(RotaryCraft.MOD_ID, "centrifuge", () -> st(RotaryBlocks.CENTRIFUGE.get()), 1, () -> {
			List<MachineRecipe> li = new ArrayList<>();
			for (RecipeHolder<CentrifugeRecipe> h : rm().getAllRecipesFor(RotaryRecipeTypes.CENTRIFUGE.get())) {
				CentrifugeRecipe r = h.value();
				MachineRecipe.Builder b = MachineRecipe.builder().in(r.ingredient()).line(power(1, 4096, 16384));
				for (CentrifugeRecipe.ChanceOutput o : r.outputs())
					b.out(o.item(), (int)o.chance()+"%");
				r.fluid().ifPresent(f -> b.fluidOut(f));
				li.add(b.build());
			}
			return li;
		});
		category(RotaryCraft.MOD_ID, "pulse_jet", () -> st(RotaryBlocks.PULSE_JET.get()), 1, () -> {
			List<MachineRecipe> li = new ArrayList<>();
			for (RecipeHolder<PulseJetRecipe> h : rm().getAllRecipesFor(RotaryRecipeTypes.PULSE_JET.get()))
				li.add(MachineRecipe.builder().in(h.value().ingredient()).out(h.value().result()).fluidIn(new FluidStack(RotaryFluids.JET_FUEL.get(), 100))
						.line(h.value().temperature()+" °C, "+power(1, 131072, 1)).build());
			return li;
		});
		category(RotaryCraft.MOD_ID, "compactor", () -> st(RotaryBlocks.COMPACTOR.get()), 2, () -> {
			List<MachineRecipe> li = new ArrayList<>();
			for (CompactorRecipes.Recipe r : CompactorRecipes.all()) {
				ItemStack in = r.in().get();
				li.add(MachineRecipe.builder().in(in).in(in).in(in).in(in).out(r.getOutput())
						.line(r.pressure()+" kPa, "+r.temperature()+" °C").line(power(4096, 1, 262144)).build());
			}
			return li;
		});
		category(RotaryCraft.MOD_ID, "drying_bed", () -> st(RotaryBlocks.DRYING_BED.get()), 1, () -> List.of(
				MachineRecipe.builder().fluidIn(new FluidStack(Fluids.WATER, 250)).out(part(CraftingPart.SALT)).line("400 ticks, no power").build(),
				MachineRecipe.builder().fluidIn(new FluidStack(Fluids.LAVA, 1000)).out(st(Items.GOLD_NUGGET)).line("400 ticks, no power").build()));
		category(RotaryCraft.MOD_ID, "extractor", () -> st(RotaryBlocks.EXTRACTOR.get()), 2, () -> {
			List<MachineRecipe> li = new ArrayList<>();
			String[] names = {"Grinding", "Washing (125 mB water)", "Leaching (125 mB water)", "Drying"};
			for (ExtractorOres.Ore o : ExtractorOres.Ore.values()) {
				for (int s = 0; s < 4; s++) {
					MachineRecipe.Builder b = MachineRecipe.builder();
					if (s == 0)
						b.in(MachineRecipe.of(Ingredient.of(o.oreTag), 1));
					else
						b.in(st(o.stage(s-1)));
					b.out(st(o.stage(s)), "x2 "+(o.rarity == ExtractorOres.OreRarity.RARE ? ExtractorBlockEntity.oreCopyRare : ExtractorBlockEntity.oreCopy)+"%");
					if (s == 1 || s == 2)
						b.fluidIn(new FluidStack(Fluids.WATER, 125));
					b.line(names[s]).line(power(ExtractorBlockEntity.MIN_TORQUE[s], ExtractorBlockEntity.MIN_SPEED[s], ExtractorBlockEntity.MIN_POWER[s]));
					li.add(b.build());
				}
			}
			return li;
		});
		category(RotaryCraft.MOD_ID, "fermenter", () -> st(RotaryBlocks.FERMENTER.get()), 2, () -> {
			List<ItemStack> plants = new ArrayList<>();
			for (Item i : BuiltInRegistries.ITEM)
				if (RotaryDataMaps.getPlantValue(st(i)) > 0)
					plants.add(st(i));
			return List.of(
					MachineRecipe.builder().in(st(Items.SUGAR)).in(st(Items.DIRT)).out(st(RotaryItems.YEAST.get())).fluidIn(new FluidStack(Fluids.WATER, 50))
							.line("No redstone signal, best at 25 °C").line(power(1, 32, 1024)).build(),
					MachineRecipe.builder().in(st(RotaryItems.YEAST.get())).in(plants).out(part(CraftingPart.SLUDGE), "plant value").fluidIn(new FluidStack(Fluids.WATER, 50))
							.line("With redstone signal, best at 35 °C").line(power(1, 32, 1024)).build());
		});
		category(RotaryCraft.MOD_ID, "fractionator", () -> st(RotaryBlocks.FRACTIONATOR.get()), 2, () -> List.of(
				MachineRecipe.builder().in(st(Items.BLAZE_POWDER)).in(part(CraftingPart.COALDUST)).in(st(Items.MAGMA_CREAM)).in(st(Items.PINK_DYE))
						.in(part(CraftingPart.NETHERRACKDUST)).in(part(CraftingPart.TAR)).in(st(Items.GHAST_TEAR))
						.fluidIn(new FluidStack(RotaryFluids.ETHANOL.get(), 250)).fluidOut(new FluidStack(RotaryFluids.JET_FUEL.get(), 2200))
						.line("Yield by pressure (up to 1000 kPa); ghast tear kept").line(power(1, 8192, 65536)).build()));
		category(RotaryCraft.MOD_ID, "refrigerator", () -> st(RotaryBlocks.REFRIGERATOR.get()), 2, () -> List.of(
				MachineRecipe.builder().in(st(Items.ICE)).out(part(CraftingPart.DRYICE), "25%").fluidOut(new FluidStack(RotaryFluids.LIQUID_NITROGEN.get(), 2000))
						.line("min(2000, 100*(torque/2048)^2) mB per cycle").line(power(2048, 1, 32768)).build()));
	}

	@Override
	public ResourceLocation getPluginUid() {
		return ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "jei");
	}
}
