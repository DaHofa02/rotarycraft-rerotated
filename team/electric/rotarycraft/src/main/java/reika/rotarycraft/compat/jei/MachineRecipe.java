/*******************************************************************************
 * Optional Just Enough Items integration of the private NeoForge 1.21.1 port of RotaryCraft (by Reika Kalseki); not part of the original
 * mod's 1.7.10 JEI-less form (the original showed its recipes in NEI and its handbook). Only loaded when JEI is installed.
 ******************************************************************************/
package reika.rotarycraft.compat.jei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * One machine "recipe" as JEI shows it: input slots (each a list of alternatives; in a 3x3 grid when {@code grid}), outputs with an
 * optional chance text, fluid tanks in and out, and the machine's requirements as text lines (power, temperature, pressure ...).
 */
public record MachineRecipe(List<List<ItemStack>> inputs, boolean grid, List<Output> outputs, List<FluidStack> fluidsIn, List<FluidStack> fluidsOut,
		List<Component> lines) {

	public record Output(ItemStack item, String note) {}

	public static Builder builder() {
		return new Builder();
	}

	public static List<ItemStack> of(Ingredient ing, int count) {
		List<ItemStack> li = new ArrayList<>();
		for (ItemStack is : ing.getItems())
			li.add(is.copyWithCount(count));
		return li;
	}

	public static final class Builder {
		private final List<List<ItemStack>> inputs = new ArrayList<>();
		private boolean grid;
		private final List<Output> outputs = new ArrayList<>();
		private final List<FluidStack> fin = new ArrayList<>();
		private final List<FluidStack> fout = new ArrayList<>();
		private final List<Component> lines = new ArrayList<>();

		public Builder in(ItemStack... alts) {
			inputs.add(Arrays.asList(alts));
			return this;
		}

		public Builder in(List<ItemStack> alts) {
			inputs.add(alts);
			return this;
		}

		public Builder in(Ingredient ing) {
			return this.in(ing, 1);
		}

		public Builder in(Ingredient ing, int count) {
			inputs.add(ing.isEmpty() ? List.of() : of(ing, count));
			return this;
		}

		/** The ingredients take the 3x3 grid positions in order (empty ones leave a gap). */
		public Builder grid(List<Ingredient> layout) {
			grid = true;
			for (Ingredient i : layout)
				this.in(i);
			return this;
		}

		public Builder out(ItemStack is) {
			outputs.add(new Output(is, null));
			return this;
		}

		public Builder out(ItemStack is, String note) {
			outputs.add(new Output(is, note));
			return this;
		}

		public Builder fluidIn(FluidStack fs) {
			fin.add(fs);
			return this;
		}

		public Builder fluidOut(FluidStack fs) {
			fout.add(fs);
			return this;
		}

		public Builder line(Component c) {
			lines.add(c);
			return this;
		}

		public Builder line(String s) {
			return this.line(Component.literal(s));
		}

		public MachineRecipe build() {
			return new MachineRecipe(List.copyOf(inputs), grid, List.copyOf(outputs), List.copyOf(fin), List.copyOf(fout), List.copyOf(lines));
		}
	}
}
