/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.blockentity;

import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;
import reika.reactorcraft.registry.ReactorItems;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;

/**
 * Original: Reika.ReactorCraft.TileEntities.Processing.TileEntityUProcessor (Uranium Processor) - no shaft power. Two parallel steps:
 * <ul><li>intermediate (every intermediateTime ticks while the input tank holds something and a fluorite crystal is in slot 0): uses the
 * fluorite and inputFluidConsumed of the input fluid, makes intermediateFluidProduced hydrofluoric acid;</li>
 * <li>output (every outputTime ticks while slot 2 holds the process item and there is enough acid): uses the item and
 * intermediateFluidConsumed acid, makes outputFluidProduced of the product.</li></ul>
 * UF6: water + uranium ingot -> uranium hexafluoride; LiFBe: molten lithium + emerald dust -> lithium beryllium fluoride. Slot 1 empties
 * fluid containers into the input tank. Three 3000 mB tanks; pipes fill the input from every side but the front and take the product
 * from the front; items can be inserted from any side but not taken out.
 */
public class UProcessorBlockEntity extends RotaryCraftBlockEntity implements PipeConnector, net.minecraft.world.Clearable {

	public static final int CAPACITY = 3000;

	public enum Process {
		UF6(() -> Fluids.WATER, () -> ReactorFluids.get("hydrofluoric_acid"), () -> ReactorFluids.get("uranium_hexafluoride"), 250, 1000, 250, 125, 80, 400,
				is -> is.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots/uranium")))),
		LIFBE(() -> ReactorFluids.get("lithium"), () -> ReactorFluids.get("hydrofluoric_acid"), () -> ReactorFluids.get("lifbe"), 100, 500, 250, 1500, 120, 600,
				is -> is.is(ReactorItems.raw(ReactorItems.Raw.EMERALDDUST).get()));

		private final Supplier<Fluid> input, intermediate, output;
		public final int inputFluidConsumed, outputFluidProduced, intermediateFluidProduced, intermediateFluidConsumed;
		public final int intermediateTime, outputTime;
		private final Predicate<ItemStack> item;

		Process(Supplier<Fluid> in, Supplier<Fluid> mid, Supplier<Fluid> out, int incons, int outprod, int prod, int cons, int t1, int t2, Predicate<ItemStack> item) {
			input = in;
			intermediate = mid;
			output = out;
			inputFluidConsumed = incons;
			outputFluidProduced = outprod;
			intermediateFluidProduced = prod;
			intermediateFluidConsumed = cons;
			intermediateTime = t1;
			outputTime = t2;
			this.item = item;
		}

		public Fluid inputFluid() {
			return input.get();
		}

		public Fluid intermediateFluid() {
			return intermediate.get();
		}

		public Fluid outputFluid() {
			return output.get();
		}

		public boolean isValidItem(ItemStack is) {
			return !is.isEmpty() && item.test(is);
		}

		@Nullable
		public static Process byInput(Fluid f) {
			for (Process p : values())
				if (p.inputFluid().isSame(f))
					return p;
			return null;
		}

		@Nullable
		public static Process byItem(ItemStack is) {
			for (Process p : values())
				if (p.isValidItem(is))
					return p;
			return null;
		}
	}

	private final FluidTank input = new FluidTank(CAPACITY);
	private final FluidTank intermediate = new FluidTank(CAPACITY);
	private final FluidTank output = new FluidTank(CAPACITY);

	public int intermediateTimer;
	public int outputTimer;

	private final ItemStackHandler inv = new ItemStackHandler(3) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return switch (slot) {
				case 0 -> isFluorite(is);
				case 1 -> {
					FluidStack fs = FluidUtil.getFluidContained(is).orElse(FluidStack.EMPTY);
					yield !fs.isEmpty() && Process.byInput(fs.getFluid()) != null;
				}
				case 2 -> Process.byItem(is) != null;
				default -> false;
			};
		}

		@Override
		protected void onContentsChanged(int slot) {
			UProcessorBlockEntity.this.setChanged();
		}
	};

	public UProcessorBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.PROCESSOR.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Uranium Processor";
	}

	/** Original isFluorite: any fluorite crystal or anything in gemFluorite. */
	public static boolean isFluorite(ItemStack is) {
		return is.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "gems/fluorite")));
	}

	/** Original getFacing(meta): 0 west, 1 east, 2 north, 3 south. */
	public Direction getFacing() {
		return switch (this.getBlockMetadata()) {
			case 1 -> Direction.EAST;
			case 2 -> Direction.NORTH;
			case 3 -> Direction.SOUTH;
			default -> Direction.WEST;
		};
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (world.isClientSide)
			return;
		this.getFluidContainers();
		Process p = this.getProcess();
		if (p == null)
			return;
		if (this.canRunIntermediate(p)) {
			intermediateTimer++;
			if (intermediateTimer >= p.intermediateTime) {
				intermediateTimer = 0;
				this.runIntermediate(p);
			}
		}
		else {
			intermediateTimer = 0;
		}
		if (this.canRunOutput(p)) {
			outputTimer++;
			if (outputTimer >= p.outputTime) {
				outputTimer = 0;
				this.runOutput(p);
			}
		}
		else {
			outputTimer = 0;
		}
	}

	@Nullable
	public Process getProcess() {
		return input.isEmpty() ? null : Process.byInput(input.getFluid().getFluid());
	}

	private boolean canRunOutput(Process p) {
		return p.isValidItem(inv.getStackInSlot(2)) && intermediate.getFluidAmount() >= p.intermediateFluidConsumed
				&& (output.isEmpty() || output.getFluid().getFluid().isSame(p.outputFluid())) && output.getFluidAmount()+p.outputFluidProduced <= CAPACITY;
	}

	private boolean canRunIntermediate(Process p) {
		return input.getFluidAmount() > 0 && intermediate.getFluidAmount()+p.intermediateFluidProduced <= CAPACITY && isFluorite(inv.getStackInSlot(0));
	}

	private void runIntermediate(Process p) {
		inv.extractItem(0, 1, false);
		intermediate.fill(new FluidStack(p.intermediateFluid(), p.intermediateFluidProduced), IFluidHandler.FluidAction.EXECUTE);
		input.drain(p.inputFluidConsumed, IFluidHandler.FluidAction.EXECUTE);
		this.setChanged();
	}

	private void runOutput(Process p) {
		inv.extractItem(2, 1, false);
		output.fill(new FluidStack(p.outputFluid(), p.outputFluidProduced), IFluidHandler.FluidAction.EXECUTE);
		intermediate.drain(p.intermediateFluidConsumed, IFluidHandler.FluidAction.EXECUTE);
		this.setChanged();
	}

	/** Original getFluidContainers: a full container in slot 1 is emptied into the input tank if a process takes its fluid. */
	private void getFluidContainers() {
		ItemStack is = inv.getStackInSlot(1);
		if (is.isEmpty())
			return;
		FluidStack fs = FluidUtil.getFluidContained(is).orElse(FluidStack.EMPTY);
		if (fs.isEmpty() || Process.byInput(fs.getFluid()) == null)
			return;
		if (input.fill(fs, IFluidHandler.FluidAction.SIMULATE) != fs.getAmount())
			return;
		var r = FluidUtil.tryEmptyContainer(is, input, fs.getAmount(), null, true);
		if (r.isSuccess()) {
			inv.setStackInSlot(1, r.getResult());
			this.setChanged();
		}
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

	public ItemStackHandler getInventory() {
		return inv;
	}

	public FluidTank getInputTank() {
		return input;
	}

	public FluidTank getIntermediateTank() {
		return intermediate;
	}

	public FluidTank getOutputTank() {
		return output;
	}

	public int getIntermediateTimerScaled(int s) {
		Process p = this.getProcess();
		return p == null ? 0 : s*intermediateTimer/p.intermediateTime;
	}

	public int getOutputTimerScaled(int s) {
		Process p = this.getProcess();
		return p == null ? 0 : s*outputTimer/p.outputTime;
	}

	/** Original canFill: the input fluids and the intermediate (which goes into the input tank, as in the original); drain: products. */
	private static boolean canFill(Fluid f) {
		for (Process p : Process.values())
			if (p.inputFluid().isSame(f) || p.intermediateFluid().isSame(f))
				return true;
		return false;
	}

	private static boolean canDrain(Fluid f) {
		for (Process p : Process.values())
			if (p.outputFluid().isSame(f))
				return true;
		return false;
	}

	/** Items in from every side, never out (original canRemoveItem false). */
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

	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 3;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return (t == 0 ? input : t == 1 ? intermediate : output).getFluid();
			}

			@Override
			public int getTankCapacity(int t) {
				return CAPACITY;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return t == 0 && canFill(stack.getFluid());
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return UProcessorBlockEntity.this.fill(side, resource, action);
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				if (resource.isEmpty() || !canDrain(resource.getFluid()) || !output.getFluid().getFluid().isSame(resource.getFluid()))
					return FluidStack.EMPTY;
				return UProcessorBlockEntity.this.drain(side, resource.getAmount(), action);
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				return UProcessorBlockEntity.this.drain(side, max, action);
			}
		};
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (!canFill(resource.getFluid()))
			return 0;
		int n = input.fill(resource, action);
		if (n > 0 && action.execute())
			this.setChanged();
		return n;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		FluidStack d = output.drain(maxDrain, action);
		if (!d.isEmpty() && action.execute())
			this.setChanged();
		return d;
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
		return side == this.getFacing() ? Flow.OUTPUT : Flow.INPUT;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("uf6", outputTimer);
		NBT.putInt("hf", intermediateTimer);
		NBT.put("in", input.writeToNBT(registries, new CompoundTag()));
		NBT.put("mid", intermediate.writeToNBT(registries, new CompoundTag()));
		NBT.put("out", output.writeToNBT(registries, new CompoundTag()));
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		outputTimer = NBT.getInt("uf6");
		intermediateTimer = NBT.getInt("hf");
		input.readFromNBT(registries, NBT.getCompound("in"));
		intermediate.readFromNBT(registries, NBT.getCompound("mid"));
		output.readFromNBT(registries, NBT.getCompound("out"));
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
