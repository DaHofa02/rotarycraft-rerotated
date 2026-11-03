/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.powergen;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.registry.RotaryFluids;

/**
 * Original: Reika.ReactorCraft.TileEntities.PowerGen.TileEntitySteamInjector - the injector parts of the high pressure turbine
 * multiblock: a 1000 mB lubricant tank (hoses and bedrock pipes, from every side) that the first turbine stage draws from.
 */
public class SteamInjectorBlockEntity extends BlockEntity implements PipeConnector {

	private final FluidTank tank = new FluidTank(1000, f -> f.getFluid().isSame(RotaryFluids.LUBRICANT.get()));

	public SteamInjectorBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.STEAM_INJECTOR.get(), pos, state);
	}

	public int getLubricant() {
		return tank.getFluidAmount();
	}

	void remove(int amt) {
		tank.drain(amt, IFluidHandler.FluidAction.EXECUTE);
		this.setChanged();
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p == PipeType.HOSE || p == PipeType.BEDROCK_PIPE;
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p);
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return Flow.INPUT;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		int n = tank.fill(resource, action);
		if (n > 0 && action.execute())
			this.setChanged();
		return n;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		return FluidStack.EMPTY;
	}

	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 1;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return tank.getFluid();
			}

			@Override
			public int getTankCapacity(int t) {
				return 1000;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return tank.isFluidValid(stack);
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return SteamInjectorBlockEntity.this.fill(side, resource, action);
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				return FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				return FluidStack.EMPTY;
			}
		};
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, tag.getCompound("tank"));
	}
}
