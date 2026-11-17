/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.registry.DifficultyEffects;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryFluids;

/**
 * Original: Reika.RotaryCraft.TileEntities.Transmission.TileEntityBusController - reads shaft power from its input side (meta 0 east, 1
 * west, 2 north, 3 south) and runs the power bus blocks attached to it. It needs lubricant (8000 mB, by hose or bedrock pipe from above or
 * below): without any it passes nothing; while running it uses max(1, LUBEUSAGE * 2 * blocks + output sides) mB every 100 ticks.
 */
public class BusControllerBlockEntity extends PowerReceiverBlockEntity implements PipeConnector {

	public static final int CAPACITY = 8000;

	private final FluidTank tank = new FluidTank(CAPACITY, fs -> fs.is(RotaryFluids.LUBRICANT.get()));
	@Nullable
	private ShaftPowerBus bus = new ShaftPowerBus(this);
	private int timer;
	private int busTick;

	public BusControllerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.BUS_CONTROLLER.get(), pos, state, 1, 1, 1);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSides(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		this.readPower();
		if (tank.isEmpty()) {
			torque = 0;
			omega = 0;
		}
		else if (power > 0 && ++timer >= 100) {
			timer = 0;
			tank.drain(this.getLubricantUsed(), IFluidHandler.FluidAction.EXECUTE);
			this.setChanged();
		}
		power = (long)torque*(long)omega;
		if (++busTick >= 10 && bus != null) {
			busTick = 0;
			bus.update();
		}
	}

	private int getLubricantUsed() {
		return bus == null ? 1 : Math.max(1, (int)(DifficultyEffects.LUBEUSAGE.getChance()*2*bus.getSize()+bus.getTotalOutputSides()));
	}

	private void getIOSides(int meta) {
		read = switch (meta) {
			case 0 -> Direction.EAST;
			case 1 -> Direction.WEST;
			case 2 -> Direction.NORTH;
			default -> Direction.SOUTH;
		};
	}

	@Nullable
	public ShaftPowerBus getBus() {
		return bus;
	}

	public int getLubricant() {
		return tank.getFluidAmount();
	}

	public void setLubricant(int amt) {
		tank.setFluid(amt > 0 ? new FluidStack(RotaryFluids.LUBRICANT.get(), amt) : FluidStack.EMPTY);
	}

	/** Original BreakAction: the bus blocks lose their controller. */
	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		if (bus != null)
			bus.clear();
		bus = null;
	}

	@Override
	public void getAllOutputs(Collection<BlockEntity> c, Direction dir) {
		if (bus != null)
			for (PowerBusBlockEntity te : bus.getBlocks())
				te.getAllOutputs(c, dir);
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p == PipeType.HOSE || p == PipeType.BEDROCK_PIPE;
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p) && side.getAxis() == Direction.Axis.Y;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side.getAxis() == Direction.Axis.Y ? Flow.INPUT : Flow.NONE;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (from != null && from.getAxis() != Direction.Axis.Y)
			return 0;
		int amt = tank.fill(resource, action);
		if (amt > 0 && action.execute())
			this.setChanged();
		return amt;
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
				return CAPACITY;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return stack.is(RotaryFluids.LUBRICANT.get());
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return BusControllerBlockEntity.this.fill(side, resource, action);
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				return FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int maxDrain, FluidAction action) {
				return FluidStack.EMPTY;
			}
		};
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
	}
}
