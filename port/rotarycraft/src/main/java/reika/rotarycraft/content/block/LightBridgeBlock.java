/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.LIGHTBRIDGE in BlockDMMachine - 4 orientations with north/south swapped (isZFlipped). */
public class LightBridgeBlock extends MachineBlock {

	public LightBridgeBlock(Properties props) {
		super(props, RotaryBlockEntities.LIGHT_BRIDGE, Placement.FOUR_SIDED_Z_FLIPPED, false);
	}
}
