/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.powergen;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.reactorcraft.content.block.SteamBlock;
import reika.reactorcraft.reactor.ReactorBoilerBlockEntity;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;

/**
 * Original: Reika.ReactorCraft.TileEntities.PowerGen.TileEntityCondenser - takes the steam block under it and turns it into 200 mB of
 * low pressure water (ammonia steam: low pressure ammonia) in a 12000 mB tank; condensers next to each other level their tanks (a
 * quarter of the difference). The liquid is taken out at the top only (standard pipes). Its temperature never changes in the
 * original, so its "below 100 C" condition always holds.
 */
public class CondenserBlockEntity extends RotaryCraftBlockEntity implements PipeConnector {

	public static final int CAPACITY = 12000;

	private final FluidTank tank = new FluidTank(CAPACITY);
	private int temperature;

	public CondenserBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.CONDENSER.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Condenser";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		BlockState below = world.getBlockState(pos.below());
		if (below.getBlock() instanceof SteamBlock && tank.getFluidAmount() < CAPACITY && temperature < 100 && !world.isClientSide) {
			Fluid f = getFluidFromSteamMetadata(below.getValue(SteamBlock.FLAGS));
			if (tank.isEmpty() || tank.getFluid().getFluid().isSame(f)) {
				world.removeBlock(pos.below(), false);
				tank.fill(new FluidStack(f, ReactorBoilerBlockEntity.WATER_PER_STEAM), IFluidHandler.FluidAction.EXECUTE);
				this.setChanged();
			}
		}
		this.balance(world, pos);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	private static Fluid getFluidFromSteamMetadata(int smeta) {
		return ReactorFluids.get((smeta&4) == 4 ? "low_pressure_ammonia" : "low_pressure_water");
	}

	private void balance(Level world, BlockPos pos) {
		for (Direction dir : Direction.values()) {
			if (world.getBlockEntity(pos.relative(dir)) instanceof CondenserBlockEntity te) {
				int dL = te.tank.getFluidAmount()-tank.getFluidAmount();
				if (dL/4 > 0) {
					tank.fill(new FluidStack(te.tank.getFluid().getFluid(), dL/4), IFluidHandler.FluidAction.EXECUTE);
					te.tank.drain(dL/4, IFluidHandler.FluidAction.EXECUTE);
				}
			}
		}
	}

	public FluidTank getTank() {
		return tank;
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return side == Direction.UP && this.canConnectToPipe(p);
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		return 0;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (from != Direction.UP)
			return FluidStack.EMPTY;
		FluidStack d = tank.drain(maxDrain, action);
		if (!d.isEmpty() && action.execute())
			this.setChanged();
		return d;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side == Direction.UP ? Flow.OUTPUT : Flow.NONE;
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
				return CAPACITY;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return false;
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return 0;
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				if (resource.isEmpty() || !tank.getFluid().getFluid().isSame(resource.getFluid()))
					return FluidStack.EMPTY;
				return CondenserBlockEntity.this.drain(side, resource.getAmount(), action);
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				return CondenserBlockEntity.this.drain(side, max, action);
			}
		};
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.putInt("temp", temperature);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		temperature = NBT.getInt("temp");
	}
}
