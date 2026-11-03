/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.api.interfaces;

/** Original: Reika.RotaryCraft.API.Interfaces.RefrigeratorAttachment - told about every completed refrigerator cycle (mB of LN2 made). */
public interface RefrigeratorAttachment {

	void onCompleteCycle(int ln2);
}
