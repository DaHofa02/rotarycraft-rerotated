/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.api.interfaces.Screwdriverable;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.TileEntityBucketFiller - fills fluid containers (buckets and any other item that holds
 * fluid) in its 18 slots from its 24000 mB tank, or, switched with the screwdriver, empties them into it. Needs 2048 W and 512 rad/s
 * from any side; acts once every max(1, 200 - 20*log2(omega+1)) + 1 ticks: filling every empty container it can, emptying one. Liquid
 * comes in (filling) or goes out (emptying) on the four sides; containers come out at the bottom only when done.
 */
public class BucketFillerBlockEntity extends PowerReceiverBlockEntity implements PipeConnector, Screwdriverable, net.minecraft.world.Clearable {

	public static final int CAPACITY = 24000;

	public boolean filling = true;

	private final FluidTank tank = new FluidTank(CAPACITY);

	private final ItemStackHandler inv = new ItemStackHandler(18) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return fluidHandler(is) != null;
		}

		@Override
		protected void onContentsChanged(int slot) {
			BucketFillerBlockEntity.this.setChanged();
		}
	};

	public BucketFillerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.BUCKET_FILLER.get(), pos, state, 1, 512, 2048);
	}

	@Override
	public String getName() {
		return "Bucket Filler";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public FluidTank getTank() {
		return tank;
	}

	@Nullable
	private static IFluidHandlerItem fluidHandler(ItemStack is) {
		return is.isEmpty() ? null : is.copyWithCount(1).getCapability(Capabilities.FluidHandler.ITEM);
	}

	private static boolean isEmptyContainer(ItemStack is) {
		IFluidHandlerItem h = fluidHandler(is);
		return h != null && h.getFluidInTank(0).isEmpty();
	}

	private static boolean isFilledContainer(ItemStack is) {
		IFluidHandlerItem h = fluidHandler(is);
		return h != null && !h.getFluidInTank(0).isEmpty();
	}

	public int getOperationTime() {
		return (int)Math.max(1, 200-20*ReikaMathLibrary.logbase(Math.max(0, omega)+1, 2));
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		tickcount++;
		this.readPowerSummative();
		if (power < MINPOWER || omega < MINSPEED || tickcount <= this.getOperationTime())
			return;
		tickcount = 0;
		if (filling)
			this.fillBuckets(world, pos);
		else
			this.emptyBuckets(world, pos);
	}

	private void fillBuckets(Level world, BlockPos pos) {
		for (int i = 0; i < inv.getSlots(); i++) {
			ItemStack slot = inv.getStackInSlot(i);
			if (tank.isEmpty() || !isEmptyContainer(slot))
				continue;
			IFluidHandlerItem h = fluidHandler(slot);
			int amt = h.fill(tank.getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
			if (amt <= 0)
				continue;
			tank.drain(amt, IFluidHandler.FluidAction.EXECUTE);
			inv.extractItem(i, 1, false);
			this.store(world, pos, h.getContainer());
		}
	}

	private void emptyBuckets(Level world, BlockPos pos) {
		for (int i = 0; i < inv.getSlots(); i++) {
			ItemStack slot = inv.getStackInSlot(i);
			if (!isFilledContainer(slot))
				continue;
			IFluidHandlerItem h = fluidHandler(slot);
			FluidStack fs = h.getFluidInTank(0);
			if (!this.canAccept(fs) || tank.getFluidAmount()+fs.getAmount() > CAPACITY)
				continue;
			FluidStack out = h.drain(fs.getAmount(), IFluidHandler.FluidAction.EXECUTE);
			if (out.isEmpty())
				continue;
			inv.extractItem(i, 1, false);
			this.store(world, pos, h.getContainer());
			tank.fill(out, IFluidHandler.FluidAction.EXECUTE);
			return;
		}
	}

	/** Original ReikaInventoryHelper.addToIInv, else dropped on top. */
	private void store(Level world, BlockPos pos, ItemStack is) {
		if (is.isEmpty())
			return;
		ItemStack left = ItemHandlerHelper.insertItemStacked(inv, is, false);
		if (!left.isEmpty())
			Containers.dropItemStack(world, pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5, left);
	}

	public boolean canAccept(FluidStack fs) {
		return tank.isEmpty() || tank.getFluid().is(fs.getFluid());
	}

	@Override
	public boolean onShiftRightClick(Level world, BlockPos pos, Direction side) {
		return this.onRightClick(world, pos, side);
	}

	/** Original ItemScrewdriver: switches between filling and emptying. */
	@Override
	public boolean onRightClick(Level world, BlockPos pos, Direction side) {
		filling = !filling;
		this.syncAllData();
		this.setChanged();
		return true;
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
		return p == PipeType.HOSE || p.isStandardPipe() || p == PipeType.FUEL_LINE;
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return side.getAxis() != Direction.Axis.Y;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side.getAxis() == Direction.Axis.Y ? Flow.NONE : filling ? Flow.INPUT : Flow.OUTPUT;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (!filling || from == null || from.getAxis() == Direction.Axis.Y || !this.canAccept(resource))
			return 0;
		int amt = tank.fill(resource, action);
		if (amt > 0 && action.execute())
			this.setChanged();
		return amt;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (filling || from == null || from.getAxis() == Direction.Axis.Y)
			return FluidStack.EMPTY;
		FluidStack out = tank.drain(maxDrain, action);
		if (!out.isEmpty() && action.execute())
			this.setChanged();
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
				return CAPACITY;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return canAccept(stack);
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return BucketFillerBlockEntity.this.fill(side, resource, action);
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				return tank.getFluid().is(resource.getFluid()) ? BucketFillerBlockEntity.this.drain(side, resource.getAmount(), action) : FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int maxDrain, FluidAction action) {
				return BucketFillerBlockEntity.this.drain(side, maxDrain, action);
			}
		};
	}

	/** Containers in from any side; out at the bottom only, filled ones while filling and empty ones while emptying (original canExtractItem). */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return new IItemHandler() {
			@Override
			public int getSlots() {
				return inv.getSlots();
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
				ItemStack is = inv.getStackInSlot(slot);
				boolean ok = side == Direction.DOWN && (filling ? isFilledContainer(is) : isEmptyContainer(is));
				return ok ? inv.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
			}

			@Override
			public int getSlotLimit(int slot) {
				return 64;
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return inv.isItemValid(slot, stack);
			}
		};
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < inv.getSlots(); i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}

	@Override
	public void clearContent() {
		for (int i = 0; i < inv.getSlots(); i++)
			inv.setStackInSlot(i, ItemStack.EMPTY);
		tank.setFluid(FluidStack.EMPTY);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("filling", filling);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		filling = !NBT.contains("filling") || NBT.getBoolean("filling");
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
