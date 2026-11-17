/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.SONICBORER in BlockDMMachine - 6 orientations (is6Sided). */
public class SonicBorerBlock extends MachineBlock {

	public SonicBorerBlock(Properties props) {
		super(props, RotaryBlockEntities.SONIC_BORER, Placement.SIX_SIDED, false);
	}
}
