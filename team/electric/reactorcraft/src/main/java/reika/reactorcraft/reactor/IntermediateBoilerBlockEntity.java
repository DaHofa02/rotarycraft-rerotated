/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.math.RandomHelper;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.ReactorCraft.Base.TileEntityIntermediateBoiler with TileEntityNuclearBoiler - heated by the reactor parts around it;
 * once a second, at its minimum temperature or more, it turns 100 mB of its coolant into the hot coolant and cools by 100 times the
 * heat capacity. Two 12000 mB tanks: coolant in from below, hot coolant out at the top (standard pipes, up or down); stacked boilers
 * pass 100 mB per tick of each upwards and level their coolant. Sides show the column position.
 */
public abstract class IntermediateBoilerBlockEntity extends ReactorBlockEntity implements Temperatured, ReactorTyped, NeutronTile, ReactorBlock, PipeConnector {

	public static final int CAPACITY = 12000;
	public static final int LIQUID_USAGE = 100;

	protected final FluidTank tank = new FluidTank(CAPACITY, f -> f.getFluid().isSame(this.getInputFluid()));
	protected final FluidTank output = new FluidTank(CAPACITY);
	private int timer;

	protected IntermediateBoilerBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public abstract int getMinimumTemperature();

	protected abstract double getFluidHeatCapacity();

	public abstract Fluid getInputFluid();

	protected abstract Fluid getOutputFluid();

	protected abstract void overheat(Level world, BlockPos pos);

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (this.thermalTick() && !world.isClientSide) {
			this.updateTemperature(world, pos);
			if (temperature > this.getMaxTemperature()) {
				this.overheat(world, pos);
				return;
			}
		}
		this.balanceFluid(world, pos);
		timer++;
		if (timer >= 20) {
			timer = 0;
			if (this.canHeat())
				this.heat();
		}
		this.transferFluid(world, pos);
	}

	public boolean canHeat() {
		return temperature >= this.getMinimumTemperature() && tank.getFluidAmount() >= LIQUID_USAGE && output.getFluidAmount() < CAPACITY
				&& tank.getFluid().getFluid().isSame(this.getInputFluid());
	}

	private void heat() {
		temperature -= LIQUID_USAGE*this.getFluidHeatCapacity();
		tank.drain(LIQUID_USAGE, IFluidHandler.FluidAction.EXECUTE);
		output.fill(new FluidStack(this.getOutputFluid(), LIQUID_USAGE), IFluidHandler.FluidAction.EXECUTE);
		this.setChanged();
	}

	private void transferFluid(Level world, BlockPos pos) {
		if (world.getBlockEntity(pos.above()) instanceof IntermediateBoilerBlockEntity te && te.getClass() == this.getClass()) {
			if (te.tank.getFluidAmount() < CAPACITY && !tank.isEmpty()) {
				int amt = Math.min(tank.getFluidAmount(), Math.min(100, CAPACITY-te.tank.getFluidAmount()));
				te.tank.fill(new FluidStack(tank.getFluid().getFluid(), amt), IFluidHandler.FluidAction.EXECUTE);
				tank.drain(amt, IFluidHandler.FluidAction.EXECUTE);
			}
			if (te.output.getFluidAmount() < CAPACITY && !output.isEmpty()) {
				int amt = Math.min(output.getFluidAmount(), Math.min(100, CAPACITY-te.output.getFluidAmount()));
				te.output.fill(new FluidStack(output.getFluid().getFluid(), amt), IFluidHandler.FluidAction.EXECUTE);
				output.drain(amt, IFluidHandler.FluidAction.EXECUTE);
			}
		}
	}

	/** Original TileEntityNuclearBoiler.balanceFluid. */
	private void balanceFluid(Level world, BlockPos pos) {
		for (Direction dir : new Direction[]{Direction.DOWN, Direction.UP}) {
			if (world.getBlockEntity(pos.relative(dir)) instanceof IntermediateBoilerBlockEntity te && te.getClass() == this.getClass()) {
				if (te.tank.getFluidAmount() < tank.getFluidAmount() && (te.tank.isEmpty() || te.tank.getFluid().getFluid().isSame(tank.getFluid().getFluid()))) {
					int amt = (tank.getFluidAmount()-te.tank.getFluidAmount())/4+1;
					te.tank.fill(new FluidStack(tank.getFluid().getFluid(), amt), IFluidHandler.FluidAction.EXECUTE);
					tank.drain(amt, IFluidHandler.FluidAction.EXECUTE);
				}
			}
		}
	}

	@Override
	public boolean onNeutron(NeutronEntity e, Level world, BlockPos pos) {
		return false;
	}

	@Override
	public boolean canDumpHeatInto(LiquidStates liq) {
		return false;
	}

	public FluidTank getTank() {
		return tank;
	}

	public FluidTank getOutputTank() {
		return output;
	}

	public int getTextureState(Level world, BlockPos pos) {
		Block b = this.getBlockState().getBlock();
		boolean up = world.getBlockState(pos.above()).is(b), down = world.getBlockState(pos.below()).is(b);
		return up && down ? 2 : up ? 1 : down ? 3 : 0;
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return side.getAxis() == Direction.Axis.Y && this.canConnectToPipe(p);
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (from != Direction.DOWN)
			return 0;
		int n = tank.fill(resource, action);
		if (n > 0 && action.execute())
			this.setChanged();
		return n;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (from != Direction.UP)
			return FluidStack.EMPTY;
		FluidStack d = output.drain(maxDrain, action);
		if (!d.isEmpty() && action.execute())
			this.setChanged();
		return d;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side == Direction.UP ? Flow.OUTPUT : side == Direction.DOWN ? Flow.INPUT : Flow.NONE;
	}

	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 2;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return (t == 0 ? tank : output).getFluid();
			}

			@Override
			public int getTankCapacity(int t) {
				return CAPACITY;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return t == 0 && tank.isFluidValid(stack);
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return IntermediateBoilerBlockEntity.this.fill(side, resource, action);
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				if (resource.isEmpty() || !output.getFluid().getFluid().isSame(resource.getFluid()))
					return FluidStack.EMPTY;
				return IntermediateBoilerBlockEntity.this.drain(side, resource.getAmount(), action);
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				return IntermediateBoilerBlockEntity.this.drain(side, max, action);
			}
		};
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.put("output", output.writeToNBT(registries, new CompoundTag()));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		output.setFluid(FluidStack.EMPTY);
		output.readFromNBT(registries, NBT.getCompound("output"));
	}
}
