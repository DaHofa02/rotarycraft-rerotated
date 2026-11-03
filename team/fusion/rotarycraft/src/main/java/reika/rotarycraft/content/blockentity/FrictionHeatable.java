/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft API). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Original: RotaryCraft API Interfaces.ThermalMachine / FrictionHeatable - a machine a Friction Heater can heat. */
public interface FrictionHeatable extends TemperatureMachine {

	boolean canBeFrictionHeated();

	/** Fraction of the temperature difference transferred per tick. */
	float getMultiplier();

	/** Stops the machine's own ambient cooling from immediately undoing the heating. */
	void resetAmbientTemperatureTimer();

	void onOverheat(Level world, BlockPos pos);
}
