/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.content.piping.PipingBlockEntity;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.lib.world.BiomeHumidity;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original TileEntityAggregator (MachineRegistry.AGGREGATOR, PowerReceivers: 1 Nm, 4096 rad/s, 8192 W): condenses water out of the air.
 * Power comes from below. While it is colder than the ambient temperature it makes max(2, (int)(torque^2 * biome humidity)) mB spread over
 * max(1, (int)(80 - 5 * log2(omega + 1))) ticks, into a 128000 mB tank that empties sideways into pipes and fluid handlers. Its
 * temperature follows the ambient one by a quarter of the difference per second (integer division: it settles up to 3 degrees off, so a
 * fresh aggregator stays just below ambient and one that was heated stays just above until it is cooled).
 * <p>
 * Not ported: the Galacticraft no-atmosphere check. Pipes draw from the tank themselves (as from the pump); other fluid handlers are
 * filled as in the original.
 */
public class AggregatorBlockEntity extends PowerReceiverBlockEntity implements PipeConnector, TemperatureMachine {

	public static final int CAPACITY = 128000;

	private final FluidTank tank = new FluidTank(CAPACITY);
	private int tempTick;
	private int temperature;

	public AggregatorBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.AGGREGATOR.get(), pos, state, 1, 4096, 8192);
	}

	@Override
	public String getName() {
		return "Aggregator";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		read = Direction.DOWN;
		this.readPower();
		tempTick++;
		if (tempTick >= 20) {
			tempTick = 0;
			this.updateTemperature(world, pos);
		}
		if (!tank.isEmpty())
			this.dumpLiquid(world, pos);
		if (power >= MINPOWER && omega >= MINSPEED && tank.getFluidAmount() < CAPACITY && temperature < this.getMaxTemperature()) {
			if (temperature < AmbientTemperature.getAmbientTemperatureAt(world, pos)) {
				tank.fill(new FluidStack(Fluids.WATER, this.getProductionPerTick(world.getBiome(pos))), IFluidHandler.FluidAction.EXECUTE);
				this.setChanged();
			}
		}
	}

	private void dumpLiquid(Level world, BlockPos pos) {
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			BlockPos to = pos.relative(dir);
			BlockEntity te = world.getBlockEntity(to);
			if (te == null || te instanceof PipingBlockEntity)
				continue;
			IFluidHandler ifl = world.getCapability(Capabilities.FluidHandler.BLOCK, to, dir.getOpposite());
			if (ifl == null)
				continue;
			int added = ifl.fill(tank.getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
			if (added > 0) {
				tank.drain(added, IFluidHandler.FluidAction.EXECUTE);
				this.setChanged();
				if (tank.isEmpty())
					return;
			}
		}
	}

	public int getProductionPerTick(Holder<Biome> biome) {
		if (omega < MINSPEED || power < MINPOWER)
			return 0;
		int n = Math.max(1, this.getOriginalOperationTime());
		return this.getOriginalWaterProduced(biome)/n;
	}

	private int getOriginalOperationTime() {
		return Math.max(0, (int)(80-5*ReikaMathLibrary.logbase(omega+1, 2)));
	}

	private int getOriginalWaterProduced(Holder<Biome> biome) {
		return Math.max(2, (int)(torque*torque*BiomeHumidity.get(biome)));
	}

	public void updateTemperature(Level world, BlockPos pos) {
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		int dT = Tamb-temperature;
		temperature += dT/4;
	}

	public int getWater() {
		return tank.getFluidAmount();
	}

	@Override
	public int getRedstoneOverride() {
		return 15*tank.getFluidAmount()/CAPACITY;
	}

	@Override
	public void addTemperature(int temp) {
		temperature += temp;
	}

	@Override
	public int getTemperature() {
		return temperature;
	}

	@Override
	public void setTemperature(int temp) {
		temperature = Math.max(1, temp);
	}

	@Override
	public int getMaxTemperature() {
		return 100;
	}

	@Override
	public boolean allowExternalHeating() {
		return true;
	}

	@Override
	public boolean canBeCooledWithFins() {
		return true;
	}

	@Override
	public boolean canConnectToPipe(PipeType m) {
		return m.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return side.getAxis() != Direction.Axis.Y;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side.getAxis() != Direction.Axis.Y ? Flow.OUTPUT : Flow.NONE;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		return 0;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (from != null && from.getAxis() == Direction.Axis.Y)
			return FluidStack.EMPTY;
		FluidStack fs = tank.drain(maxDrain, action);
		if (!fs.isEmpty() && action.execute())
			this.setChanged();
		return fs;
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
				return FluidStack.isSameFluidSameComponents(resource, tank.getFluid()) ? AggregatorBlockEntity.this.drain(side, resource.getAmount(), action) : FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int maxDrain, FluidAction action) {
				return AggregatorBlockEntity.this.drain(side, maxDrain, action);
			}
		};
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public void onEMP() {}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.putInt("temp", temperature);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		temperature = NBT.getInt("temp");
	}
}
