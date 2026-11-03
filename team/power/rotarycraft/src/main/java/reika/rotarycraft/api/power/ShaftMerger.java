/*******************************************************************************
 * Original: Reika.RotaryCraft.API (RotaryCraft API by Reika Kalseki).
 * Adapted to Minecraft 1.21.1 types for a private port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.api.power;

/** If your machine merges two shafts, implement this to avoid being the source of a loop exploit or StackOverflow */
public interface ShaftMerger {

	void onPowerLooped(PowerTracker pwr);

	void fail();
}
