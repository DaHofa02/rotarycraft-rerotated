/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.api.interfaces;

import net.minecraft.world.item.ItemStack;

/** Original: Reika.RotaryCraft.API.Interfaces.TensionStorage - a coil the coil winder can wind up and unwind. */
public interface TensionStorage {

	/** How much torque charging needs: max charge = input torque / stiffness. Also multiplies the unwind time. */
	int getStiffness(ItemStack is);

	/** Scale factor of the torque and speed the coil gives when unwound (power scales with its square). */
	int getPowerScale(ItemStack is);

	/** Whether the coil may break while charging. */
	boolean isBreakable(ItemStack is);
}
