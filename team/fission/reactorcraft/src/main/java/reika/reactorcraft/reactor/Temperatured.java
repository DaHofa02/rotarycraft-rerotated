/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

/** Original: Reika.ReactorCraft.Auxiliary.Temperatured - reactor parts that exchange heat. */
public interface Temperatured {

	int getTemperature();

	void setTemperature(int T);

	int getMaxTemperature();

	boolean canDumpHeatInto(LiquidStates liq);
}
