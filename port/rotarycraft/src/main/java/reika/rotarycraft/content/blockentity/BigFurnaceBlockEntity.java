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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Processing.TileEntityBigFurnace (the lava smeltery) - 18 input slots, each with its own
 * output slot below. Needs 2048 W from below and at least 400 deg C; then every 200 ticks (150 from 600 deg C, half that from 1000 deg C)
 * one item of every input slot is smelted (vanilla furnace recipes). Lava from pipes on the four sides goes into a 16000 mB tank; every
 * second 15 mB are used and raise the temperature it moves towards by 600 (otherwise the ambient one; by 1 deg C a second, 2 while
 * far off). Above 100 deg C it melts a snow block beside it and turns ice beside it into water. Not ported: pyrotheum (Thermal
 * Expansion); the original's XP counter is never raised and is left out.
 */
public class BigFurnaceBlockEntity extends PowerReceiverBlockEntity implements TemperatureMachine, PipeConnector, net.minecraft.world.Clearable {

	public static final int INPUTS = 18;
	public static final int CAPACITY = 16000;
	public static final int MAXTEMP = 1200;
	public static final int SMELT_TEMP = 400;

	private int temperature;
	private int tempTimer;
	public int smeltTick;

	private final FluidTank tank = new FluidTank(CAPACITY, fs -> fs.is(Fluids.LAVA));

	private final ItemStackHandler inv = new ItemStackHandler(2*INPUTS) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return slot < INPUTS;
		}

		@Override
		protected void onContentsChanged(int slot) {
			BigFurnaceBlockEntity.this.setChanged();
		}
	};

	public BigFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.BIG_FURNACE.get(), pos, state, 1, 1, 2048);
	}

	@Override
	public String getName() {
		return "Lava Smeltery";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public FluidTank getTank() {
		return tank;
	}

	public void addLava(int amt) {
		tank.fill(new FluidStack(Fluids.LAVA, amt), IFluidHandler.FluidAction.EXECUTE);
	}

	public int getOperationTime() {
		int base = temperature >= 600 ? 150 : 200;
		return temperature >= 1000 ? base/2 : base;
	}

	public int getCookScaled(int i) {
		return smeltTick*i/this.getOperationTime();
	}

	public int getLavaScaled(int i) {
		return tank.getFluidAmount()*i/CAPACITY;
	}

	private ItemStack getSmeltingResult(ItemStack is) {
		if (level == null || is.isEmpty())
			return ItemStack.EMPTY;
		SingleRecipeInput in = new SingleRecipeInput(is);
		return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, in, level).map(r -> r.value().assemble(in, level.registryAccess())).orElse(ItemStack.EMPTY);
	}

	public boolean canSmelt() {
		if (temperature < SMELT_TEMP || power < MINPOWER)
			return false;
		for (int i = 0; i < INPUTS; i++)
			if (!this.getSmeltingResult(inv.getStackInSlot(i)).isEmpty())
				return true;
		return false;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		read = Direction.DOWN;
		this.readPower();
		tempTimer++;
		if (tempTimer >= 20) {
			tempTimer = 0;
			this.updateTemperature(world, pos);
		}
		if (this.canSmelt()) {
			smeltTick++;
			if (smeltTick >= this.getOperationTime()) {
				smeltTick = 0;
				this.smelt();
			}
		}
		else
			smeltTick = 0;
	}

	private void smelt() {
		for (int i = 0; i < INPUTS; i++) {
			ItemStack to = this.getSmeltingResult(inv.getStackInSlot(i));
			if (to.isEmpty())
				continue;
			ItemStack out = inv.getStackInSlot(i+INPUTS);
			if (out.isEmpty())
				inv.setStackInSlot(i+INPUTS, to.copy());
			else if (ItemStack.isSameItemSameComponents(out, to) && out.getCount()+to.getCount() <= out.getMaxStackSize())
				inv.setStackInSlot(i+INPUTS, out.copyWithCount(out.getCount()+to.getCount()));
			else
				continue;
			inv.extractItem(i, 1, false);
		}
		this.syncAllData();
	}

	private void updateTemperature(Level world, BlockPos pos) {
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		if (!tank.isEmpty()) {
			tank.drain(15, IFluidHandler.FluidAction.EXECUTE);
			Tamb += 600;
		}
		if (temperature > Tamb)
			temperature--;
		if (temperature > Tamb*2)
			temperature--;
		if (temperature < Tamb)
			temperature++;
		if (temperature*2 < Tamb)
			temperature++;
		if (temperature > MAXTEMP)
			temperature = MAXTEMP;
		if (temperature > 100) {
			this.replaceAdjacent(world, pos, Blocks.SNOW_BLOCK.defaultBlockState(), Blocks.AIR.defaultBlockState());
			this.replaceAdjacent(world, pos, Blocks.ICE.defaultBlockState(), Blocks.WATER.defaultBlockState());
		}
		this.syncAllData();
	}

	/** Original ReikaWorldHelper.checkForAdjBlock + changeAdjBlock: the first side (in direction order) with that block. */
	private static void replaceAdjacent(Level world, BlockPos pos, BlockState find, BlockState to) {
		for (Direction d : Direction.values()) {
			if (world.getBlockState(pos.relative(d)).is(find.getBlock())) {
				world.setBlockAndUpdate(pos.relative(d), to);
				return;
			}
		}
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public int getRedstoneOverride() {
		return 0;
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
		temperature = temp;
	}

	@Override
	public int getMaxTemperature() {
		return MAXTEMP;
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return side.getAxis() != Direction.Axis.Y && this.canConnectToPipe(p);
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side.getAxis() != Direction.Axis.Y ? Flow.INPUT : Flow.NONE;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (from != null && from.getAxis() == Direction.Axis.Y)
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
				return stack.is(Fluids.LAVA);
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return BigFurnaceBlockEntity.this.fill(side, resource, action);
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

	/** Items into the 18 inputs; out only from the 18 outputs (original canExtractItem). */
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
				return slot >= INPUTS ? inv.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
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
		NBT.putInt("temp", temperature);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		temperature = NBT.getInt("temp");
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
