/*******************************************************************************
 * Dev-only visual check of the JEI plugins of the private ports (inactive unless the system property "reactorcraft.showcase.jei" is set,
 * as the jeiShowcase run does): opens every RotaryCraft/ReactorCraft JEI category and every multiblock in turn, takes a screenshot of each
 * (run/screenshots/jei_*.png) and quits. With "reactorcraft.showcase.jei.only" (comma-separated category ids, also of other mods, e.g.
 * gtceu:macerator for comparing the look) only those categories.
 ******************************************************************************/
package reika.reactorcraft.compat.jei;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import reika.reactorcraft.ReactorCraft;

public final class JeiShowcase {

	static final boolean ACTIVE = System.getProperty("reactorcraft.showcase.jei") != null;
	private static final List<String> ONLY = System.getProperty("reactorcraft.showcase.jei.only", "").isBlank() ? List.of()
			: List.of(System.getProperty("reactorcraft.showcase.jei.only").split(","));

	static IJeiRuntime runtime;

	private static final List<Runnable> pages = new ArrayList<>();
	private static final List<String> names = new ArrayList<>();
	private static int ticks;

	private JeiShowcase() {}

	private static <T> void addCategory(IRecipeCategory<T> cat) {
		List<T> recipes = runtime.getRecipeManager().createRecipeLookup(cat.getRecipeType()).get().toList();
		if (recipes.isEmpty())
			return;
		int n = cat.getRecipeType() == MultiblockCategory.TYPE ? recipes.size() : 1;
		for (int i = 0; i < n; i++) {
			T r = recipes.get(i);
			pages.add(() -> runtime.getRecipesGui().showRecipes(cat, List.of(r), List.of()));
			names.add(cat.getRecipeType().getUid().getNamespace()+"_"+cat.getRecipeType().getUid().getPath()+(n > 1 ? "_"+i : ""));
		}
	}

	public static void tick() {
		if (!ACTIVE || runtime == null)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null)
			return;
		ticks++;
		if (ticks == 60) {
			runtime.getRecipeManager().createRecipeCategoryLookup().get().forEach(c -> {
				String ns = c.getRecipeType().getUid().getNamespace();
				if (ONLY.isEmpty() ? ns.equals("rotarycraft") || ns.equals(ReactorCraft.MOD_ID) : ONLY.contains(c.getRecipeType().getUid().toString()))
					addCategory(c);
			});
		}
		if (ticks < 100)
			return;
		int page = (ticks-100)/40, phase = (ticks-100)%40;
		if (page >= pages.size()) {
			mc.stop();
			return;
		}
		if (phase == 0)
			pages.get(page).run();
		if (phase == 35)
			Screenshot.grab(mc.gameDirectory, "jei_"+names.get(page)+".png", mc.getMainRenderTarget(), msg -> ReactorCraft.LOGGER.info("jei showcase: {}", msg.getString()));
	}
}
