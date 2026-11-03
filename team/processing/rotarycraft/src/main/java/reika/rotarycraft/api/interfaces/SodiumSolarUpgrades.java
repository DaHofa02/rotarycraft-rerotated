/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.api.interfaces;

/**
 * Original: Reika.RotaryCraft.Auxiliary.Interfaces.SodiumSolarUpgrades - the ReactorCraft add-ons of the solar tower: a receiver on top
 * of the tower is heated by the mirrors (and allows sodium in the tower), an output under it takes the sodium the tower uses up.
 */
public interface SodiumSolarUpgrades {

	boolean isActive();

	interface SodiumSolarReceiver extends SodiumSolarUpgrades {

		void tick(int mirrorCount, float totalBrightness);

		int getTemperature();
	}

	interface SodiumSolarOutput extends SodiumSolarUpgrades {

		int receiveSodium(int amt);
	}
}
