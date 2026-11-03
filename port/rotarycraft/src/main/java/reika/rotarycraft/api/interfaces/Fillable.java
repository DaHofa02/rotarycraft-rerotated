/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.api.interfaces;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/** Original: Reika.RotaryCraft.API.Interfaces.Fillable - an item the filling station can fill with a fluid. */
public interface Fillable {

	/** Return true if the item can currently accept this fluid. */
	boolean isValidFluid(Fluid f, ItemStack is);

	/** The max amount of fluid (in millibuckets) the item can take. */
	int getCapacity(ItemStack is);

	/** The current fluid level in millibuckets. */
	int getCurrentFillLevel(ItemStack is);

	/** This adds fluid to the item and returns how much was successfully added. */
	int addFluid(ItemStack is, Fluid f, int amt);

	boolean isFull(ItemStack is);

	@Nullable
	Fluid getCurrentFluid(ItemStack is);
}
