/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.reactorcraft.content.block.ThoriumFuelBlock;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorBlocks;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;

/**
 * Original: Reika.ReactorCraft.TileEntities.Fission.Thorium.TileEntityFuelDump - under a thorium core: from 1100 C it drains the core's
 * fuel into its 2000 mB tank (if it stays full for 10 s while the core is that hot it explodes into corium) and lets it out below as
 * thorium fuel layers (125 mB each, up to 8). Pipes on it reach the core above (it passes drains on).
 */
public class FuelDumpBlockEntity extends RotaryCraftBlockEntity implements PipeConnector, ReactorBlock {

	public static final int CAPACITY = 2000;

	private final FluidTank tank = new FluidTank(CAPACITY);
	private int fullTicks;

	public FuelDumpBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.FUEL_DUMP.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Emergency Fuel Dump";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (world.isClientSide)
			return;
		if (world.getBlockEntity(pos.above()) instanceof ThoriumCoreBlockEntity tc) {
			if (tc.getTemperature() >= ThoriumCoreBlockEntity.FUEL_DUMP_TEMPERATURE && tc.hasFuel()) {
				int rem = tank.getSpace();
				if (rem > 0) {
					int fuel = tc.dumpFuel(rem);
					if (fuel > 0)
						tank.fill(new FluidStack(ReactorFluids.get("lifbe_fuel"), fuel), IFluidHandler.FluidAction.EXECUTE);
					fullTicks = 0;
				}
				else {
					fullTicks++;
					if (fullTicks > 200) {
						this.overload(world, pos);
						return;
					}
				}
			}
		}
		if (tank.getFluidAmount() >= 125 && ThoriumFuelBlock.canDumpAt(world, pos.below()))
			this.dumpFuel(world, pos);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	private void dumpFuel(Level world, BlockPos pos) {
		BlockPos p = pos.below();
		int n1 = Math.min(8, tank.getFluidAmount()/125);
		int n2 = n1-1;
		BlockState s = world.getBlockState(p);
		if (s.is(ReactorBlocks.THORIUM_FUEL.get())) {
			int fmeta = s.getValue(ThoriumFuelBlock.LEVEL);
			n1 = Math.min(n1, 7-fmeta);
			n2 = n1+fmeta;
		}
		tank.drain(n1*125, IFluidHandler.FluidAction.EXECUTE);
		world.setBlockAndUpdate(p, ReactorBlocks.THORIUM_FUEL.get().defaultBlockState().setValue(ThoriumFuelBlock.LEVEL, Math.max(0, Math.min(7, n2))));
		fullTicks = 0;
		world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1, 1);
	}

	private void overload(Level world, BlockPos pos) {
		world.removeBlock(pos, false);
		world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 3, true, Level.ExplosionInteraction.BLOCK);
		world.setBlockAndUpdate(pos, ReactorBlocks.CORIUM.get().defaultBlockState());
	}

	public FluidTank getTank() {
		return tank;
	}

	@Nullable
	private ThoriumCoreBlockEntity getCore() {
		return level != null && level.getBlockEntity(worldPosition.above()) instanceof ThoriumCoreBlockEntity tc ? tc : null;
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		ThoriumCoreBlockEntity c = this.getCore();
		return c != null && c.canConnectToPipe(p);
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p);
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return this.getCore() != null ? Flow.OUTPUT : Flow.NONE;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		return 0;
	}

	/** Original drain: passed on to the core above. */
	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		ThoriumCoreBlockEntity c = this.getCore();
		return c != null ? c.drain(from, maxDrain, action) : FluidStack.EMPTY;
	}

	/** Original IFluidHandler of TileEntityTankedReactorMachine: nothing goes in, drains are passed on to the core above. */
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
				FluidStack can = FuelDumpBlockEntity.this.drain(side, resource.getAmount(), FluidAction.SIMULATE);
				if (can.isEmpty() || !can.getFluid().isSame(resource.getFluid()))
					return FluidStack.EMPTY;
				return FuelDumpBlockEntity.this.drain(side, resource.getAmount(), action);
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				return FuelDumpBlockEntity.this.drain(side, max, action);
			}
		};
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.putInt("full", fullTicks);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		fullTicks = NBT.getInt("full");
	}
}
