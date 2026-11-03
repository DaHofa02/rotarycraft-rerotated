/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.network;

import net.minecraft.core.Direction;

/** Original: Reika.ElectriCraft.Auxiliary.Interfaces.WireEmitter - a power source (generator, battery). */
public interface WireEmitter extends NetworkTile {

	int getGenVoltage();

	int getGenCurrent();

	boolean canEmitPowerToSide(Direction dir);

	boolean canEmitPower();
}
