/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.waste;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.radiation.Isotopes;
import reika.reactorcraft.reactor.ReactorBlockEntity;
import reika.reactorcraft.registry.ReactorComponents;
import reika.reactorcraft.registry.ReactorItems;
import reika.rotarycraft.lib.math.RandomHelper;

/**
 * Original: Reika.ReactorCraft.Base.TileEntityWasteUnit - holds nuclear waste and lets it decay: each tick every waste item may give
 * off radiation (chance from its half-life; leaking units then send a waste neutron out with 50 %) and halves (at least by one) with the
 * decay chance ln2/half-life times the unit's rate (base rate * 192). Units in a column pass their waste down (Feedable). Items enter
 * and leave from all sides. The ChromatiCraft acceleration upgrade does not exist here.
 */
public abstract class WasteUnitBlockEntity extends ReactorBlockEntity implements net.minecraft.world.Clearable {

	/** Original ReikaTimeHelper.YEAR.getMinecraftDuration (48 days of 24000 ticks). */
	public static final double MC_YEAR = 24000*8*6;

	protected final ItemStackHandler inv;
	private long lastTickTime = -1;

	protected WasteUnitBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
		super(type, pos, state);
		inv = new ItemStackHandler(size) {
			@Override
			public int getSlotLimit(int slot) {
				return WasteUnitBlockEntity.this.getInventoryStackLimit();
			}

			@Override
			public boolean isItemValid(int slot, ItemStack is) {
				return WasteUnitBlockEntity.this.isItemValidForSlot(slot, is);
			}

			@Override
			protected void onContentsChanged(int slot) {
				WasteUnitBlockEntity.this.setChanged();
			}
		};
	}

	public abstract boolean leaksRadiation();

	public abstract boolean isValidIsotope(Isotopes i);

	protected abstract double getBaseDecayRate();

	protected abstract boolean accountForOutGameTime();

	protected abstract boolean canRemoveItem(int slot, ItemStack is);

	public int getInventoryStackLimit() {
		return 1;
	}

	@Nullable
	public static Isotopes getIsotope(ItemStack is) {
		if (!is.is(ReactorItems.WASTE.get()))
			return null;
		Integer id = is.get(ReactorComponents.ISOTOPE.get());
		int i = id == null ? 0 : id;
		return i >= 0 && i < Isotopes.isoList.length ? Isotopes.isoList[i] : null;
	}

	public boolean isItemValidForSlot(int i, ItemStack is) {
		Isotopes atom = getIsotope(is);
		return atom != null && this.isValidIsotope(atom) && this.isValidSlot(i, is);
	}

	protected boolean isValidSlot(int i, ItemStack is) {
		return true;
	}

	private double getAccelerationFactor() {
		return this.getBaseDecayRate()*192;
	}

	protected final void decayWaste(Level world, BlockPos pos) {
		double mult = this.getAccelerationFactor();
		if (this.accountForOutGameTime())
			mult *= (1+this.getSkippedTicks());
		for (int i = 0; i < inv.getSlots(); i++) {
			ItemStack is = inv.getStackInSlot(i);
			Isotopes atom = getIsotope(is);
			if (atom != null) {
				if (RandomHelper.doWithChance(mult/this.getBaseDecayRate()*0.5*decayChance(Math.log(atom.getMCHalfLife())), rand)) {
					if (this.leaksRadiation() && rand.nextBoolean())
						this.leakRadiation(world, pos);
				}
				if (RandomHelper.doWithChance(mult*decayChance(atom.getMCHalfLife()), rand)) {
					inv.extractItem(i, Math.max(1, is.getCount()/2), false);
					this.onDecayWaste(i);
				}
			}
		}
	}

	/** Original ReikaNuclearHelper.getDecayChanceFromHalflife. */
	public static double decayChance(double halflife) {
		return Math.log(2)/halflife;
	}

	/** Compensates for lag and lets storage decay continue while the game is closed (original getSkippedTicks, real time); nothing is skipped before the first tick. */
	private long getSkippedTicks() {
		long time = System.currentTimeMillis();
		long dur = time-lastTickTime;
		long ticks = 0;
		if (lastTickTime >= 0 && dur > 50)
			ticks = (dur/50)-1;
		lastTickTime = time;
		return ticks;
	}

	protected void onDecayWaste(int i) {}

	protected void leakRadiation(Level world, BlockPos pos) {
		Direction dir = Direction.values()[rand.nextInt(6)];
		if (!world.isClientSide)
			world.addFreshEntity(new NeutronEntity(world, pos, dir, NeutronEntity.NeutronType.WASTE));
	}

	public final int countWaste() {
		int count = 0;
		for (int i = 0; i < inv.getSlots(); i++)
			if (inv.getStackInSlot(i).is(ReactorItems.WASTE.get()))
				count += inv.getStackInSlot(i).getCount();
		return count;
	}

	public final boolean hasWaste() {
		return this.countWaste() > 0;
	}

	public static boolean isLongLivedWaste(ItemStack is) {
		Isotopes i = getIsotope(is);
		return i != null && isLongLivedWaste(i);
	}

	/** Half-life above six Minecraft years. */
	public static boolean isLongLivedWaste(Isotopes i) {
		return i.getMCHalfLife() > 6*MC_YEAR;
	}

	/** Original Feedable.feed: when the unit below takes the bottom slot, everything moves down and the bottom of the unit above comes in. */
	protected final void feed(Level world, BlockPos pos) {
		if (world.getBlockEntity(pos.below()) instanceof WasteUnitBlockEntity tile && tile.getClass() == this.getClass()) {
			int last = inv.getSlots()-1;
			if (tile.feedIn(inv.getStackInSlot(last))) {
				for (int i = last; i > 0; i--)
					inv.setStackInSlot(i, inv.getStackInSlot(i-1));
				if (world.getBlockEntity(pos.above()) instanceof WasteUnitBlockEntity up && up.getClass() == this.getClass())
					inv.setStackInSlot(0, up.feedOut());
				else
					inv.setStackInSlot(0, ItemStack.EMPTY);
			}
		}
		this.collapseInventory();
	}

	protected abstract void collapseInventory();

	public boolean feedIn(ItemStack is) {
		if (is.isEmpty())
			return true;
		if (!this.isItemValidForSlot(0, is))
			return false;
		if (inv.getStackInSlot(0).isEmpty()) {
			inv.setStackInSlot(0, is.copy());
			return true;
		}
		return false;
	}

	public ItemStack feedOut() {
		int last = inv.getSlots()-1;
		ItemStack is = inv.getStackInSlot(last).copy();
		inv.setStackInSlot(last, ItemStack.EMPTY);
		return is;
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	protected boolean canItemEnterFromSide(@Nullable Direction dir) {
		return true;
	}

	protected boolean canItemExitToSide(@Nullable Direction dir) {
		return true;
	}

	/** Original ISidedInventory: insertion from the allowed sides, extraction only of removable items to the allowed sides. */
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
				return WasteUnitBlockEntity.this.canItemEnterFromSide(side) ? inv.insertItem(slot, stack, simulate) : stack;
			}

			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				if (!WasteUnitBlockEntity.this.canItemExitToSide(side) || !WasteUnitBlockEntity.this.canRemoveItem(slot, inv.getStackInSlot(slot)))
					return ItemStack.EMPTY;
				return inv.extractItem(slot, amount, simulate);
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

	/** Original ReikaWorldHelper.canBlockSee: a ray from the point towards one of ten points of the block reaches it without a solid block. */
	public static boolean canBlockSee(Level world, BlockPos pos, Vec3 from, double range) {
		BlockState here = world.getBlockState(pos);
		range += 2;
		float[][] offs = {{0, 0, 0}, {1, 0, 0}, {0, 1, 0}, {1, 1, 0}, {0, 0, 1}, {1, 0, 1}, {0, 1, 1}, {1, 1, 1}, {0.5F, 0.5F, 0.5F}, {0, 0.5F, 0}};
		for (float[] o : offs) {
			Vec3 dir = new Vec3(pos.getX()+o[0]-from.x, pos.getY()+o[1]-from.y, pos.getZ()+o[2]-from.z).normalize();
			for (float i = 0; i <= range; i += 0.25F) {
				Vec3 v = from.add(dir.scale(i));
				BlockPos p = BlockPos.containing(v);
				if (p.equals(pos))
					return true;
				BlockState s = world.getBlockState(p);
				if (s.getBlock() != here.getBlock() && !s.getCollisionShape(world, p).isEmpty() && !(s.canBeReplaced() || !s.getFluidState().isEmpty()))
					break;
			}
		}
		return false;
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
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}

	@Override
	protected void writeAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeAdditional(NBT, registries);
		NBT.putLong("lasttime", lastTickTime);
	}

	@Override
	protected void readAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readAdditional(NBT, registries);
		lastTickTime = NBT.contains("lasttime") ? NBT.getLong("lasttime") : -1; //original: 0 when missing, which counts every tick since 1970 as passed
	}
}
