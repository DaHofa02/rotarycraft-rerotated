/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.item.EngineUpgradeItem;
import reika.rotarycraft.content.recipe.WorktableRecipe;
import reika.rotarycraft.lib.data.InventoryHelper;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryComponents;
import reika.rotarycraft.registry.RotaryItems;
import reika.rotarycraft.registry.RotaryRecipeTypes;
import reika.rotarycraft.registry.RotarySounds;

/**
 * Original: Reika.RotaryCraft.TileEntities.Production.TileEntityWorktable. Slots 0-8 are the grid, 9-17 the outputs (13 is the main
 * output), 18 the crafting pattern (pattern item not ported yet). Crafting happens from the GUI (taking from the main output) or on a
 * rising redstone edge; with an empty grid except the centre, a redstone pulse takes the centre item apart again (uncrafting). The
 * integrated redstone upgrade pulses by itself every 6 ticks. Chargeable tools are charged from wound coils. Not ported yet (their
 * items do not exist): jetpack and bedrock armour upgrades.
 */
public class WorktableBlockEntity extends RotaryCraftBlockEntity implements UpgradeableMachine, net.minecraft.world.Clearable {

	/** Vanilla Clearable: commands such as /setblock and /fill empty the inventory before replacing the block, so nothing drops. */
	@Override
	public void clearContent() {
		for (int i = 0; i < inv.getSlots(); i++)
			inv.setStackInSlot(i, ItemStack.EMPTY);
	}


	public static final int OUTPUT = 13;

	private boolean hasUpgrade;
	private boolean lastRedstone;

	private final ItemStackHandler inv = new ItemStackHandler(19) {
		@Override
		protected void onContentsChanged(int slot) {
			WorktableBlockEntity.this.setChanged();
		}
	};

	public WorktableBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.WORKTABLE.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Worktable";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public static CraftingInput matrix(IItemHandler inv) {
		List<ItemStack> li = new ArrayList<>(9);
		for (int i = 0; i < 9; i++)
			li.add(inv.getStackInSlot(i));
		return CraftingInput.of(3, 3, li);
	}

	/** Original WorktableRecipes.findMatchingRecipe. */
	@Nullable
	public static WorktableRecipe findMatchingRecipe(Level world, IItemHandler inv) {
		CraftingInput ic = matrix(inv);
		if (ic.isEmpty())
			return null;
		Optional<RecipeHolder<WorktableRecipe>> o = world.getRecipeManager().getRecipeFor(RotaryRecipeTypes.WORKTABLE.get(), ic, world);
		return o.map(RecipeHolder::value).orElse(null);
	}

	private boolean isMatrixEmpty() {
		for (int i = 0; i < 9; i++)
			if (!inv.getStackInSlot(i).isEmpty())
				return false;
		return true;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (world.isClientSide)
			return;
		boolean signal = world.hasNeighborSignal(pos);
		if (signal && !lastRedstone)
			this.onPositiveRedstoneEdge();
		lastRedstone = signal;
		if (this.isMatrixEmpty())
			return;
		if (hasUpgrade && this.getTicksExisted()%6 == 0 && !signal) {
			this.onPositiveRedstoneEdge();
			world.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5F, 0.675F);
		}
		if (this.isReadyToCraft() && this.getTicksExisted()%4 == 0)
			this.chargeTools();
	}

	/**
	 * Original chargeTools: with exactly a wound coil and a chargeable tool in the worktable (17 of 19 slots empty), the tool takes the
	 * coil's charge and the coil keeps what the tool returns (the tools swap the two); the tool goes to slot 9, the coil to slot 10.
	 * (The original also recharged RotaryCraft's own charged tools and armour, which are not ported yet.)
	 */
	private void chargeTools() {
		int coilslot = -1, toolslot = -1, empty = 0;
		for (int i = 0; i < inv.getSlots(); i++) {
			ItemStack is = inv.getStackInSlot(i);
			if (is.isEmpty())
				empty++;
			else if (coilslot == -1 && is.is(RotaryItems.SPRING.get()))
				coilslot = i;
			else if (toolslot == -1 && is.getItem() instanceof reika.rotarycraft.api.interfaces.ChargeableTool)
				toolslot = i;
		}
		if (coilslot == -1) {
			for (int i = 0; i < inv.getSlots(); i++)
				if (inv.getStackInSlot(i).is(RotaryItems.STRONGCOIL.get()))
					coilslot = i;
		}
		if (toolslot == -1 || coilslot == -1 || empty != 17)
			return;
		ItemStack coil = inv.getStackInSlot(coilslot);
		ItemStack tool = inv.getStackInSlot(toolslot).copy();
		int newcoil = ((reika.rotarycraft.api.interfaces.ChargeableTool)tool.getItem()).setCharged(tool, reika.rotarycraft.content.item.CoilItem.getCharge(coil),
				coil.is(RotaryItems.STRONGCOIL.get()));
		ItemStack coilOut = reika.rotarycraft.content.item.CoilItem.withCharge(coil.copy(), newcoil);
		inv.setStackInSlot(toolslot, ItemStack.EMPTY);
		inv.setStackInSlot(coilslot, ItemStack.EMPTY);
		inv.setStackInSlot(9, tool);
		inv.setStackInSlot(10, coilOut);
	}

	private void onPositiveRedstoneEdge() {
		if (!this.craft()) {
			if (this.canUncraft())
				this.uncraft();
		}
	}

	private boolean craft() {
		WorktableRecipe wr = findMatchingRecipe(level, inv);
		if (wr != null) {
			return this.handleCrafting(wr, this.getPlacer(), false);
		}
		return false;
	}

	public boolean isReadyToCraft() {
		for (int i = 9; i < 18; i++) {
			if (!inv.getStackInSlot(i).isEmpty())
				return false;
		}
		return true;
	}

	/**
	 * Original handleCrafting. Fixed here: the original only checked the main output's stack size, so a different item already lying
	 * there (possible when crafting by redstone) made the result vanish; now crafting stops instead.
	 */
	public boolean handleCrafting(WorktableRecipe wr, @Nullable Player ep, boolean craftAll) {
		int maxCrafts = craftAll ? this.getSmallestInputStack() : 1;
		int crafts = 0;
		if (wr instanceof WorktableRecipe.Recycling rec) {
			int i = 9;
			for (ItemStack is : rec.getSplitOutput()) {
				InventoryHelper.addOrSetStack(is, inv, i);
				i++;
			}
			crafts = 1;
		}
		else {
			for (int i = 0; i < maxCrafts; i++) {
				ItemStack is = wr.assemble(matrix(inv), level.registryAccess());
				ItemStack at = inv.getStackInSlot(OUTPUT);
				if (!at.isEmpty() && (!ItemStack.isSameItemSameComponents(at, is) || at.getCount()+is.getCount() > Math.min(64, is.getMaxStackSize())))
					break;
				if (ep != null)
					is.onCraftedBy(level, ep, is.getCount());
				InventoryHelper.addOrSetStack(is, inv, OUTPUT);
				crafts++;
			}
		}
		if (crafts > 0) {
			for (int i = 0; i < 9; ++i) {
				if (!inv.getStackInSlot(i).isEmpty())
					InventoryHelper.decrStack(inv, i, crafts);
			}
			RotarySounds.play(level, worldPosition, RotarySounds.CRAFT.get(), 0.3F, 1.5F);
			return true;
		}
		return false;
	}

	private int getSmallestInputStack() {
		int smallest = Integer.MAX_VALUE;
		for (int i = 0; i < 9; ++i) {
			ItemStack item = inv.getStackInSlot(i);
			if (!item.isEmpty() && item.getCount() < smallest) {
				smallest = item.getCount();
			}
		}
		return smallest < Integer.MAX_VALUE ? smallest : -1;
	}

	/** Original WorktableRecipes.getInputRecipe: a (non-recycling) recipe making this item; lubricant and bearing data are ignored. */
	@Nullable
	private WorktableRecipe getInputRecipe(ItemStack is) {
		ItemStack cmp = is.copy();
		cmp.remove(RotaryComponents.GEARBOX_DATA.get());
		List<RecipeHolder<WorktableRecipe>> all = new ArrayList<>(level.getRecipeManager().getAllRecipesFor(RotaryRecipeTypes.WORKTABLE.get()));
		all.sort(java.util.Comparator.comparing(h -> h.id().toString())); //the original searched in registration order; recipe ids give a fixed order
		for (RecipeHolder<WorktableRecipe> h : all) {
			WorktableRecipe wr = h.value();
			if (wr.isRecycling())
				continue;
			ItemStack out = wr.getResultItem(level.registryAccess()).copy();
			out.remove(RotaryComponents.GEARBOX_DATA.get());
			if (ItemStack.isSameItem(cmp, out) && cmp.getCount() >= out.getCount()) {
				if (cmp.getComponentsPatch().isEmpty() || ItemStack.isSameItemSameComponents(cmp, out))
					return wr;
			}
		}
		return null;
	}

	/**
	 * Original isNotUncraftable: damaged tools and machines (gear damage, gas turbine damage), anything holding lubricant, gearboxes
	 * whose bearing does not match, enchanted items.
	 */
	private boolean isNotUncraftable(ItemStack is) {
		if (is.isDamageableItem() && is.getDamageValue() > 0)
			return true;
		RotaryComponents.GearboxData data = is.get(RotaryComponents.GEARBOX_DATA.get());
		if (data != null) {
			if (data.damage() > 0 || data.lubricant() > 0)
				return true;
			if (data.bearing().material != is.getOrDefault(RotaryComponents.GEARBOX_TYPE.get(), data.bearing()).material)
				return true;
		}
		if (is.getOrDefault(RotaryComponents.JET_DAMAGE.get(), 0) > 0)
			return true;
		return is.isEnchanted();
	}

	public boolean canUncraft() {
		boolean can = false;
		for (int i = 0; i < 9; i++) {
			ItemStack is = inv.getStackInSlot(i);
			if (i == 4) {
				if (is.isEmpty() || this.isNotUncraftable(is))
					return false;
				WorktableRecipe ir = this.getInputRecipe(is);
				if (ir == null)
					return false;
				List<Ingredient> in = ir.getGridLayout();
				boolean flag = true;
				for (int k = 0; k < 9; k++) {
					if (k < in.size() && !in.get(k).isEmpty()) {
						ItemStack at = inv.getStackInSlot(k+9);
						if (!at.isEmpty()) {
							if (!in.get(k).test(at))
								flag = false;
							if (at.getCount() >= Math.min(64, at.getMaxStackSize()))
								flag = false;
						}
					}
				}
				can = flag;
			}
			else {
				if (!is.isEmpty())
					return false;
			}
		}
		return can;
	}

	private void uncraft() {
		ItemStack is = inv.getStackInSlot(4);
		WorktableRecipe ir = this.getInputRecipe(is);
		if (ir == null)
			return;
		List<Ingredient> in = ir.getGridLayout();

		InventoryHelper.decrStack(inv, 4, ir.getResultItem(level.registryAccess()).getCount());

		for (int i = 0; i < 9 && i < in.size(); i++) {
			if (!in.get(i).isEmpty()) {
				ItemStack at = inv.getStackInSlot(i+9);
				if (at.isEmpty()) {
					ItemStack[] options = in.get(i).getItems();
					if (options.length > 0)
						inv.setStackInSlot(i+9, options[0].copyWithCount(1));
				}
				else {
					inv.setStackInSlot(i+9, at.copyWithCount(at.getCount()+1));
				}
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
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("redstoneUpgrade", hasUpgrade);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		hasUpgrade = NBT.getBoolean("redstoneUpgrade");
	}

	@Override
	protected void writeAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeAdditional(NBT, registries);
		NBT.put("inv", inv.serializeNBT(registries));
		NBT.putBoolean("lastRedstone", lastRedstone);
	}

	@Override
	protected void readAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readAdditional(NBT, registries);
		inv.deserializeNBT(registries, NBT.getCompound("inv"));
		lastRedstone = NBT.getBoolean("lastRedstone");
	}

	@Override
	public void onEMP() {

	}

	/** Original isItemValidForSlot / canExtractItem: pipes and hoppers fill the grid and empty the outputs. */
	public IItemHandler getAutomationHandler() {
		return new IItemHandler() {
			@Override
			public int getSlots() {
				return 18;
			}

			@Override
			public ItemStack getStackInSlot(int slot) {
				return inv.getStackInSlot(slot);
			}

			@Override
			public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
				return slot < 9 ? inv.insertItem(slot, stack, simulate) : stack;
			}

			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				return slot >= 9 ? inv.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
			}

			@Override
			public int getSlotLimit(int slot) {
				return 64;
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return slot < 9;
			}
		};
	}

	@Override
	public void upgrade(ItemStack is) {
		this.addRedstoneUpgrade();
	}

	@Override
	public boolean canUpgradeWith(ItemStack item) {
		return !this.hasRedstoneUpgrade() && EngineUpgradeItem.is(item, EngineUpgradeItem.Upgrades.REDSTONE);
	}

	public void addRedstoneUpgrade() {
		hasUpgrade = true;
	}

	public boolean hasRedstoneUpgrade() {
		return hasUpgrade;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < inv.getSlots(); i++)
			net.minecraft.world.Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
		if (this.hasRedstoneUpgrade())
			net.minecraft.world.Containers.dropItemStack(world, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, new ItemStack(RotaryItems.UPGRADE_REDSTONE.get()));
	}
}
