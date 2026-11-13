/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.MyceliumBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.lib.world.CropHelper;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.TileEntities.Farming.TileEntityFertilizer - with water in its 6000 mB tank and bone meal or compost in
 * its 18 slots it gives random blocks within 2*log2(torque) blocks (at most 32; not above one block over itself) a forced random tick,
 * 4*log2(omega) blocks a tick (at least 1024 W from below), several times each from 2^20 rad/s. Crops, saplings, cacti, sugar cane,
 * stems, vines, mushrooms, nether wart and mycelium count as fertilized: then with a chance of 1/5 it uses 5 mB of water and, with a
 * chance of 1/4, one fertilizer item. Redstone parts are never ticked. Not ported: machine enchantments, Forestry fertilizer, other
 * mods' crops and saplings.
 */
public class FertilizerBlockEntity extends PowerReceiverBlockEntity implements PipeConnector, net.minecraft.world.Clearable {

	public static final int CAPACITY = 6000;
	public static final int MAXRANGE = 32;

	private int firstValidSlot = -1;
	private final FluidTank tank = new FluidTank(CAPACITY, fs -> fs.is(Fluids.WATER));

	private final ItemStackHandler inv = new ItemStackHandler(18) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return isValidFertilizer(is);
		}

		@Override
		protected void onContentsChanged(int slot) {
			FertilizerBlockEntity.this.setChanged();
		}
	};

	public FertilizerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.FERTILIZER.get(), pos, state, 1, 1, 1024);
	}

	@Override
	public String getName() {
		return "Fertilizer";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public FluidTank getTank() {
		return tank;
	}

	public static boolean isValidFertilizer(ItemStack is) {
		return is.is(Items.BONE_MEAL) || is.is(RotaryItems.part(CraftingPart.COMPOST));
	}

	public boolean hasFertilizer() {
		return firstValidSlot >= 0;
	}

	private void checkFertilizer() {
		firstValidSlot = -1;
		if (tank.isEmpty())
			return;
		for (int i = 0; i < inv.getSlots(); i++) {
			if (isValidFertilizer(inv.getStackInSlot(i))) {
				firstValidSlot = i;
				return;
			}
		}
	}

	public int getUpdatesPerTick() {
		return power < MINPOWER ? 0 : 4*ReikaMathLibrary.logbase2(omega);
	}

	public int getConsecutiveUpdates() {
		return omega < 1048576 ? 1 : 1+ReikaMathLibrary.logbase2(omega/1048576);
	}

	public int getRange() {
		if (torque <= 0)
			return 0;
		return Math.min(MAXRANGE, 2*(int)ReikaMathLibrary.logbase(torque, 2));
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (!(world instanceof ServerLevel sl))
			return;
		read = Direction.DOWN;
		this.readPower();
		this.checkFertilizer();
		int n = this.getUpdatesPerTick();
		for (int i = 0; i < n && this.hasFertilizer(); i++)
			this.tickBlock(sl, pos);
	}

	private void tickBlock(ServerLevel world, BlockPos pos) {
		int r = this.getRange();
		int dx = pos.getX()-r+rand.nextInt(2*r+1);
		int dy = pos.getY()-r+rand.nextInt(2*r+1);
		while (dy > pos.getY()+1)
			dy = pos.getY()-r+rand.nextInt(2*r+1);
		int dz = pos.getZ()-r+rand.nextInt(2*r+1);
		BlockPos c = new BlockPos(dx, dy, dz);
		BlockState s = world.getBlockState(c);
		if (s.isAir() || Math.sqrt(c.distSqr(pos)) > r || !canTick(s))
			return;
		int n = this.getConsecutiveUpdates();
		for (int i = 0; i < n; i++) {
			s.randomTick(world, c, world.random);
			s = world.getBlockState(c);
		}
		if (didSomething(s) && rand.nextInt(5) == 0)
			this.consumeItem();
	}

	private static boolean canTick(BlockState s) {
		return !(s.getBlock() instanceof DiodeBlock) && !(s.getBlock() instanceof ComparatorBlock) && !(s.getBlock() instanceof RedStoneWireBlock);
	}

	/** Original didSomething: crops, saplings and the other plants that grow on a tick, and mycelium. */
	public static boolean didSomething(BlockState s) {
		return CropHelper.isCrop(s) || CropHelper.isGrowingPlant(s) || s.getBlock() instanceof MyceliumBlock;
	}

	private void consumeItem() {
		tank.drain(5, IFluidHandler.FluidAction.EXECUTE);
		if (rand.nextInt(4) == 0) {
			inv.extractItem(firstValidSlot, 1, false);
			this.checkFertilizer();
		}
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (level == null)
			phi = 0;
		else
			phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public int getRedstoneOverride() {
		return this.hasFertilizer() ? 0 : 15;
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return side != Direction.DOWN && this.canConnectToPipe(p);
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side != Direction.DOWN ? Flow.INPUT : Flow.NONE;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (from == Direction.DOWN)
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
				return stack.is(Fluids.WATER);
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return FertilizerBlockEntity.this.fill(side, resource, action);
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

	/** Fertilizer in from any side; nothing comes out (original canExtractItem: false). */
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
