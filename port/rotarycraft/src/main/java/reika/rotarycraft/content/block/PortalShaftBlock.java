/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.content.blockentity.PortalShaftBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.registry.MaterialRegistry;
import reika.rotarycraft.registry.RotaryBlockEntities;

/** The portal shaft: never placed by hand (a shaft into a portal turns into one); broken, it drops its plain shaft. */
public class PortalShaftBlock extends MachineBlock {

	public PortalShaftBlock(Properties props) {
		super(props, RotaryBlockEntities.PORTAL_SHAFT, Placement.NONE, false);
	}

	private static List<ItemStack> shaft(@Nullable RotaryCraftBlockEntity te) {
		MaterialRegistry mat = te instanceof PortalShaftBlockEntity ps ? ps.getShaftType() : MaterialRegistry.STEEL;
		return List.of(ShaftBlock.shaftItem(mat, false));
	}

	@Override
	protected List<ItemStack> getHarvestDrops(BlockState state, RotaryCraftBlockEntity te) {
		return shaft(te);
	}

	@Override
	protected List<ItemStack> getPlainDrops(BlockState state, @Nullable RotaryCraftBlockEntity te) {
		return shaft(te);
	}
}
