/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.BUSCONTROLLER in BlockDMachine - a full block (steel top and bottom), 4 orientations for its input. */
public class BusControllerBlock extends MachineBlock {

	public BusControllerBlock(Properties props) {
		super(props, RotaryBlockEntities.BUS_CONTROLLER, Placement.FOUR_SIDED, false);
	}
}
