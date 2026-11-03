/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.piping;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Original: Reika.RotaryCraft.Auxiliary.Interfaces.PipeConnector. A RotaryCraft machine that decides itself which pipe types
 * may connect on which side and in which direction fluid flows. {@code side} is always the machine's side that faces the pipe.
 */
public interface PipeConnector {

	boolean canConnectToPipe(PipeType p);

	boolean canConnectToPipeOnSide(PipeType p, Direction side);

	int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action);

	FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action);

	Flow getFlowForSide(Direction side);

	/** Original TileEntityPiping.Flow. */
	enum Flow {
		INPUT(true, false),
		OUTPUT(false, true),
		DUAL(true, true),
		NONE(false, false);

		public final boolean canIntake;
		public final boolean canOutput;

		Flow(boolean in, boolean out) {
			canIntake = in;
			canOutput = out;
		}
	}
}
