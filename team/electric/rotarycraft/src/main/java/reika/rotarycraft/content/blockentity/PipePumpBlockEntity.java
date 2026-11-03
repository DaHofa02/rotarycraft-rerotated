/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import reika.rotarycraft.api.interfaces.PumpablePipe;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Auxiliary.TileEntityPipePump - a pump inline in a pipe (6 orientations). It takes shaft power
 * from any side (all sides delivering must turn at the same speed; torques add up) and needs 4096 W at 4096 rad/s; then every tick it
 * moves min(level, omega/4) mB from the pipe on its input side (the metadata's read side) into the pipe on the opposite side, if that
 * one is connected both ways and holds the same liquid or none. Pipes (and ReactorCraft steam lines) connect only on that axis.
 */
public class PipePumpBlockEntity extends PowerReceiverBlockEntity implements PipeConnector {

	public PipePumpBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.PIPE_PUMP.get(), pos, state, 1, 4096, 4096);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.readPowerSummative();
		this.getIOSidesDefault(this.getBlockMetadata());

		if (this.getTicksExisted() == 0 && !world.isClientSide)
			world.updateNeighborsAt(pos, state.getBlock());

		if (!world.isClientSide && power >= MINPOWER && omega >= MINSPEED && read != null) {
			Direction dir = read.getOpposite();
			Direction dir2 = dir.getOpposite();
			BlockEntity te = this.getAdjacentTileEntity(dir);
			BlockEntity te2 = this.getAdjacentTileEntity(dir2);
			if (te instanceof PumpablePipe p1 && te2 instanceof PumpablePipe p2) {
				int lvl2 = p2.getFluidLevel();
				if (p2.canTransferTo(p1, dir))
					p1.transferFrom(p2, this.getTransferrableAmount(lvl2));
			}
		}
	}

	private int getTransferrableAmount(int amt) {
		return Math.min(amt, omega/4);
	}

	/** Original canConnectToPipeOnSide (PipeRenderConnector): the read axis. */
	public boolean connectsOnSide(Direction dir) {
		return read != null && (dir == read || dir.getOpposite() == read);
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return true;
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		this.getIOSidesDefault(this.getBlockMetadata());
		return this.connectsOnSide(side);
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		return 0;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		return FluidStack.EMPTY;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return Flow.NONE;
	}
}
