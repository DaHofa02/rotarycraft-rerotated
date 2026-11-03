/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.api.interfaces;

/** Original: Reika.RotaryCraft.API.Interfaces.Shockable - block entities with special behaviour when hit by a Van de Graaff discharge. */
public interface Shockable {

	/** Called when the VDG hits the block entity with a bolt. Args: charge in the bolt, distance of the hit */
	void onDischarge(int charge, double range);

	/** The minimum charge the VDG must reach before it hits the block entity */
	int getMinDischarge();

	/** Whether the block entity accepts discharges from further than one block (2 or 3 blocks above the VDG) */
	boolean canDischargeLongRange();

	/** Where in the block to aim the bolt tip, 0-1 */
	float getAimX();

	float getAimY();

	float getAimZ();
}
