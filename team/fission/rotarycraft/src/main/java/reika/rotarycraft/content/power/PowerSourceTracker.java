/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.power;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.rotarycraft.api.power.ShaftMerger;

/** Original: Reika.RotaryCraft.Auxiliary.Interfaces.PowerSourceTracker */
public interface PowerSourceTracker {

	PowerSourceList getPowerSources(PowerSourceTracker io, ShaftMerger caller);

	/** c may contain nulls. */
	void getAllOutputs(Collection<BlockEntity> c, Direction dir);

	Level getWorld();

	BlockPos getPos();

	/** The block this machine's power I/O is attributed to; zero for everything except multi-block machines. */
	BlockPos getIoOffset();
}
