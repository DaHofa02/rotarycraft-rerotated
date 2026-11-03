/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.TileEntities.Processing.TileEntityDryingBed with RecipesDryingBed - a 2000 mB basin, no power. While it
 * holds enough of a dryable liquid and the output slot can take more, its timer runs; every 400 ticks it dries as many portions as fit:
 * water 250 mB -> salt, lava 1000 mB -> gold nugget, oil 200 mB -> tar, honey 400 mB -> slime ball (oil and honey from other mods, here
 * through the c:oil and c:honey fluid tags; the ChromatiCraft recipe is not ported). Liquid comes in from every side except the bottom
 * (pipes, hoses and fuel lines; as in the original the pipe connection test looks at the opposite side, so pipes join from below but
 * not from above) or by hand with a bucket; the product is taken out of its slot from any side.
 */
public class DryingBedBlockEntity extends RotaryCraftBlockEntity implements PipeConnector, net.minecraft.world.Clearable {

	public static final int CAPACITY = 2000;
	public static final int TIMER_CAP = 400;

	private static final TagKey<Fluid> OIL = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "oil"));
	private static final TagKey<Fluid> HONEY = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "honey"));

	/** Original RecipesDryingBed. */
	public enum Drying {
		WATER(f -> f.isSame(Fluids.WATER), 250, () -> new ItemStack(RotaryItems.part(CraftingPart.SALT))),
		LAVA(f -> f.isSame(Fluids.LAVA), 1000, () -> new ItemStack(Items.GOLD_NUGGET)),
		OIL(f -> f.is(DryingBedBlockEntity.OIL), 200, () -> new ItemStack(RotaryItems.part(CraftingPart.TAR))),
		HONEY(f -> f.is(DryingBedBlockEntity.HONEY), 400, () -> new ItemStack(Items.SLIME_BALL));

		private final Predicate<Fluid> fluid;
		public final int amount;
		private final Supplier<ItemStack> output;

		Drying(Predicate<Fluid> f, int amt, Supplier<ItemStack> out) {
			fluid = f;
			amount = amt;
			output = out;
		}

		public ItemStack getOutput() {
			return output.get();
		}

		@Nullable
		public static Drying forFluid(Fluid f) {
			for (Drying d : values())
				if (d.fluid.test(f))
					return d;
			return null;
		}
	}

	private final FluidTank tank = new FluidTank(CAPACITY, fs -> Drying.forFluid(fs.getFluid()) != null) {
		@Override
		protected void onContentsChanged() {
			DryingBedBlockEntity.this.setChanged();
		}
	};

	private final ItemStackHandler inv = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return false;
		}

		@Override
		protected void onContentsChanged(int slot) {
			DryingBedBlockEntity.this.setChanged();
		}
	};

	private int timer;
	public int progress;

	public DryingBedBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.DRYING_BED.get(), pos, state);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	/** Original RecipesDryingBed.getDryingResult: the recipe of the tank's liquid if there is enough of it. */
	@Nullable
	private Drying getResult() {
		if (tank.isEmpty())
			return null;
		Drying d = Drying.forFluid(tank.getFluid().getFluid());
		return d != null && d.amount <= tank.getFluidAmount() ? d : null;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (world.isClientSide)
			return;
		Drying d = this.getResult();
		if (this.canMake(d)) {
			timer++;
			if (timer >= TIMER_CAP) {
				timer = 0;
				while (this.canMake(d)) {
					this.make(d);
					d = this.getResult();
				}
				this.syncAllData();
			}
		}
		else {
			timer = 0;
		}
		if (progress != timer) {
			progress = timer;
			if (timer%20 == 0)
				this.syncAllData();
		}
	}

	private boolean canMake(@Nullable Drying d) {
		if (d == null)
			return false;
		ItemStack out = d.getOutput();
		ItemStack has = inv.getStackInSlot(0);
		return has.isEmpty() || (ItemStack.isSameItemSameComponents(has, out) && has.getCount()+out.getCount() <= has.getMaxStackSize());
	}

	private void make(Drying d) {
		ItemStack out = d.getOutput();
		ItemStack has = inv.getStackInSlot(0);
		if (has.isEmpty())
			inv.setStackInSlot(0, out);
		else
			has.grow(out.getCount());
		tank.drain(d.amount, IFluidHandler.FluidAction.EXECUTE);
	}

	/** Original BlockBasicMultiTE.onBlockActivated: a filled container (bucket) is emptied in when it fits and matches. */
	public boolean addLiquid(FluidStack fs) {
		if (Drying.forFluid(fs.getFluid()) == null || tank.getFluidAmount()+fs.getAmount() > CAPACITY)
			return false;
		if (!tank.isEmpty() && !tank.getFluid().getFluid().isSame(fs.getFluid()))
			return false;
		tank.fill(fs, IFluidHandler.FluidAction.EXECUTE);
		this.syncAllData();
		return true;
	}

	public FluidTank getTank() {
		return tank;
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public int getLiquidScaled(int a) {
		return a*tank.getFluidAmount()/CAPACITY;
	}

	public int getProgressScaled(int a) {
		return a*progress/TIMER_CAP;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	private static boolean canReceiveFrom(@Nullable Direction from) {
		return from != Direction.DOWN;
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe() || p == PipeType.HOSE || p == PipeType.FUEL_LINE;
	}

	/** Original RCFluidReceiver.canConnectToPipeOnSide: canReceiveFrom(side.getOpposite()). */
	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return canReceiveFrom(side.getOpposite()) && this.canConnectToPipe(p);
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (!canReceiveFrom(from))
			return 0;
		return tank.fill(resource, action);
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		return FluidStack.EMPTY;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return canReceiveFrom(side) ? Flow.INPUT : Flow.NONE;
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
				return tank.isFluidValid(stack);
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return DryingBedBlockEntity.this.fill(side, resource, action);
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

	/** Nothing goes in, the product comes out from every side. */
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

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		net.minecraft.world.Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(0).copy());
	}

	@Override
	public void clearContent() {
		inv.setStackInSlot(0, ItemStack.EMPTY);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.put("inv", inv.serializeNBT(registries));
		NBT.putInt("progress", timer);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
		timer = progress = NBT.getInt("progress");
	}
}
