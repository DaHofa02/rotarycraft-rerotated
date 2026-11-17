/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.item.CraftPatternItem;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/**
 * The auto crafter, written for the port from the original's behaviour. 18 slots for crafting patterns, each with its output slot and a slot
 * for what the recipe leaves behind (empty buckets and the like). With 1024 W (from any side, all at the same speed) it crafts from the
 * inventory on top of it. Modes: request (each GUI request crafts one cycle of that pattern), continuous (crafts whatever it has
 * ingredients for, each pattern every 4 ticks), sustain (crafts a pattern until its output slot holds the set number). Outputs and
 * leftovers can be taken from any side.
 * Changes: Applied Energistics supply and the original's lag profiling and crafting of intermediate items are left out.
 */
public class AutoCrafterBlockEntity extends PowerReceiverBlockEntity {

	public static final int SIZE = 18;
	public static final int OUTPUT_OFFSET = 18;
	public static final int CONTAINER_OFFSET = 36;
	public static final int PERIOD = 4;

	public enum Mode {
		REQUEST, CONTINUOUS, SUSTAIN;

		public Mode next() {
			return values()[(this.ordinal()+1)%values().length];
		}
	}

	private final ItemStackHandler inv = new ItemStackHandler(54) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return slot >= SIZE || is.is(RotaryItems.CRAFT_PATTERN.get()); //outputs and leftovers: filled by the crafter (the capability and menu keep others out)
		}

		@Override
		public int getSlotLimit(int slot) {
			return slot < SIZE ? 1 : 64;
		}

		@Override
		protected void onContentsChanged(int slot) {
			AutoCrafterBlockEntity.this.setChanged();
		}
	};
	private Mode mode = Mode.REQUEST;
	private final int[] thresholds = new int[SIZE];
	private final int[] requests = new int[SIZE];
	public int crafted;

	public AutoCrafterBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.AUTO_CRAFTER.get(), pos, state, 1, 1, 1024);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	/** Capability: outputs and leftovers out from any side; nothing in (patterns go in by hand). */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return new net.neoforged.neoforge.items.wrapper.RangedWrapper(inv, OUTPUT_OFFSET, 54) {
			@Override
			public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
				return stack;
			}
		};
	}

	public Mode getMode() {
		return mode;
	}

	public void stepMode() {
		mode = mode.next();
		this.syncAllData();
	}

	public int getThreshold(int i) {
		return thresholds[i];
	}

	public void setThreshold(int i, int n) {
		thresholds[i] = Math.max(0, Math.min(64, n));
		this.syncAllData();
	}

	/** Request mode: one more cycle of that pattern. */
	public void request(int i) {
		if (power >= MINPOWER)
			requests[i]++;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		this.readPowerSummative();
		if (power < MINPOWER || this.getTicksExisted()%PERIOD != 0)
			return;
		IItemHandler src = world.getCapability(Capabilities.ItemHandler.BLOCK, pos.above(), Direction.DOWN);
		for (int i = 0; i < SIZE; i++) {
			boolean want = switch (mode) {
				case REQUEST -> requests[i] > 0;
				case CONTINUOUS -> true;
				case SUSTAIN -> inv.getStackInSlot(OUTPUT_OFFSET+i).getCount() < thresholds[i];
			};
			if (want && src != null && this.craft(world, i, src) && mode == Mode.REQUEST)
				requests[i]--;
		}
	}

	/** One crafting cycle of pattern i from the ingredient inventory, if everything is there and the output fits. */
	private boolean craft(Level world, int i, IItemHandler src) {
		ItemStack pattern = inv.getStackInSlot(i);
		if (!pattern.is(RotaryItems.CRAFT_PATTERN.get()))
			return false;
		NonNullList<ItemStack> grid = CraftPatternItem.getGrid(pattern);
		CraftingInput in = CraftingInput.of(3, 3, grid);
		var recipe = world.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, in, world);
		if (recipe.isEmpty())
			return false;
		ItemStack out = recipe.get().value().assemble(in, world.registryAccess());
		if (out.isEmpty() || !inv.insertItem(OUTPUT_OFFSET+i, out, true).isEmpty())
			return false;
		List<ItemStack> leftovers = new ArrayList<>(recipe.get().value().getRemainingItems(in));
		leftovers.removeIf(ItemStack::isEmpty);
		for (ItemStack l : leftovers)
			if (!inv.insertItem(CONTAINER_OFFSET+i, l, true).isEmpty())
				return false;
		//take the ingredients (same item and components as in the pattern); simulate first
		List<ItemStack> need = new ArrayList<>();
		for (ItemStack g : grid)
			if (!g.isEmpty()) {
				ItemStack found = need.stream().filter(n -> ItemStack.isSameItemSameComponents(n, g)).findFirst().orElse(null);
				if (found != null)
					found.grow(1);
				else
					need.add(g.copyWithCount(1));
			}
		for (ItemStack n : need)
			if (count(src, n) < n.getCount())
				return false;
		for (ItemStack n : need)
			take(src, n);
		inv.insertItem(OUTPUT_OFFSET+i, out, false);
		for (ItemStack l : leftovers)
			inv.insertItem(CONTAINER_OFFSET+i, l, false);
		crafted++;
		return true;
	}

	private static int count(IItemHandler h, ItemStack like) {
		int n = 0;
		for (int s = 0; s < h.getSlots(); s++)
			if (ItemStack.isSameItemSameComponents(h.getStackInSlot(s), like))
				n += h.getStackInSlot(s).getCount();
		return n;
	}

	private static void take(IItemHandler h, ItemStack like) {
		int left = like.getCount();
		for (int s = 0; s < h.getSlots() && left > 0; s++)
			if (ItemStack.isSameItemSameComponents(h.getStackInSlot(s), like))
				left -= h.extractItem(s, left, false).getCount();
	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < 54; i++)
			Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("inv", inv.serializeNBT(registries));
		NBT.putInt("mode", mode.ordinal());
		NBT.putIntArray("thresh", thresholds);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		inv.deserializeNBT(registries, NBT.getCompound("inv"));
		mode = Mode.values()[Math.floorMod(NBT.getInt("mode"), Mode.values().length)];
		int[] t = NBT.getIntArray("thresh");
		System.arraycopy(t, 0, thresholds, 0, Math.min(t.length, SIZE));
	}
}
