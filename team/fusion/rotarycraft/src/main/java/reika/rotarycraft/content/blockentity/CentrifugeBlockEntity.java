/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.content.recipe.CentrifugeRecipe;
import reika.rotarycraft.lib.data.InventoryHelper;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryRecipeTypes;

/**
 * Original: Reika.RotaryCraft.TileEntities.Processing.TileEntityCentrifuge. Powered from below (above when flipped); needs 4096 rad/s
 * and 16384 W; time max(1, 1200 - 60*log2(omega+1)). Each operation processes up to {@code max_stack} input items, as long as every
 * possible output would fit, and rolls the outputs ({@code rotarycraft:centrifuge} recipes) and the fluid into a 10000 mB tank,
 * which flows out to liquid pipes and hoses on the four sides. Slots: 0 input, 1-9 outputs.
 * Not ported: machine enchantments (fortune raises the chances, efficiency the fluid amount).
 */
public class CentrifugeBlockEntity extends PowerReceiverBlockEntity implements PipeConnector, net.minecraft.world.Clearable {

	public static final int CAPACITY = 10000;
	/** Original DurationRegistry.CENTRIFUGE. */
	private static final int DURATION_BASE = 1200, DURATION_SCALE = 60;

	private int progressTime;

	private final FluidTank tank = new FluidTank(CAPACITY);

	private final ItemStackHandler inv = new ItemStackHandler(10) {
		@Override
		protected void onContentsChanged(int slot) {
			CentrifugeBlockEntity.this.setChanged();
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return CentrifugeBlockEntity.this.isItemValidForSlot(slot, stack);
		}
	};

	public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.CENTRIFUGE.get(), pos, state, 1, 4096, 16384);
	}

	@Override
	public String getName() {
		return "Centrifuge";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	private Optional<RecipeHolder<CentrifugeRecipe>> getRecipe(ItemStack is) {
		if (level == null || is.isEmpty())
			return Optional.empty();
		return level.getRecipeManager().getRecipeFor(RotaryRecipeTypes.CENTRIFUGE.get(), new SingleRecipeInput(is), level);
	}

	public boolean isCentrifugable(ItemStack is) {
		return this.getRecipe(is).isPresent();
	}

	public int getProgressScaled(int l) {
		return l * progressTime / this.getOperationTime();
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		phi += ReikaMathLibrary.logbase2(omega); //integer log, 0 at standstill
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		read = isFlipped ? Direction.UP : Direction.DOWN; //original getPowerAbove / getPowerBelow
		if (world.isClientSide)
			return;
		this.readPower();
		if (power >= MINPOWER && omega >= MINSPEED) {
			int n = this.getNumberConsecutiveOperations();
			for (int i = 0; i < n; i++)
				this.doOperation(n > 1);
		}
		else {
			progressTime = 0;
		}
	}

	private void doOperation(boolean multiple) {
		ItemStack in = inv.getStackInSlot(0);
		Optional<RecipeHolder<CentrifugeRecipe>> rec = this.getRecipe(in);
		if (rec.isPresent()) {
			progressTime++;
			if (multiple || progressTime >= this.getOperationTime()) {
				CentrifugeRecipe out = rec.get().value();
				for (int i = 0; i < out.maxStack() && !inv.getStackInSlot(0).isEmpty(); i++) {
					if (!this.canMakeAllOf(out))
						break;
					FluidStack fs = out.fluid().map(FluidStack::copy).orElse(FluidStack.EMPTY);
					if (!fs.isEmpty() && tank.fill(fs, IFluidHandler.FluidAction.SIMULATE) < fs.getAmount())
						break;
					for (ItemStack is : out.rollItems(rand))
						InventoryHelper.addToRange(is, inv, 1, inv.getSlots());
					FluidStack rolled = out.rollFluid(rand);
					if (!rolled.isEmpty())
						tank.fill(rolled, IFluidHandler.FluidAction.EXECUTE);
					InventoryHelper.decrStack(inv, 0, 1);
				}
				progressTime = 0;
			}
		}
		else {
			progressTime = 0;
		}
	}

	/** Original canMakeAllOf: every possible output fits into the output slots at once. */
	private boolean canMakeAllOf(CentrifugeRecipe out) {
		ItemStackHandler temp = new ItemStackHandler(9);
		for (int i = 0; i < 9; i++)
			temp.setStackInSlot(i, inv.getStackInSlot(i+1).copy());
		for (CentrifugeRecipe.ChanceOutput is : out.outputs()) {
			if (!InventoryHelper.addToRange(is.item().copy(), temp, 0, 9))
				return false;
		}
		return true;
	}

	public int getFluidLevel() {
		return tank.getFluidAmount();
	}

	public FluidStack getFluid() {
		return tank.getFluid();
	}

	public int getLiquidScaled(int a) {
		return a * tank.getFluidAmount() / tank.getCapacity();
	}

	public int getProgress() {
		return progressTime;
	}

	public int getOperationTime() {
		return (int)Math.max(1, DURATION_BASE-DURATION_SCALE*ReikaMathLibrary.logbase(Math.max(0, omega)+1, 2));
	}

	public int getNumberConsecutiveOperations() {
		int threshold = (int)ReikaMathLibrary.intpow(2, 1+DURATION_BASE/DURATION_SCALE);
		return 1+(omega >= threshold ? (int)(ReikaMathLibrary.logbase(omega, 2)-DURATION_BASE/DURATION_SCALE) : 0);
	}

	public boolean isItemValidForSlot(int i, ItemStack is) {
		return i == 0 && this.isCentrifugable(is);
	}

	@Override
	public boolean canConnectToPipe(PipeType m) {
		return m.isStandardPipe() || m == PipeType.HOSE;
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p) && side.getAxis() != Direction.Axis.Y;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side.getAxis() != Direction.Axis.Y ? Flow.OUTPUT : Flow.NONE;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		return 0;
	}

	/** Original canDrain: not from above or below (an unknown side counts as a side, like ForgeDirection.UNKNOWN). */
	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (from != null && from.getAxis() == Direction.Axis.Y)
			return FluidStack.EMPTY;
		FluidStack fs = tank.drain(maxDrain, action);
		if (!fs.isEmpty() && action.execute())
			this.setChanged();
		return fs;
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
				return false;
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return 0;
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				return FluidStack.isSameFluidSameComponents(resource, tank.getFluid()) ? CentrifugeBlockEntity.this.drain(side, resource.getAmount(), action) : FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int maxDrain, FluidAction action) {
				return CentrifugeBlockEntity.this.drain(side, maxDrain, action);
			}
		};
	}

	/** Original ISidedInventory: insert into the input, take from every other slot. */
	public IItemHandler getAutomationHandler() {
		return new IItemHandler() {
			@Override
			public int getSlots() {
				return 10;
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
				return slot != 0 ? inv.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
			}

			@Override
			public int getSlotLimit(int slot) {
				return 64;
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return CentrifugeBlockEntity.this.isItemValidForSlot(slot, stack);
			}
		};
	}

	@Override
	public void clearContent() {
		for (int i = 0; i < inv.getSlots(); i++)
			inv.setStackInSlot(i, ItemStack.EMPTY);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("CookTime", progressTime);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		progressTime = NBT.getInt("CookTime");
		tank.readFromNBT(registries, NBT.getCompound("tank"));
	}

	@Override
	protected void writeAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeAdditional(NBT, registries);
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readAdditional(NBT, registries);
		inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < inv.getSlots(); i++)
			net.minecraft.world.Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}
}
