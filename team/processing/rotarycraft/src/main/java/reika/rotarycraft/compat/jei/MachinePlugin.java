/*******************************************************************************
 * Optional Just Enough Items integration of the private NeoForge 1.21.1 port of RotaryCraft (by Reika Kalseki). Only loaded with JEI.
 ******************************************************************************/
package reika.rotarycraft.compat.jei;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import reika.rotarycraft.RotaryCraft;

/** Base of the three mods' JEI plugins: each lists its machines as {@link MachineCategory} categories with their catalysts. */
public abstract class MachinePlugin implements IModPlugin {

	private final Map<RecipeType<MachineRecipe>, Def> defs = new LinkedHashMap<>();
	private final Map<RecipeType<MachineRecipe>, MachineCategory> categories = new LinkedHashMap<>();

	private record Def(Supplier<ItemStack> machine, Supplier<Component> title, int lines, Supplier<List<MachineRecipe>> recipes) {}

	protected final void category(String mod, String id, Supplier<ItemStack> machine, int lines, Supplier<List<MachineRecipe>> recipes) {
		this.category(mod, id, machine, null, lines, recipes);
	}

	protected final void category(String mod, String id, Supplier<ItemStack> machine, Supplier<Component> title, int lines, Supplier<List<MachineRecipe>> recipes) {
		defs.put(RecipeType.create(mod, id, MachineRecipe.class), new Def(machine, title, lines, recipes));
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration reg) {
		var gui = reg.getJeiHelpers().getGuiHelper();
		for (var e : defs.entrySet()) {
			ItemStack m = e.getValue().machine().get();
			Component title = e.getValue().title() != null ? e.getValue().title().get() : m.getHoverName();
			MachineCategory c = new MachineCategory(gui, e.getKey(), title, m, e.getValue().lines());
			categories.put(e.getKey(), c);
			reg.addRecipeCategories(c);
		}
	}

	@Override
	public void registerRecipes(IRecipeRegistration reg) {
		for (var e : defs.entrySet()) {
			try {
				List<MachineRecipe> li = e.getValue().recipes().get();
				MachineCategory c = categories.get(e.getKey());
				if (c != null)
					c.layout(li);
				reg.addRecipes(e.getKey(), li);
			}
			catch (RuntimeException ex) {
				RotaryCraft.LOGGER.error("JEI: could not list the recipes of {}", e.getKey().getUid(), ex);
			}
		}
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
		for (var e : defs.entrySet())
			reg.addRecipeCatalyst(e.getValue().machine().get(), e.getKey());
	}
}
