/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

/** Original: Reika.RotaryCraft.Auxiliary.Interfaces.TemperatureTE (the parts used by the ported machines). */
public interface TemperatureMachine extends HeatConduction {

	void addTemperature(int temp);

	@Override
	int getTemperature();

	@Override
	void setTemperature(int temp);

	int getMaxTemperature();

	/** Whether outside heat sources (e.g. a gas turbine's exhaust) set this machine's temperature. Original default for engines: false. */
	@Override
	default boolean allowExternalHeating() {
		return false;
	}

	/** Whether a heat pipe may take heat out (original HeatConduction, set by every TemperatureTE; most RotaryCraft machines: false). */
	@Override
	default boolean allowHeatExtraction() {
		return false;
	}

	/** Original canBeCooledWithFins (cooling fin target). */
	default boolean canBeCooledWithFins() {
		return false;
	}
}
