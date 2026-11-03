/*******************************************************************************
 * Original: Reika.RotaryCraft.API (RotaryCraft API by Reika Kalseki).
 * Adapted to Minecraft 1.21.1 types for a private port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.api.power;

public interface PowerTracker {

	long getMaxGennablePower();

	long getRealMaxPower();

	boolean isLooping();

	boolean contains(PowerGenerator te);

	boolean calledFrom(ShaftMerger sm);

	boolean passesThrough(ShaftMerger sm);

	boolean isEngineSpam();
}
