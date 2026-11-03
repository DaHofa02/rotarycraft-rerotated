/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.api.interfaces;

import net.minecraft.core.Direction;

/** Original: Reika.RotaryCraft.Auxiliary.Interfaces.PumpablePipe - a pipe the pipe pump can move liquid out of and into. */
public interface PumpablePipe {

	int getFluidLevel();

	boolean canTransferTo(PumpablePipe p, Direction dir);

	void transferFrom(PumpablePipe from, int amt);
}
