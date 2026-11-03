/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.math.RandomHelper;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Processing.TileEntityFuelConverter (Fuel Enhancer) - with shaft power from below (at least
 * 16384 rad/s and 16384 W) it converts its input fluid by a registered conversion each tick: speed (1 + log2(omega/16384)/2) times the
 * conversion's speed is made from ratio times as much input, if all the conversion's items are in its 9 slots; each item is used up
 * with the conversion's chance. Input from the top (standard pipes and fuel lines), output at the sides (fuel lines); 5000 mB tanks;
 * buckets and other containers can be emptied into it by right-click. Comparator: 15 while the input is empty or the output full.
 * The original's two conversions to jet fuel start from other mods' fuels (BuildCraft fuel, kerosene) and are therefore absent;
 * other mods (here ReactorCraft) add conversions with {@link #addRecipe}.
 */
public class FuelEnhancerBlockEntity extends PowerReceiverBlockEntity implements PipeConnector, net.minecraft.world.Clearable {

	public static final int CAPACITY = 5000;

	/** Original FuelConversion (registered by other mods through addRecipe). */
	public record FuelConversion(Supplier<Fluid> input, Supplier<Fluid> output, int speedFactor, int fluidRatio, double itemConsumptionChance, List<Ingredient> ingredients) {
		public boolean isValidItem(ItemStack is) {
			for (Ingredient i : ingredients)
				if (i.test(is))
					return true;
			return false;
		}
	}

	private static final List<FuelConversion> CONVERSIONS = new ArrayList<>();

	public static FuelConversion addRecipe(Supplier<Fluid> in, Supplier<Fluid> out, int speed, int fluidRatio, double itemConsumeChance, Ingredient... items) {
		FuelConversion c = new FuelConversion(in, out, speed, fluidRatio, itemConsumeChance, List.of(items));
		CONVERSIONS.add(c);
		return c;
	}

	public static List<FuelConversion> getAllRecipes() {
		return Collections.unmodifiableList(CONVERSIONS);
	}

	private final FluidTank input = new FluidTank(CAPACITY, fs -> isConvertible(fs.getFluid()));
	private final FluidTank output = new FluidTank(CAPACITY);

	private final ItemStackHandler inv = new ItemStackHandler(9) {
		@Override
		protected void onContentsChanged(int slot) {
			FuelEnhancerBlockEntity.this.setChanged();
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			for (FuelConversion c : CONVERSIONS)
				if (c.isValidItem(stack))
					return true;
			return false;
		}
	};

	public FuelEnhancerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.FUEL_ENHANCER.get(), pos, state, 1, 16384, 16384);
	}

	@Override
	public String getName() {
		return "Fuel Enhancer";
	}

	public static boolean isConvertible(Fluid f) {
		for (FuelConversion c : CONVERSIONS)
			if (f.isSame(c.input().get()))
				return true;
		return false;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		read = Direction.DOWN; //original getPowerBelow
		if (world.isClientSide)
			return;
		this.readPower();
		power = (long)omega*(long)torque;
		if (power < MINPOWER || omega < MINSPEED)
			return;
		if (input.isEmpty())
			return;
		int boost = 1+ReikaMathLibrary.logbase2(omega/MINSPEED)/2;
		for (FuelConversion fc : CONVERSIONS) {
			if (!input.getFluid().getFluid().isSame(fc.input().get()))
				continue;
			int spd = boost*fc.speedFactor();
			if (input.getFluidAmount() >= fc.fluidRatio()*spd && output.getSpace() >= spd && (output.isEmpty() || output.getFluid().getFluid().isSame(fc.output().get()))
					&& this.hasItems(fc)) {
				input.drain(fc.fluidRatio()*spd, IFluidHandler.FluidAction.EXECUTE);
				output.fill(new FluidStack(fc.output().get(), spd), IFluidHandler.FluidAction.EXECUTE);
				this.consumeItems(fc);
				this.setChanged();
				break;
			}
		}
	}

	private boolean hasItems(FuelConversion c) {
		for (Ingredient ing : c.ingredients()) {
			boolean found = false;
			for (int i = 0; i < inv.getSlots() && !found; i++)
				found = ing.test(inv.getStackInSlot(i));
			if (!found)
				return false;
		}
		return true;
	}

	private void consumeItems(FuelConversion c) {
		for (Ingredient ing : c.ingredients()) {
			if (RandomHelper.doWithChance(c.itemConsumptionChance(), rand)) {
				for (int i = 0; i < inv.getSlots(); i++) {
					if (ing.test(inv.getStackInSlot(i))) {
						inv.extractItem(i, 1, false);
						break;
					}
				}
			}
		}
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (world == null) {
			phi = 0;
			return;
		}
		phi += (float)Math.pow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public int getRedstoneOverride() {
		return input.isEmpty() || output.getFluidAmount() >= CAPACITY ? 15 : 0;
	}

	/** Original getLiquidModelOffset. */
	public double getLiquidModelOffset(boolean in) {
		return in ? 10/16D : 1/16D;
	}

	public FluidTank getInputTank() {
		return input;
	}

	public FluidTank getOutputTank() {
		return output;
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	/** Items go in from every side and never come out (original canExtractItem false). */
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
				return ItemStack.EMPTY;
			}

			@Override
			public int getSlotLimit(int slot) {
				return inv.getSlotLimit(slot);
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return inv.isItemValid(slot, stack);
			}
		};
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p == PipeType.FUEL_LINE || p.isStandardPipe();
	}

	/** Original canIntakeFromPipe (top: standard pipes and fuel lines) and canOutputToPipe (sides: fuel lines). */
	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		if (side == Direction.UP)
			return p.isStandardPipe() || p == PipeType.FUEL_LINE;
		return side != Direction.DOWN && p == PipeType.FUEL_LINE;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		if (side == Direction.UP)
			return Flow.INPUT;
		return side == Direction.DOWN ? Flow.NONE : Flow.OUTPUT;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (from != Direction.UP && from != null)
			return 0;
		int n = input.fill(resource, action);
		if (n > 0 && action.execute())
			this.setChanged();
		return n;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (from == null || from.getAxis() == Direction.Axis.Y)
			return FluidStack.EMPTY;
		FluidStack d = output.drain(maxDrain, action);
		if (!d.isEmpty() && action.execute())
			this.setChanged();
		return d;
	}

	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 2;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return (t == 0 ? input : output).getFluid();
			}

			@Override
			public int getTankCapacity(int t) {
				return CAPACITY;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return t == 0 && input.isFluidValid(stack);
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return FuelEnhancerBlockEntity.this.fill(side, resource, action);
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				if (resource.isEmpty() || !output.getFluid().getFluid().isSame(resource.getFluid()))
					return FluidStack.EMPTY;
				return FuelEnhancerBlockEntity.this.drain(side, resource.getAmount(), action);
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				return FuelEnhancerBlockEntity.this.drain(side, max, action);
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
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("in", input.writeToNBT(registries, new CompoundTag()));
		NBT.put("out", output.writeToNBT(registries, new CompoundTag()));
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		input.setFluid(FluidStack.EMPTY);
		input.readFromNBT(registries, NBT.getCompound("in"));
		output.setFluid(FluidStack.EMPTY);
		output.readFromNBT(registries, NBT.getCompound("out"));
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
