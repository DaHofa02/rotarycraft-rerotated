/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

/** Original: Reika.RotaryCraft.Auxiliary.Interfaces.TemperatureTE (the parts used by the ported machines). */
public interface TemperatureMachine {

	void addTemperature(int temp);

	int getTemperature();

	void setTemperature(int temp);

	int getMaxTemperature();

	/** Whether outside heat sources (e.g. a gas turbine's exhaust) set this machine's temperature. Original default for engines: false. */
	default boolean allowExternalHeating() {
		return false;
	}

	/** Original canBeCooledWithFins (cooling fin target). */
	default boolean canBeCooledWithFins() {
		return false;
	}
}
