/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

/**
 * Original: Reika.ReactorCraft.Auxiliary.LinkableReactorCore - a reactor part (fuel cores, control rods) whose temperature a CPU can
 * watch after linking them with the CPU remote control; the part remembers its CPU and leaves its checks when broken.
 */
public interface LinkableReactorCore extends Temperatured {

	void link(CpuBlockEntity te);
}
