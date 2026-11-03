/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.network;

import net.minecraft.core.Direction;

import reika.rotarycraft.api.power.ShaftMachine;
import reika.rotarycraft.content.power.PowerSourceTracker;

/**
 * Original: Reika.ElectriCraft.API.WrappableWireSource - a block entity of another mod (the ReactorCraft generator) that feeds a wire
 * network without being a network part itself; adjacent wires wrap it in a {@link WrappedSource}.
 */
public interface WrappableWireSource extends ShaftMachine, PowerSourceTracker {

	boolean canConnectToSide(Direction dir);

	boolean isFunctional();

	boolean hasPowerStatusChangedSinceLastTick();
}
