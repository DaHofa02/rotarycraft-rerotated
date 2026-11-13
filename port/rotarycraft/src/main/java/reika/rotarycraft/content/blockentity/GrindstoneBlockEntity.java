/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryComponents;
import reika.rotarycraft.registry.RotarySounds;

/**
 * Original: Reika.RotaryCraft.TileEntities.TileEntityGrindstone - sharpens the tool, sword or shears in its slot with water: needs
 * 16384 W and 256 Nm from both ends of its axle (metadata 0: south and north, 1: east and west); every max(1, 80 - 6*log2(omega+1))
 * ticks one point of damage is mended for 100 mB of water. A tool can be mended 2 * max damage times in all, and each mending wears it
 * for good by half a point: it cannot be brought below max damage - ceil(repairs left / 2). Only finished tools come out by automation.
 */
public class GrindstoneBlockEntity extends PowerReceiverBlockEntity implements PipeConnector, net.minecraft.world.Clearable {

	public static final int CAPACITY = 1000;
	/** Original DurationRegistry.GRINDSTONE. */
	private static final int DURATION_BASE = 80, DURATION_SCALE = 6;

	private int soundtick;
	private final FluidTank tank = new FluidTank(CAPACITY, fs -> fs.is(Fluids.WATER));

	private final ItemStackHandler inv = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return isSharpenable(is);
		}

		@Override
		public int getSlotLimit(int slot) {
			return 1;
		}

		@Override
		protected void onContentsChanged(int slot) {
			GrindstoneBlockEntity.this.setChanged();
			GrindstoneBlockEntity.this.syncAllData();
		}
	};

	public GrindstoneBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.GRINDSTONE.get(), pos, state, 256, 1, 16384);
	}

	@Override
	public String getName() {
		return "Grindstone";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public FluidTank getTank() {
		return tank;
	}

	/** Original isItemValidForSlot: damageable shears, swords and tools. */
	public static boolean isSharpenable(ItemStack is) {
		return is.isDamageableItem() && (is.getItem() instanceof ShearsItem || is.getItem() instanceof SwordItem || is.getItem() instanceof DiggerItem);
	}

	public static int getMinimumDamageForItem(ItemStack is) {
		Integer r = is.get(RotaryComponents.GRINDSTONE_REPAIRS.get());
		return r == null ? 0 : is.getMaxDamage()-Mth.ceil(r/2F);
	}

	public boolean hasValidItem() {
		ItemStack is = inv.getStackInSlot(0);
		return !is.isEmpty() && isSharpenable(is) && is.getDamageValue() > getMinimumDamageForItem(is);
	}

	public int getOperationTime() {
		return (int)Math.max(1, DURATION_BASE-DURATION_SCALE*ReikaMathLibrary.logbase(Math.max(0, omega)+1, 2));
	}

	public int getNumberConsecutiveOperations() {
		int threshold = (int)ReikaMathLibrary.intpow(2, 1+DURATION_BASE/DURATION_SCALE);
		return 1+(omega >= threshold ? (int)(ReikaMathLibrary.logbase(omega, 2)-DURATION_BASE/DURATION_SCALE) : 0);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (this.getBlockMetadata() == 1) {
			read = Direction.EAST;
			read2 = Direction.WEST;
		}
		else {
			read = Direction.SOUTH;
			read2 = Direction.NORTH;
		}
		if (world.isClientSide)
			return;
		this.readPowerDoubleSided();
		tickcount++;
		if (power < MINPOWER || torque < MINTORQUE)
			return;
		if (!inv.getStackInSlot(0).isEmpty()) {
			soundtick++;
			if (soundtick > 49) {
				RotarySounds.play(world, pos, RotarySounds.FRICTION.get(), 0.5F, 1);
				soundtick = 0;
			}
		}
		if (tickcount >= this.getOperationTime()) {
			tickcount = 0;
			int n = this.getNumberConsecutiveOperations();
			for (int i = 0; i < n; i++)
				this.doOperation();
		}
	}

	private void doOperation() {
		if (tank.isEmpty() || !this.hasValidItem())
			return;
		ItemStack is = inv.getStackInSlot(0).copy();
		int repairs = is.getOrDefault(RotaryComponents.GRINDSTONE_REPAIRS.get(), is.getMaxDamage()*2);
		is.setDamageValue(is.getDamageValue()-1);
		is.set(RotaryComponents.GRINDSTONE_REPAIRS.get(), repairs-1);
		inv.setStackInSlot(0, is);
		tank.drain(100, IFluidHandler.FluidAction.EXECUTE);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (level == null)
			phi = 0;
		else if (power >= MINPOWER)
			phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
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
				return stack.is(Fluids.WATER);
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return GrindstoneBlockEntity.this.fill(side, resource, action);
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

	/** Tools in from any side; out only once they cannot be mended further (original canExtractItem). */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return new IItemHandler() {
			@Override
			public int getSlots() {
				return 1;
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
				return is.getDamageValue() <= getMinimumDamageForItem(is) ? inv.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
			}

			@Override
			public int getSlotLimit(int slot) {
				return 1;
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return inv.isItemValid(slot, stack);
			}
		};
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(0).copy());
	}

	@Override
	public void clearContent() {
		inv.setStackInSlot(0, ItemStack.EMPTY);
		tank.setFluid(FluidStack.EMPTY);
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
