/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryComponents;

/**
 * Original: Reika.RotaryCraft.TileEntities.Storage.TileEntityFluidCompressor (the gas tank) - stores one fluid; how much it takes depends
 * on the torque from below (at least 16384 W and 1024 Nm): 10^(floor(log2(torque)/2)) / 40 mB, eight times that for gases (2500 mB of a
 * liquid at 1024 Nm, 250000 at 16384 Nm), at most 10^9. Without power it takes nothing but keeps what it has. Fluids come in on every
 * side but the top, where pipes take them out; any side can be drained. Broken, it keeps its contents in the item. Not ported: the
 * creative variants pre-filled with gases.
 */
public class FluidCompressorBlockEntity extends PowerReceiverBlockEntity implements PipeConnector {

	public static final int MAX = 1000000000;

	private final FluidTank tank = new FluidTank(MAX);

	public FluidCompressorBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.FLUID_COMPRESSOR.get(), pos, state, 1024, 1, 16384);
	}

	@Override
	public String getName() {
		return "Fluid Compressor";
	}

	public FluidTank getTank() {
		return tank;
	}

	/** Original getCapacity(Fluid). */
	public int getCapacity(Fluid f) {
		if (power < MINPOWER || torque < MINTORQUE)
			return 0;
		int log2 = (int)(ReikaMathLibrary.logbase(torque, 2)/2);
		long pow = ReikaMathLibrary.longpow(10, log2);
		int factor = isGaseous(f) ? 8 : 1;
		return (int)Math.min(factor*(pow/40), MAX);
	}

	/**
	 * Original Fluid.isGaseous: the common tag c:gaseous (ReactorCraft tags its gases there, as RotaryCraft cannot see its gas flags) or
	 * a fluid lighter than air.
	 */
	public static boolean isGaseous(Fluid f) {
		return f.is(net.neoforged.neoforge.common.Tags.Fluids.GASEOUS) || f.getFluidType().isLighterThanAir();
	}

	public int getCapacity() {
		return tank.isEmpty() ? 0 : this.getCapacity(tank.getFluid().getFluid());
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		read = Direction.DOWN;
		this.readPower();
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe() || p == PipeType.HOSE || p == PipeType.FUEL_LINE || p == PipeType.VALVE;
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p);
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side == Direction.UP ? Flow.OUTPUT : Flow.INPUT;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (resource.isEmpty())
			return 0;
		int toadd = Math.min(resource.getAmount(), this.getCapacity(resource.getFluid())-tank.getFluidAmount());
		if (toadd <= 0)
			return 0;
		int amt = tank.fill(resource.copyWithAmount(toadd), action);
		if (amt > 0 && action.execute()) {
			this.setChanged();
			this.syncAllData();
		}
		return amt;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		FluidStack out = tank.drain(maxDrain, action);
		if (!out.isEmpty() && action.execute()) {
			this.setChanged();
			this.syncAllData();
		}
		return out;
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
				return tank.isEmpty() ? 0 : FluidCompressorBlockEntity.this.getCapacity();
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return tank.isEmpty() || tank.getFluid().is(stack.getFluid());
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return FluidCompressorBlockEntity.this.fill(side, resource, action);
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				return tank.getFluid().is(resource.getFluid()) ? FluidCompressorBlockEntity.this.drain(side, resource.getAmount(), action) : FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int maxDrain, FluidAction action) {
				return FluidCompressorBlockEntity.this.drain(side, maxDrain, action);
			}
		};
	}

	@Override
	protected void applyImplicitComponents(DataComponentInput input) {
		super.applyImplicitComponents(input);
		SimpleFluidContent c = input.get(RotaryComponents.TANK_FLUID.get());
		if (c != null)
			tank.setFluid(c.copy());
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (!tank.isEmpty())
			components.set(RotaryComponents.TANK_FLUID.get(), SimpleFluidContent.copyOf(tank.getFluid()));
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
	}
}
