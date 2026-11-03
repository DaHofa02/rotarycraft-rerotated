/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.network;

/** Original: Reika.ElectriCraft.Auxiliary.Interfaces.CurrentThrottle - limits the current of every path through it. */
public interface CurrentThrottle {

	int getCurrentLimit();
}
