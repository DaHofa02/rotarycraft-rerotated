/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft by Reika Kalseki; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.recipe;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * The Blast Furnace inventory as seen by its recipes: slot 0 centre additive, 1-9 the main grid, 11 lower and 14 upper additive
 * (original TileEntityBlastFurnace slot numbers), plus the furnace temperature.
 */
public record BlastFurnaceInput(List<ItemStack> slots, int temperature) implements RecipeInput {

	@Override
	public ItemStack getItem(int i) {
		return slots.get(i);
	}

	@Override
	public int size() {
		return slots.size();
	}

	/** The 3x3 main grid (slots 1-9) as a crafting grid, for the Blast Furnace crafting recipes. */
	public CraftingInput grid() {
		List<ItemStack> li = new ArrayList<>(9);
		for (int i = 1; i <= 9; i++)
			li.add(slots.get(i));
		return CraftingInput.of(3, 3, li);
	}
}
