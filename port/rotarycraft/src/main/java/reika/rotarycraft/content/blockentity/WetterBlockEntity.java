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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryFluids;

/**
 * Original: Reika.RotaryCraft.TileEntities.Processing.TileEntityWetter - soaks the one item in its slot in the liquid of its 1000 mB
 * tank (RecipesWetter: sand + 500 mB lubricant -> soul sand in 200 ticks, cobblestone + 20 mB jet fuel -> netherrack in 80 ticks). Needs
 * 4096 W and 1024 rad/s from below; the timer advances by 1 + 4*log2(omega/1024) per tick up to max(1, duration - 5*(omega/1024 - 1)).
 * Liquids come in from the four sides only, and only one that fits the item in the slot (none while it works); the item can be taken
 * out by automation only 10 ticks after the last work. Not ported: other mods' liquids (oil, ender).
 */
public class WetterBlockEntity extends PowerReceiverBlockEntity implements PipeConnector, net.minecraft.world.Clearable {

	public static final int CAPACITY = 1000;

	/** Original WettingRecipe. */
	public record WettingRecipe(net.minecraft.world.item.Item input, Fluid fluid, int amount, net.minecraft.world.item.Item output, int duration) {}

	public static final WettingRecipe[] RECIPES = {
			new WettingRecipe(Blocks.SAND.asItem(), RotaryFluids.LUBRICANT.get(), 500, Blocks.SOUL_SAND.asItem(), 200),
			new WettingRecipe(Blocks.COBBLESTONE.asItem(), RotaryFluids.JET_FUEL.get(), 20, Blocks.NETHERRACK.asItem(), 80),
	};

	private int tick;
	private int extractionCooldown;

	private final FluidTank tank = new FluidTank(CAPACITY, fs -> this.isValidFluid(fs.getFluid()));

	private final ItemStackHandler inv = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return tank.isEmpty() ? isWettable(is) : isWettableWith(is, tank.getFluid().getFluid());
		}

		@Override
		public int getSlotLimit(int slot) {
			return 1;
		}

		@Override
		protected void onContentsChanged(int slot) {
			WetterBlockEntity.this.setChanged();
			WetterBlockEntity.this.syncAllData();
		}
	};

	public WetterBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.WETTER.get(), pos, state, 1, 1024, 4096);
	}

	@Override
	public String getName() {
		return "Wetter";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public FluidTank getTank() {
		return tank;
	}

	public int getTick() {
		return tick;
	}

	@Nullable
	private static WettingRecipe find(ItemStack is, Fluid f) {
		for (WettingRecipe r : RECIPES)
			if (is.is(r.input()) && (f == null || f.isSame(r.fluid())))
				return r;
		return null;
	}

	public static boolean isWettable(ItemStack is) {
		return find(is, null) != null;
	}

	public static boolean isWettableWith(ItemStack is, Fluid f) {
		return find(is, f) != null;
	}

	/** Original RecipesWetter.getRecipe: the recipe for the item and liquid, if the tank holds enough. */
	@Nullable
	public static WettingRecipe getRecipe(ItemStack is, FluidStack fs) {
		if (is.isEmpty() || fs.isEmpty())
			return null;
		WettingRecipe r = find(is, fs.getFluid());
		return r != null && fs.getAmount() >= r.amount() ? r : null;
	}

	private boolean isValidFluid(Fluid f) {
		if (tick > 0)
			return false;
		ItemStack is = inv.getStackInSlot(0);
		if (!is.isEmpty())
			return isWettableWith(is, f);
		for (WettingRecipe r : RECIPES)
			if (f.isSame(r.fluid()))
				return true;
		return false;
	}

	public int getDuration(WettingRecipe wr) {
		return Math.max(1, wr.duration()-5*(omega/MINSPEED-1));
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		read = Direction.DOWN;
		this.readPower();
		boolean ticked = false;
		if (power >= MINPOWER && omega >= MINSPEED) {
			WettingRecipe wr = getRecipe(inv.getStackInSlot(0), tank.getFluid());
			if (wr != null) {
				ticked = true;
				if (tick >= this.getDuration(wr)) {
					tank.drain(wr.amount(), IFluidHandler.FluidAction.EXECUTE);
					inv.setStackInSlot(0, new ItemStack(wr.output()));
					tick = 0;
				}
				else {
					tick += 1+4*ReikaMathLibrary.logbase2(omega/MINSPEED);
				}
			}
		}
		if (ticked)
			extractionCooldown = 10;
		else {
			if (extractionCooldown > 0)
				extractionCooldown--;
			tick = 0;
		}
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (level == null)
			phi = 0;
		else if (power >= MINPOWER && torque >= MINTORQUE)
			phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p == PipeType.HOSE || p == PipeType.FUEL_LINE || p.isStandardPipe();
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
		if (amt > 0 && action.execute()) {
			this.setChanged();
			this.syncAllData();
		}
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
				return WetterBlockEntity.this.isValidFluid(stack.getFluid());
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return WetterBlockEntity.this.fill(side, resource, action);
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

	/** Items in from any side; out only while it is not working and 10 ticks after (original canExtractItem). */
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
				return tick == 0 && extractionCooldown == 0 ? inv.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
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
		NBT.putInt("tick", tick);
		NBT.putInt("extract", extractionCooldown);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		tick = NBT.getInt("tick");
		extractionCooldown = NBT.getInt("extract");
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
