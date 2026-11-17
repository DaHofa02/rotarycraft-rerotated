/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.PILEDRIVER in BlockDMMachine - 2 orientations (is2Sided): its axis runs north-south or east-west. */
public class PileDriverBlock extends MachineBlock {

	public PileDriverBlock(Properties props) {
		super(props, RotaryBlockEntities.PILE_DRIVER, Placement.TWO_SIDED, false);
	}
}
