/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.DifficultyEffects;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.Auxiliary.RecipeManagers.RecipesCompactor - four of the same item are pressed into the result at a
 * minimum pressure (kPa) and temperature (C): coal (charcoal gives 1.5 times as much) -> anthracite -> prismane -> lonsdaleite ->
 * diamond (550000 kPa, 800 C; 2 per step, 1 on hard), blaze powder -> glowstone (2000 kPa, 600 C), ice -> 2 packed ice (24000 kPa,
 * -80 C). Other mods add theirs with {@link #add} (ReactorCraft: lodestone and permanent magnets).
 */
public final class CompactorRecipes {

	public record Recipe(Supplier<ItemStack> in, Supplier<ItemStack> out, int pressure, int temperature) {

		public boolean matches(ItemStack is) {
			return ItemStack.isSameItemSameComponents(in.get(), is);
		}

		public ItemStack getOutput() {
			return out.get().copy();
		}
	}

	private static final List<Recipe> RECIPES = new ArrayList<>();

	static {
		int rp = CompactorBlockEntity.REQPRESS;
		int rt = CompactorBlockEntity.REQTEMP;
		add(() -> new ItemStack(Items.COAL), () -> part(CraftingPart.ANTHRACITE, perStep()), rp, rt);
		add(() -> new ItemStack(Items.CHARCOAL), () -> part(CraftingPart.ANTHRACITE, perStep()*3/2), rp, rt);
		add(() -> part(CraftingPart.ANTHRACITE, 1), () -> part(CraftingPart.PRISMANE, perStep()), rp, rt);
		add(() -> part(CraftingPart.PRISMANE, 1), () -> part(CraftingPart.LONSDA, perStep()), rp, rt);
		add(() -> part(CraftingPart.LONSDA, 1), () -> new ItemStack(Items.DIAMOND, perStep()), rp, rt);
		add(() -> new ItemStack(Items.BLAZE_POWDER), () -> new ItemStack(Items.GLOWSTONE), 2000, 600);
		add(() -> new ItemStack(Items.ICE), () -> new ItemStack(Items.PACKED_ICE, 2), 24000, -80);
	}

	private CompactorRecipes() {}

	private static ItemStack part(CraftingPart p, int n) {
		return new ItemStack(RotaryItems.part(p), n);
	}

	/** Original getNumberPerStep: DifficultyEffects.COMPACTOR. */
	public static int perStep() {
		return DifficultyEffects.COMPACTOR.getInt();
	}

	/** Original addRecipe / addAPIRecipe (args: input, output, pressure, temperature). */
	public static void add(Supplier<ItemStack> in, Supplier<ItemStack> out, int pressure, int temperature) {
		RECIPES.add(new Recipe(in, out, pressure, temperature));
	}

	@Nullable
	public static Recipe get(ItemStack is) {
		if (is.isEmpty())
			return null;
		for (Recipe r : RECIPES)
			if (r.matches(is))
				return r;
		return null;
	}

	public static boolean isCompactable(ItemStack is) {
		return get(is) != null;
	}

	public static List<Recipe> all() {
		return Collections.unmodifiableList(RECIPES);
	}
}
