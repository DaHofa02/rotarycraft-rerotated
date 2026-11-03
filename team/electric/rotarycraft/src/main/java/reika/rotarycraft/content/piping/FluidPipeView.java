/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.piping;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;

/** What PipeFluidRenderer needs from a pipe (RotaryCraft pipes and other mods' pipes drawn the same way, original RenderableDuct). */
public interface FluidPipeView {

	@Nullable
	Fluid getFluidType();

	int getFluidLevel();

	boolean isConnectedDirectly(Direction dir);
}
