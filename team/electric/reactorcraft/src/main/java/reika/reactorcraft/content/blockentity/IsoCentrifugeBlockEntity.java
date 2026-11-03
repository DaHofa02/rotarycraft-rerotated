/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;
import reika.reactorcraft.registry.ReactorItems;
import reika.rotarycraft.api.power.PowerTransferHelper;
import reika.rotarycraft.api.power.ShaftPowerReceiver;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;

/**
 * Original: Reika.ReactorCraft.TileEntities.Processing.TileEntityCentrifuge (Isotope Centrifuge). Powered from below at 262144 rad/s or
 * more (any torque), it takes 50 mB uranium hexafluoride per step and makes enriched uranium dust with 9 % chance, else depleted uranium
 * dust. Step time 900 ticks, shorter from 524288 rad/s (800) up to 67108864 rad/s (8). 12000 mB tank filled only from the top; the dusts
 * can be taken out at the top and bottom.
 */
public class IsoCentrifugeBlockEntity extends RotaryCraftBlockEntity implements ShaftPowerReceiver, PipeConnector, net.minecraft.world.Clearable {

	public static final int MINSPEED = 262144;
	public static final int UF6_PER_DUST = 50;
	public static final int FUEL_CHANCE = 9;

	private int omega, torque, iotick;
	private long power;
	private final FluidTank tank = new FluidTank(12000);
	private int timer;
	public int split;

	private final ItemStackHandler inv = new ItemStackHandler(2) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return false;
		}

		@Override
		protected void onContentsChanged(int slot) {
			IsoCentrifugeBlockEntity.this.setChanged();
		}
	};

	public IsoCentrifugeBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.ISO_CENTRIFUGE.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Isotope Centrifuge";
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (omega >= 262144)
			phi += 40;
		else if (omega >= 65536)
			phi += 30;
		else if (omega >= 16384)
			phi += 20;
		else if (omega >= 4096)
			phi += 15;
		if (omega >= 1024)
			phi += 10;
		if (omega >= 256)
			phi += 7;
		else if (omega > 0)
			phi += 5;
		iotick -= 8;
	}

	/** Original setTimer. */
	private int getCap() {
		if (omega >= 67108864)
			return 8;
		if (omega >= 33554432)
			return 20;
		if (omega >= 16777216)
			return 50;
		if (omega >= 8388608)
			return 100;
		if (omega >= 4194304)
			return 240;
		if (omega >= 2097152)
			return 400;
		if (omega >= 1048576)
			return 600;
		if (omega >= 524288)
			return 800;
		return 900;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (world.isClientSide)
			return;
		if (!PowerTransferHelper.checkPowerFrom(this, Direction.DOWN))
			this.noInputMachine();
		if (power > 0 && omega >= MINSPEED && tank.getFluidAmount() >= UF6_PER_DUST && this.isUF6() && this.hasInventorySpace()) {
			timer++;
			if (timer >= this.getCap()) {
				timer = 0;
				this.make();
			}
		}
		else {
			timer = 0;
		}
		split = timer;
	}

	private boolean isUF6() {
		return !tank.isEmpty() && tank.getFluid().getFluid().isSame(ReactorFluids.get("uranium_hexafluoride"));
	}

	private ItemStack outA() {
		return new ItemStack(ReactorItems.raw(ReactorItems.Raw.FUELDUST).get());
	}

	private ItemStack outB() {
		return new ItemStack(ReactorItems.raw(ReactorItems.Raw.DEPDUST).get());
	}

	private void make() {
		tank.drain(UF6_PER_DUST, IFluidHandler.FluidAction.EXECUTE);
		if (rand.nextFloat()*100 < FUEL_CHANCE)
			this.addOrSet(0, this.outA());
		else
			this.addOrSet(1, this.outB());
		this.setChanged();
	}

	/** Original ReikaInventoryHelper.addOrSetStack (the slots refuse items from outside, so not insertItem). */
	private void addOrSet(int slot, ItemStack is) {
		ItemStack has = inv.getStackInSlot(slot);
		if (has.isEmpty())
			inv.setStackInSlot(slot, is);
		else
			inv.setStackInSlot(slot, has.copyWithCount(has.getCount()+is.getCount()));
	}

	private boolean hasInventorySpace() {
		ItemStack a = inv.getStackInSlot(0), b = inv.getStackInSlot(1);
		if (!a.isEmpty() && (!ItemStack.isSameItem(a, this.outA()) || a.getCount() >= a.getMaxStackSize()))
			return false;
		if (!b.isEmpty() && (!ItemStack.isSameItem(b, this.outB()) || b.getCount() >= b.getMaxStackSize()))
			return false;
		return true;
	}

	public int getProcessingScaled(int p) {
		return (int)(p*split/(float)this.getCap());
	}

	public FluidTank getTank() {
		return tank;
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	/** Dusts out at the top and bottom, nothing in (original canItemExitToSide / canItemEnterFromSide). */
	@Nullable
	public IItemHandler getItemHandler(@Nullable Direction side) {
		if (side != null && side.getAxis().isHorizontal())
			return null;
		return new IItemHandler() {
			@Override
			public int getSlots() {
				return 2;
			}

			@Override
			public ItemStack getStackInSlot(int slot) {
				return inv.getStackInSlot(slot);
			}

			@Override
			public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
				return stack;
			}

			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				return inv.extractItem(slot, amount, simulate);
			}

			@Override
			public int getSlotLimit(int slot) {
				return 64;
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return false;
			}
		};
	}

	/** Fill only from the top with uranium hexafluoride; nothing comes out. */
	@Nullable
	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		if (side != Direction.UP)
			return null;
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
				return tank.getCapacity();
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return stack.getFluid().isSame(ReactorFluids.get("uranium_hexafluoride"));
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return IsoCentrifugeBlockEntity.this.fill(Direction.UP, resource, action);
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
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (from != Direction.UP || !resource.getFluid().isSame(ReactorFluids.get("uranium_hexafluoride")))
			return 0;
		int n = tank.fill(resource, action);
		if (n > 0 && action.execute())
			this.setChanged();
		return n;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		return FluidStack.EMPTY;
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p) && side == Direction.UP;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side == Direction.UP ? Flow.INPUT : Flow.NONE;
	}

	@Override
	public void setOmega(int omega) {
		this.omega = omega;
	}

	@Override
	public void setTorque(int torque) {
		this.torque = torque;
	}

	@Override
	public void setPower(long power) {
		this.power = power;
	}

	@Override
	public int getOmega() {
		return omega;
	}

	@Override
	public int getTorque() {
		return torque;
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public boolean canReadFrom(Direction dir) {
		return dir == Direction.DOWN;
	}

	@Override
	public boolean isReceiving() {
		return true;
	}

	@Override
	public void noInputMachine() {
		omega = torque = 0;
		power = 0;
	}

	@Override
	public int getMinTorque(int available) {
		return 1;
	}

	@Override
	public int getIORenderAlpha() {
		return iotick;
	}

	@Override
	public void setIORenderAlpha(int io) {
		iotick = io;
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
		NBT.putInt("time", split);
		NBT.putInt("omg", omega);
		NBT.putInt("tq", torque);
		NBT.putLong("pwr", power);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		split = NBT.getInt("time");
		omega = NBT.getInt("omg");
		torque = NBT.getInt("tq");
		power = NBT.getLong("pwr");
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
