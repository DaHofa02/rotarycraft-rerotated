/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.api.interfaces;

import net.minecraft.world.item.ItemStack;

/** Original: Reika.RotaryCraft.API.Interfaces.ChargeableTool - a tool charged from a wound coil in the worktable. */
public interface ChargeableTool {

	/** Sets the tool's charge from the coil and returns the charge the coil keeps (the original tools swap the two). */
	int setCharged(ItemStack is, int charge, boolean strongcoil);
}
