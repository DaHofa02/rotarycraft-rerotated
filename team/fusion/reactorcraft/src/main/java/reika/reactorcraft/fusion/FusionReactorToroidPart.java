/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.fusion;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Original: Reika.ReactorCraft.Auxiliary.FusionReactorToroidPart - a link of the toroid ring (magnets and injectors). */
public interface FusionReactorToroidPart {

	@Nullable
	FusionReactorToroidPart getNextPart(Level world, BlockPos pos);
}
