/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.network;

import net.minecraft.core.Direction;

/** Original: Reika.ElectriCraft.Auxiliary.Interfaces.WireReceiver - a power sink (motor, battery). */
public interface WireReceiver extends NetworkTile {

	boolean canReceivePowerFromSide(Direction dir);

	boolean canReceivePower();
}
