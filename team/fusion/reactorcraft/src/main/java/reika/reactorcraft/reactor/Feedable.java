/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

import net.minecraft.world.item.ItemStack;

/** Original: Reika.ReactorCraft.Auxiliary.Feedable - fuel handed down a column of cores. */
public interface Feedable {

	boolean feedIn(ItemStack is);

	ItemStack feedOut();
}
