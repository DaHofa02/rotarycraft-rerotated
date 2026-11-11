/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;

import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.Auxiliary.RecipeManagers.RecipesLavaMaker - what the rock melter turns into which liquid, the temperature
 * (deg C) it needs and the energy (J) one item takes. The ore dictionary names "stone" and "cobblestone" are the c:stones and
 * c:cobblestones tags. Open: dry ice -> CO2 (no CO2 fluid in the port) and the recipes for other mods' liquids.
 */
public final class LavaMakerRecipes {

	public record Recipe(Predicate<ItemStack> input, Supplier<Fluid> fluid, int amount, int temperature, long energy) {

		public FluidStack output() {
			return new FluidStack(fluid.get(), amount);
		}
	}

	private static final List<Recipe> RECIPES = new ArrayList<>();

	static {
		add(is -> is.is(Items.STONE), () -> Fluids.LAVA, 1000, 1000, 5200000);
		add(is -> is.is(Items.COBBLESTONE), () -> Fluids.LAVA, 500, 1000, 3120000);
		add(is -> is.is(Items.NETHERRACK), () -> Fluids.LAVA, 2000, 600, 480000);
		add(is -> is.is(Items.STONE_BRICKS), () -> Fluids.LAVA, 1000, 1200, 4160000);
		add(is -> is.is(RotaryItems.ETHANOL.get()), () -> RotaryFluids.ETHANOL.get(), 1000, 180, 6000);
		add(is -> is.is(RotaryItems.part(CraftingPart.CLEANSLUDGE)), () -> RotaryFluids.ETHANOL.get(), 1000, 180, 9000);
		add(is -> is.is(Tags.Items.STONES), () -> Fluids.LAVA, 1000, 1000, 5200000);
		add(is -> is.is(Tags.Items.COBBLESTONES), () -> Fluids.LAVA, 500, 1000, 2820000);
	}

	private LavaMakerRecipes() {}

	private static void add(Predicate<ItemStack> in, Supplier<Fluid> f, int amount, int temperature, long energy) {
		RECIPES.add(new Recipe(in, f, amount, temperature, energy));
	}

	@Nullable
	public static Recipe get(ItemStack is) {
		if (is.isEmpty())
			return null;
		for (Recipe r : RECIPES)
			if (r.input.test(is))
				return r;
		return null;
	}

	public static boolean isValidFuel(ItemStack is) {
		return get(is) != null;
	}
}
