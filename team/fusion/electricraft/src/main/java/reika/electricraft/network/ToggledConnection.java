/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.network;

/** Original: Reika.ElectriCraft.Auxiliary.Interfaces.ToggledConnection - a path through it only conducts while it connects (relay). */
public interface ToggledConnection {

	boolean canConnect();
}
