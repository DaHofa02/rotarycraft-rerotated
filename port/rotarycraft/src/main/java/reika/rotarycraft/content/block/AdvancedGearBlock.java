/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import reika.rotarycraft.content.blockentity.AdvancedGearBlockEntity;
import reika.rotarycraft.registry.RotaryBlockEntities;

/** The worm gear and the high-ratio gear: four-sided, input on the side the placer looked away from, no GUI. */
public class AdvancedGearBlock extends MachineBlock {

	public final AdvancedGearBlockEntity.Kind kind;

	public AdvancedGearBlock(Properties props, AdvancedGearBlockEntity.Kind kind) {
		super(props, RotaryBlockEntities.ADVANCED_GEAR, Placement.FOUR_SIDED, false);
		this.kind = kind;
	}
}
