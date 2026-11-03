/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.api.interfaces.Fillable;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.TileEntities.Auxiliary.TileEntityFillingStation - fills {@link Fillable} items from its 32000 mB tank.
 * Powered from the side its metadata names (at least 1024 W). An ethanol crystal in the fuel slot (1) gives 1000 mB ethanol, a filled
 * container is emptied into the tank (same fluid only). A Fillable item in the input slot (3) moves, one at a time, to the filling slot
 * (0), which gets 4*log2(omega) mB per tick of a fluid it accepts; once full it goes to the output slot (2). Fluid comes in from fuel
 * lines, pipes and hoses on every side except the bottom, where it can be drained. Comparator: 15 when it cannot fill.
 */
public class FillingStationBlockEntity extends PowerReceiverBlockEntity implements PipeConnector, net.minecraft.world.Clearable {

	public static final int CAPACITY = 32000;

	public static final int FUEL_PER_CRYSTAL = 1000;

	public static final int INPUT_SLOT = 3;
	public static final int FUEL_SLOT = 1;
	public static final int OUTPUT_SLOT = 2;
	public static final int FILLING_SLOT = 0;

	private final FluidTank tank = new FluidTank(CAPACITY) {
		@Override
		protected void onContentsChanged() {
			FillingStationBlockEntity.this.setChanged();
		}
	};

	private final ItemStackHandler inv = new ItemStackHandler(4) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return FillingStationBlockEntity.this.isItemValidForSlot(slot, is);
		}

		@Override
		protected void onContentsChanged(int slot) {
			FillingStationBlockEntity.this.setChanged();
		}
	};

	public FillingStationBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.FILLING_STATION.get(), pos, state, 1, 1, 1024);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public FluidTank getTank() {
		return tank;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSidesDefault(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		this.readPower();
		if (power < MINPOWER)
			return;

		if (this.canMakeFuel())
			this.makeFuel();

		if (this.hasFillable()) {
			if (this.canFill())
				this.fill();
		}
		else {
			ItemStack is = inv.getStackInSlot(INPUT_SLOT);
			if (!is.isEmpty() && is.getItem() instanceof Fillable) {
				inv.setStackInSlot(FILLING_SLOT, is.copyWithCount(1));
				is.shrink(1);
				this.syncAllData();
			}
		}
	}

	/** The fluid an item holds and the container it leaves behind (original FluidContainerRegistry / ReikaFluidHelper.getFluidForItem). */
	@Nullable
	private static IFluidHandlerItem handlerFor(ItemStack is) {
		return is.isEmpty() ? null : is.copyWithCount(1).getCapability(Capabilities.FluidHandler.ITEM);
	}

	public boolean canMakeFuel() {
		ItemStack is = inv.getStackInSlot(FUEL_SLOT);
		if (is.isEmpty())
			return false;
		FluidStack fs = FluidUtil.getFluidContained(is).orElse(FluidStack.EMPTY);
		if (fs.isEmpty()) {
			boolean item = is.is(RotaryItems.ETHANOL.get());
			boolean space = tank.getFluidAmount()+FUEL_PER_CRYSTAL <= CAPACITY && (tank.isEmpty() || tank.getFluid().is(RotaryFluids.ETHANOL.get()));
			return item && space;
		}
		if (tank.isEmpty())
			return true;
		return tank.getFluidAmount()+fs.getAmount() <= CAPACITY && FluidStack.isSameFluidSameComponents(tank.getFluid(), fs);
	}

	public void makeFuel() {
		ItemStack is = inv.getStackInSlot(FUEL_SLOT);
		if (is.is(RotaryItems.ETHANOL.get())) {
			tank.fill(new FluidStack(RotaryFluids.ETHANOL.get(), FUEL_PER_CRYSTAL), IFluidHandler.FluidAction.EXECUTE);
			is.shrink(1);
			return;
		}
		IFluidHandlerItem h = handlerFor(is);
		if (h == null)
			return;
		FluidStack fs = h.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
		if (fs.isEmpty())
			return;
		tank.fill(fs, IFluidHandler.FluidAction.EXECUTE);
		ItemStack rest = h.getContainer();
		is.shrink(1);
		if (is.isEmpty())
			inv.setStackInSlot(FUEL_SLOT, rest);
		else if (!rest.isEmpty())
			net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX()+0.5, worldPosition.getY()+1, worldPosition.getZ()+0.5, rest);
	}

	private void fill() {
		ItemStack is = inv.getStackInSlot(FILLING_SLOT);
		Fillable i = (Fillable)is.getItem();
		int added = i.addFluid(is, tank.getFluid().getFluid(), this.getFluidToAdd());
		if (added > 0)
			tank.drain(added, IFluidHandler.FluidAction.EXECUTE);
		if (this.canShuttleItem()) {
			ItemStack out = inv.getStackInSlot(OUTPUT_SLOT);
			if (out.isEmpty())
				inv.setStackInSlot(OUTPUT_SLOT, is.copy());
			else
				out.grow(is.getCount());
			inv.setStackInSlot(FILLING_SLOT, ItemStack.EMPTY);
		}
		this.syncAllData();
	}

	private boolean canShuttleItem() {
		ItemStack is = inv.getStackInSlot(FILLING_SLOT);
		Fillable f = (Fillable)is.getItem();
		ItemStack is2 = inv.getStackInSlot(OUTPUT_SLOT);
		if (is2.isEmpty())
			return f.isFull(is);
		if (!ItemStack.isSameItemSameComponents(is, is2))
			return false;
		if (!f.isFull(is) || !f.isFull(is2))
			return false;
		return is.getCount()+is2.getCount() <= is.getMaxStackSize();
	}

	private int getFluidToAdd() {
		int toadd = 4*(int)ReikaMathLibrary.logbase(omega, 2);
		return Math.min(toadd, tank.getFluidAmount());
	}

	private boolean hasFillable() {
		ItemStack is = inv.getStackInSlot(FILLING_SLOT);
		return !is.isEmpty() && is.getItem() instanceof Fillable;
	}

	private boolean canFill() {
		if (tank.isEmpty())
			return false;
		ItemStack is = inv.getStackInSlot(FILLING_SLOT);
		if (is.isEmpty() || !(is.getItem() instanceof Fillable i))
			return false;
		return i.isValidFluid(tank.getFluid().getFluid(), is) && i.getCapacity(is) > i.getCurrentFillLevel(is);
	}

	public boolean isItemValidForSlot(int i, ItemStack is) {
		if (i == INPUT_SLOT)
			return is.getItem() instanceof Fillable;
		if (i == FUEL_SLOT)
			return FluidUtil.getFluidContained(is).isPresent() || is.is(RotaryItems.ETHANOL.get());
		return false;
	}

	@Nullable
	public Fluid getFluid() {
		return tank.isEmpty() ? null : tank.getFluid().getFluid();
	}

	public int getLiquidScaled(int i) {
		return tank.getFluidAmount()*i/CAPACITY;
	}

	public ItemStack getItemForRender() {
		return inv.getStackInSlot(FILLING_SLOT).copy();
	}

	@Override
	public int getRedstoneOverride() {
		return this.canFill() ? 0 : 15;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p == PipeType.FUEL_LINE || p.isStandardPipe() || p == PipeType.HOSE;
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p);
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side == Direction.DOWN ? Flow.OUTPUT : Flow.INPUT;
	}

	/** Original canReceiveFrom (every side) and the InventoriedPowerLiquidInOut tank: any fluid while empty, then only that one. */
	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (from == Direction.DOWN)
			return 0;
		return tank.fill(resource, action);
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (from != Direction.DOWN)
			return FluidStack.EMPTY;
		return tank.drain(maxDrain, action);
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
				return true;
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return FillingStationBlockEntity.this.fill(side, resource, action);
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				return FluidStack.isSameFluidSameComponents(resource, tank.getFluid()) ? FillingStationBlockEntity.this.drain(side, resource.getAmount(), action) : FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				return FillingStationBlockEntity.this.drain(side, max, action);
			}
		};
	}

	/** Original canExtractItem: only the output slot; insertion by isItemValidForSlot (input and fuel slots). */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return new IItemHandler() {
			@Override
			public int getSlots() {
				return 4;
			}

			@Override
			public ItemStack getStackInSlot(int slot) {
				return inv.getStackInSlot(slot);
			}

			@Override
			public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
				return inv.insertItem(slot, stack, simulate);
			}

			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				return slot == OUTPUT_SLOT ? inv.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
			}

			@Override
			public int getSlotLimit(int slot) {
				return 64;
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return isItemValidForSlot(slot, stack);
			}
		};
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < inv.getSlots(); i++)
			net.minecraft.world.Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}

	@Override
	public void clearContent() {
		for (int i = 0; i < inv.getSlots(); i++)
			inv.setStackInSlot(i, ItemStack.EMPTY);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
