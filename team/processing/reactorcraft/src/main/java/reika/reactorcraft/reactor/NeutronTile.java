/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import reika.reactorcraft.content.entity.NeutronEntity;

/** Original: NeutronTile - a block entity that reacts to a neutron entering it; true = the neutron is absorbed. */
public interface NeutronTile {

	boolean onNeutron(NeutronEntity e, Level world, BlockPos pos);
}
