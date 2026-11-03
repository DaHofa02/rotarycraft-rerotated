/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.network;

/** Original: Reika.ElectriCraft.Auxiliary.Interfaces.Overloadable - melts or blows at too much current. */
public interface Overloadable {

	void overload(int current);
}
