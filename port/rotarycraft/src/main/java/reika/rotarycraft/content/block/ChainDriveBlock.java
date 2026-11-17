/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import net.minecraft.world.item.ItemStack;

import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/** Original: MachineRegistry.CHAIN in BlockDMachine - a belt hub joined with chains. */
public class ChainDriveBlock extends BeltHubBlock {

	public ChainDriveBlock(Properties props) {
		super(props, RotaryBlockEntities.CHAIN_DRIVE);
	}

	@Override
	protected boolean isProperBelt(ItemStack is) {
		return is.is(RotaryItems.part(CraftingPart.CHAIN));
	}
}
