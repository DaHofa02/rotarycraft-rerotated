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
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.content.item.NuclearWasteItem;
import reika.reactorcraft.radiation.Isotopes;
import reika.reactorcraft.reactor.NeutronTile;
import reika.reactorcraft.reactor.ReactorBlock;
import reika.reactorcraft.reactor.ReactorBlockEntity;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorItems;
import reika.rotarycraft.lib.math.RandomHelper;

/**
 * Original: Reika.ReactorCraft.TileEntities.Processing.TileEntityWasteDecayer - 15 slots (8 each) of waste, fed in from the top, the
 * short-lived products taken out at the bottom; a stack passes its waste down. Fission/fusion/breeder/thorium neutrons are caught with
 * 50 % and then, with a chance by temperature (0-5 % up to 150 C, 5-25 % up to 400 C; fast neutrons 2.2x), one long-lived waste item
 * decays into its product (the next heavier isotope x1, two heavier x0.5, otherwise a lighter one; lead products - no lead without other
 * mods - are not created). Its temperature follows the surroundings only (it is not a temperatured reactor part in the original, so neighbours lose heat to it without warming it). Sides show the column position, top and bottom #4.
 */
public class WasteDecayerBlockEntity extends ReactorBlockEntity implements NeutronTile, ReactorBlock, net.minecraft.world.Clearable {

	public static final int BASE_TEMP = 150;
	public static final int OPTIMAL_TEMP = 400;
	public static final int SIZE = 15;

	private final ItemStackHandler inv = new ItemStackHandler(SIZE) {
		@Override
		public int getSlotLimit(int slot) {
			return 8;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return is.is(ReactorItems.WASTE.get());
		}

		@Override
		protected void onContentsChanged(int slot) {
			WasteDecayerBlockEntity.this.setChanged();
		}
	};

	public WasteDecayerBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.WASTE_DECAYER.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Waste Decayer";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (!world.isClientSide)
			this.feed(world, pos);
		if (this.thermalTick() && !world.isClientSide)
			this.updateTemperature(world, pos);
	}

	private void feed(Level world, BlockPos pos) {
		if (world.getBlockEntity(pos.below()) instanceof WasteDecayerBlockEntity tile) {
			if (tile.feedIn(inv.getStackInSlot(SIZE-1))) {
				for (int i = SIZE-1; i > 0; i--)
					inv.setStackInSlot(i, inv.getStackInSlot(i-1));
				if (world.getBlockEntity(pos.above()) instanceof WasteDecayerBlockEntity up)
					inv.setStackInSlot(0, up.feedOut());
				else
					inv.setStackInSlot(0, ItemStack.EMPTY);
			}
		}
		this.collapseInventory();
	}

	/** Original collapseInventory: one move per tick - into an empty slot further on, or one item onto a matching stack. */
	private void collapseInventory() {
		for (int i = 0; i < SIZE; i++) {
			for (int k = SIZE-1; k > 0; k--) {
				ItemStack a = inv.getStackInSlot(k-1), b = inv.getStackInSlot(k);
				if (b.isEmpty() && !a.isEmpty()) {
					inv.setStackInSlot(k, a);
					inv.setStackInSlot(k-1, ItemStack.EMPTY);
					return;
				}
				else if (!a.isEmpty() && !b.isEmpty() && ItemStack.isSameItemSameComponents(a, b) && b.getCount() < Math.min(b.getMaxStackSize(), 8)) {
					inv.setStackInSlot(k, b.copyWithCount(b.getCount()+1));
					inv.setStackInSlot(k-1, a.getCount() > 1 ? a.copyWithCount(a.getCount()-1) : ItemStack.EMPTY);
					return;
				}
			}
		}
	}

	private boolean feedIn(ItemStack is) {
		if (is.isEmpty())
			return true;
		if (!is.is(ReactorItems.WASTE.get()))
			return false;
		if (inv.getStackInSlot(0).isEmpty()) {
			inv.setStackInSlot(0, is.copy());
			return true;
		}
		return false;
	}

	private ItemStack feedOut() {
		ItemStack is = inv.getStackInSlot(SIZE-1).copy();
		inv.setStackInSlot(SIZE-1, ItemStack.EMPTY);
		return is;
	}

	@Override
	public boolean onNeutron(NeutronEntity e, Level world, BlockPos pos) {
		if (!world.isClientSide && e.getNeutronType().canIrradiateMaterials() && RandomHelper.doWithChance(50, rand)) {
			if (RandomHelper.doWithChance(this.getDecayChance()*e.getNeutronSpeed().getWasteConversionMultiplier(), rand))
				this.tryDecay();
			return true;
		}
		return false;
	}

	/** Original getDecayChance (linear interpolation, percent). */
	public double getDecayChance() {
		return temperature < BASE_TEMP ? lerp(temperature, 20, BASE_TEMP, 0, 5) : lerp(temperature, BASE_TEMP, OPTIMAL_TEMP, 5, 25);
	}

	private static double lerp(double x, double x1, double x2, double y1, double y2) {
		return y1+(x-x1)/(x2-x1)*(y2-y1);
	}

	public boolean tryDecay() {
		for (int i = 0; i < SIZE; i++) {
			ItemStack is = inv.getStackInSlot(i);
			if (!is.isEmpty() && WasteUnitBlockEntity.isLongLivedWaste(is) && this.tryDecay(i, is))
				return true;
		}
		return false;
	}

	private boolean tryDecay(int i, ItemStack is) {
		Isotopes.DecayData split = WasteUnitBlockEntity.getIsotope(is).getDecay();
		if (split == null) {
			inv.extractItem(i, 1, false);
			return true;
		}
		int amt = (int)Math.floor(split.amount());
		if (RandomHelper.doWithChance(split.amount()-amt, rand))
			amt++;
		if (amt == 0)
			return false;
		if (split.product().isotope() == null) //lead: only from other mods in the original
			return true;
		ItemStack add = NuclearWasteItem.of(split.product().isotope()).copyWithCount(amt);
		if (ItemHandlerHelper.insertItemStacked(inv, add, true).isEmpty()) {
			ItemHandlerHelper.insertItemStacked(inv, add, false);
			inv.extractItem(i, 1, false);
			return true;
		}
		return false;
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	/** Items enter from the top and leave (short-lived waste only) at the bottom. */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return new IItemHandler() {
			@Override
			public int getSlots() {
				return SIZE;
			}

			@Override
			public ItemStack getStackInSlot(int slot) {
				return inv.getStackInSlot(slot);
			}

			@Override
			public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
				return side == Direction.UP || side == null ? inv.insertItem(slot, stack, simulate) : stack;
			}

			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				if (!(side == Direction.DOWN || side == null) || WasteUnitBlockEntity.isLongLivedWaste(inv.getStackInSlot(slot)))
					return ItemStack.EMPTY;
				return inv.extractItem(slot, amount, simulate);
			}

			@Override
			public int getSlotLimit(int slot) {
				return 8;
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return inv.isItemValid(slot, stack);
			}
		};
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < SIZE; i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}

	@Override
	public void clearContent() {
		for (int i = 0; i < SIZE; i++)
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
}
