/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.SPLITBELT in BlockDMachine - the take-off belt, joined with belts. */
public class SplitBeltBlock extends BeltHubBlock {

	public SplitBeltBlock(Properties props) {
		super(props, RotaryBlockEntities.SPLIT_BELT);
	}
}
