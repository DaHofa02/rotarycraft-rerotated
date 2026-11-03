/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.world.item.ItemStack;

/** Original: Reika.RotaryCraft.Auxiliary.Interfaces.UpgradeableMachine - applied by right-clicking with an upgrade item. */
public interface UpgradeableMachine {

	void upgrade(ItemStack is);

	boolean canUpgradeWith(ItemStack item);
}
