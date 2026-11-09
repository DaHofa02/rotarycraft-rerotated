/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.block.BlastFurnaceBlock;
import reika.rotarycraft.content.recipe.BlastCraftingRecipe;
import reika.rotarycraft.content.recipe.BlastFurnaceInput;
import reika.rotarycraft.content.recipe.BlastFurnaceRecipe;
import reika.rotarycraft.lib.data.InventoryHelper;
import reika.rotarycraft.lib.math.RandomHelper;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.DifficultyEffects;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryRecipeTypes;

/**
 * Original: Reika.RotaryCraft.TileEntities.Production.TileEntityBlastFurnace. Unpowered; its temperature follows the surroundings
 * (ambient, water, ice, fire, lava) and outside heat sources (gas turbine exhaust, Friction Heater). Recipes:
 * {@link BlastFurnaceRecipe} (additives + main items, e.g. HSLA steel) and {@link BlastCraftingRecipe} (shaped 3x3 above a temperature).
 * Not ported yet: the crafting pattern slot (slot 15; the pattern item is not ported), achievements, and collecting the stored XP
 * (originally only the Vacuum takes it).
 */
public class BlastFurnaceBlockEntity extends RotaryCraftBlockEntity implements FrictionHeatable, net.minecraft.world.Clearable {

	/** Vanilla Clearable: commands such as /setblock and /fill empty the inventory before replacing the block, so nothing drops. */
	@Override
	public void clearContent() {
		for (int i = 0; i < inv.getSlots(); i++)
			inv.setStackInSlot(i, ItemStack.EMPTY);
	}


	private int temperature;
	public int smeltTime = 0;

	public static final int SMELTTEMP = 600;
	public static final int BEDROCKTEMP = 1450;
	public static final int MAXTEMP = 2000;
	public static final float SMELT_XP = 0.6F;

	public static final int CENTER_ADDITIVE = 0;
	public static final int LOWER_ADDITIVE = 11;
	public static final int UPPER_ADDITIVE = 14;
	public static final int PATTERN_SLOT = 15;

	public static final int OUTPUT_CENTER = 10;
	public static final int OUTPUT_UPPER = 12;
	public static final int OUTPUT_LOWER = 13;

	private float xp;

	private int tempTimer;

	public boolean[] lockedSlots = new boolean[16];
	public boolean leaveLastItem;

	private final ItemStackHandler inv = new ItemStackHandler(16) {
		@Override
		protected void onContentsChanged(int slot) {
			BlastFurnaceBlockEntity.this.setChanged();
		}
	};

	public BlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.BLAST_FURNACE.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Blast Furnace";
	}

	/** The inventory as the GUI sees it (any item in any input slot, as with the original plain Slots). */
	public ItemStackHandler getInventory() {
		return inv;
	}

	private BlastFurnaceInput input() {
		List<ItemStack> li = new ArrayList<>(16);
		for (int i = 0; i < 16; i++)
			li.add(inv.getStackInSlot(i));
		return new BlastFurnaceInput(li, temperature);
	}

	@Nullable
	private BlastCraftingRecipe getCrafting() {
		Optional<RecipeHolder<BlastCraftingRecipe>> o = level.getRecipeManager().getRecipeFor(RotaryRecipeTypes.BLAST_CRAFTING.get(), this.input(), level);
		if (o.isEmpty())
			return null;
		BlastCraftingRecipe c = o.get().value();
		if (leaveLastItem) {
			for (int i = 1; i <= 9; i++) {
				if (!inv.getStackInSlot(i).isEmpty() && inv.getStackInSlot(i).getCount() == 1)
					return null;
			}
		}
		return c;
	}

	@Nullable
	private BlastFurnaceRecipe getRecipe() {
		Optional<RecipeHolder<BlastFurnaceRecipe>> o = level.getRecipeManager().getRecipeFor(RotaryRecipeTypes.BLAST_FURNACE.get(), this.input(), level);
		if (o.isEmpty())
			return null;
		BlastFurnaceRecipe rec = o.get().value();

		if (rec.needsEmpty()) {
			if (!inv.getStackInSlot(10).isEmpty() || !inv.getStackInSlot(13).isEmpty() || !inv.getStackInSlot(12).isEmpty())
				return null;
		}

		ItemStack out = rec.results().get(0).copy();
		int num = this.getProducedFor(rec);
		out.setCount(num*out.getCount());
		if (!this.checkCanMakeItem(out))
			return null;

		if (leaveLastItem) {
			for (int i = 1; i <= 9; i++) {
				if (!inv.getStackInSlot(i).isEmpty() && inv.getStackInSlot(i).getCount() == 1)
					return null;
			}
		}

		return rec;
	}

	private boolean checkCanMakeItem(ItemStack out) {
		return this.canAdd(out, 10) || this.canAdd(out, 13) || this.canAdd(out, 12);
	}

	private boolean canAdd(ItemStack is, int slot) {
		ItemStack in = inv.getStackInSlot(slot);
		if (in.isEmpty())
			return true;
		return ItemStack.isSameItemSameComponents(is, in) && is.getCount()+in.getCount() <= Math.min(64, is.getMaxStackSize());
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (world.isClientSide)
			return;
		tempTimer++;
		if (tempTimer >= 20) {
			tempTimer = 0;
			this.updateTemperature(world, pos);
		}

		BlastFurnaceRecipe rec = this.getRecipe();
		BlastCraftingRecipe bc = this.getCrafting();
		this.setActive(world, pos, state, rec != null || bc != null);
		if (bc != null) {
			if (bc.speed() <= 1 || world.getGameTime()%bc.speed() == 0)
				smeltTime++;
			if (smeltTime >= this.getOperationTime()) {
				this.craft(bc);
			}
		}
		else if (rec != null) {
			smeltTime++;
			if (smeltTime >= this.getOperationTime()) {
				this.make(rec);
			}
		}
		else {
			smeltTime = 0;
		}
	}

	/** Original getActiveTexture: the front glows while something can be made. */
	private void setActive(Level world, BlockPos pos, BlockState state, boolean active) {
		if (state.hasProperty(BlastFurnaceBlock.ACTIVE) && state.getValue(BlastFurnaceBlock.ACTIVE) != active)
			world.setBlock(pos, state.setValue(BlastFurnaceBlock.ACTIVE, active), 3);
	}

	private void craft(BlastCraftingRecipe bc) {
		smeltTime = 0;
		ItemStack out = bc.assemble(this.input(), level.registryAccess());

		if (!InventoryHelper.addOrSetStack(out, inv, 10))
			if (!InventoryHelper.addOrSetStack(out, inv, 12))
				if (!InventoryHelper.addOrSetStack(out, inv, 13))
					if (!this.checkSpreadFit(out, out.getCount()))
						return;

		xp += out.getCount()*bc.xp();
		Player ep = this.getPlacer();
		if (ep != null)
			out.onCraftedBy(level, ep, out.getCount());

		for (int i = 1; i < 10; i++) {
			if (!inv.getStackInSlot(i).isEmpty())
				InventoryHelper.decrStack(inv, i, 1);
		}
	}

	private void make(BlastFurnaceRecipe rec) {
		smeltTime = 0;

		int num = this.getProducedFor(rec);
		int made = num;

		if (rec.bonus() > 0) {
			double chance = DifficultyEffects.BONUSSTEEL.getDouble()*(ReikaMathLibrary.intpow(1.005, num*num)-1);
			if (RandomHelper.doWithChance(chance, rand)) {
				num *= 1+rand.nextFloat()*rec.bonus();
			}
		}

		List<ItemStack> results = rec.results();
		for (ItemStack res : results) {
			ItemStack out = res.copyWithCount(res.getCount()*num);
			if (!InventoryHelper.addOrSetStack(out, inv, 10))
				if (!InventoryHelper.addOrSetStack(out, inv, 12))
					if (!InventoryHelper.addOrSetStack(out, inv, 13))
						if (results.size() > 1 || !this.checkSpreadFit(out, out.getCount())) //original: no spreading for the steel-block case
							return;
		}

		xp += rec.xp()*num;

		rec.primary().ifPresent(a -> this.consume(a, CENTER_ADDITIVE, made));
		rec.secondary().ifPresent(a -> this.consume(a, LOWER_ADDITIVE, made));
		rec.tertiary().ifPresent(a -> this.consume(a, UPPER_ADDITIVE, made));

		for (int i = 1; i < 10; i++) {
			if (!inv.getStackInSlot(i).isEmpty())
				InventoryHelper.decrStack(inv, i, 1);
		}
	}

	private void consume(BlastFurnaceRecipe.Additive a, int slot, int made) {
		for (int i = 0; i < a.count(); i++) {
			if (RandomHelper.doWithChance(this.getConsumptionFactor(a.chanceToUse(), made), rand))
				InventoryHelper.decrStack(inv, slot, 1);
		}
	}

	private float getConsumptionFactor(float base, int made) {
		return Mth.clamp(base*made*DifficultyEffects.BLASTCONSUME.getChance(), base, 1);
	}

	private int getProducedFor(BlastFurnaceRecipe rec) {
		int num = 0;
		for (int i = 1; i < 10; i++) {
			if (rec.isValidMainItem(inv.getStackInSlot(i)))
				num++;
		}
		return rec.getNumberProduced(num);
	}

	public int getTemperatureScaled(int p1) {
		return ((p1*temperature)/MAXTEMP);
	}

	public float getXP() {
		return xp;
	}

	public void clearXP() {
		xp = 0;
	}

	/**
	 * Original checkSpreadFit: fills the three output slots up to a full stack each. Fixed here: the original added the last part to
	 * the upper output instead of the lower one, which could push that stack past its limit.
	 */
	private boolean checkSpreadFit(ItemStack is, int num) {
		int[] outs = {10, 12, 13};
		int maxfit = 0;
		for (int s : outs) {
			ItemStack at = inv.getStackInSlot(s);
			if (!ItemStack.isSameItemSameComponents(is, at))
				return false;
			maxfit += is.getMaxStackSize()-at.getCount();
		}
		if (num > maxfit)
			return false;
		for (int s : outs) {
			ItemStack at = inv.getStackInSlot(s);
			int f = is.getMaxStackSize()-at.getCount();
			if (f > num) {
				inv.setStackInSlot(s, at.copyWithCount(at.getCount()+num));
				return true;
			}
			inv.setStackInSlot(s, at.copyWithCount(at.getMaxStackSize()));
			num -= f;
		}
		return true;
	}

	public int getOperationTime() {
		int time = 2*((1500-(temperature-SMELTTEMP))/12); //1500 was MAXTEMP
		if (time < 1)
			return 1;
		return time;
	}

	public int getCookScaled(int p1) {
		return ((p1*smeltTime)/this.getOperationTime());
	}

	@Nullable
	private static Direction checkForAdjBlock(Level world, BlockPos pos, java.util.function.Predicate<BlockState> test) {
		for (Direction d : Direction.values()) {
			if (test.test(world.getBlockState(pos.relative(d))))
				return d;
		}
		return null;
	}

	/** Original updateTemperature, with RotaryAux.isNextToWater/Fire/Lava (water and lava by fluid tag, fire by #fire). */
	public void updateTemperature(Level world, BlockPos pos) {
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);

		if (checkForAdjBlock(world, pos, s -> s.getFluidState().is(FluidTags.WATER)) != null) {
			Tamb /= 2;
		}
		Direction iceside = checkForAdjBlock(world, pos, s -> s.is(Blocks.ICE));
		if (iceside == null)
			iceside = checkForAdjBlock(world, pos, s -> s.is(Blocks.PACKED_ICE));
		if (iceside != null) {
			if (Tamb > 0)
				Tamb /= 4;
			world.setBlockAndUpdate(pos.relative(iceside), Blocks.WATER.defaultBlockState());
		}
		int Tadd = 0;
		if (checkForAdjBlock(world, pos, s -> s.is(BlockTags.FIRE)) != null) {
			Tadd += Tamb >= 100 ? 100 : 200;
		}
		if (checkForAdjBlock(world, pos, s -> s.getFluidState().is(FluidTags.LAVA)) != null) {
			Tadd += Tamb >= 100 ? 400 : 600;
		}
		Tamb += Tadd;

		if (temperature > Tamb)
			temperature--;
		if (temperature > Tamb*2)
			temperature--;
		if (temperature < Tamb)
			temperature++;
		if (temperature*2 < Tamb)
			temperature++;
		if (temperature > MAXTEMP)
			temperature = MAXTEMP;
		if (temperature > 100) {
			Direction side = checkForAdjBlock(world, pos, s -> s.is(Blocks.SNOW_BLOCK));
			if (side == null)
				side = checkForAdjBlock(world, pos, s -> s.is(Blocks.SNOW));
			if (side != null)
				world.setBlockAndUpdate(pos.relative(side), Blocks.AIR.defaultBlockState());
			side = checkForAdjBlock(world, pos, s -> s.is(Blocks.ICE));
			if (side != null)
				world.setBlockAndUpdate(pos.relative(side), Blocks.WATER.defaultBlockState());
		}
	}

	/** Original: the blast furnace's front faces the player (4-sided metadata), kept in the blockstate for the model. */
	@Override
	public void setBlockMetadata(int meta) {
		super.setBlockMetadata(meta);
		if (level != null && !level.isClientSide) {
			BlockState bs = this.getBlockState();
			if (bs.hasProperty(BlastFurnaceBlock.FACING))
				level.setBlock(worldPosition, bs.setValue(BlastFurnaceBlock.FACING, BlastFurnaceBlock.frontFromMeta(meta)), 3);
		}
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("melt", smeltTime);
		NBT.putInt("temp", temperature);
		NBT.putFloat("exp", xp);

		int locks = 0;
		for (int i = 0; i < lockedSlots.length; i++)
			if (lockedSlots[i])
				locks |= 1 << i;
		NBT.putInt("locks", locks);
		NBT.putBoolean("last", leaveLastItem);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		smeltTime = NBT.getInt("melt");
		temperature = NBT.getInt("temp");
		xp = NBT.getFloat("exp");

		int locks = NBT.getInt("locks");
		for (int i = 0; i < lockedSlots.length; i++)
			lockedSlots[i] = (locks & (1 << i)) != 0;
		leaveLastItem = NBT.getBoolean("last");
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

	/** Original getInputTypesForItem: 0 main item, 1-3 primary/secondary/tertiary additive of some mainline recipe. */
	private Set<Integer> getInputTypesForItem(ItemStack is) {
		Set<Integer> set = new HashSet<>();
		for (RecipeHolder<BlastFurnaceRecipe> h : level.getRecipeManager().getAllRecipesFor(RotaryRecipeTypes.BLAST_FURNACE.get())) {
			BlastFurnaceRecipe r = h.value();
			if (r.isValidMainItem(is))
				set.add(0);
			else if (r.primary().isPresent() && r.primary().get().ingredient().test(is))
				set.add(1);
			else if (r.secondary().isPresent() && r.secondary().get().ingredient().test(is))
				set.add(2);
			else if (r.tertiary().isPresent() && r.tertiary().get().ingredient().test(is))
				set.add(3);
		}
		return set;
	}

	/** Original isItemValidForSlot / getSlotForItem (automation only; the GUI slots accept anything). */
	public boolean isItemValidForSlot(int slot, ItemStack is) {
		if (is.isEmpty() || slot >= PATTERN_SLOT)
			return false;
		if (lockedSlots[slot])
			return false;
		Set<Integer> slots = new HashSet<>();
		for (int i = 1; i <= 9; i++)
			if (ItemStack.isSameItemSameComponents(inv.getStackInSlot(i), is))
				slots.add(i);
		if (!slots.isEmpty()) {
			return slots.contains(slot);
		}

		Set<Integer> types = this.getInputTypesForItem(is);
		if (slot == CENTER_ADDITIVE)
			return types.contains(1);
		if (slot == LOWER_ADDITIVE)
			return types.contains(2);
		if (slot == UPPER_ADDITIVE)
			return types.contains(3);
		if (slot >= 1 && slot <= 9)
			return types.contains(0); //check this last, since there are fewer variants that go in additives
		return false;
	}

	/** Original ISidedInventory rules for pipes and hoppers: insertion as above, extraction only from the three outputs. */
	public IItemHandler getAutomationHandler() {
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
				return BlastFurnaceBlockEntity.this.isItemValidForSlot(slot, stack) ? inv.insertItem(slot, stack, simulate) : stack;
			}

			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				return slot == 10 || slot == 12 || slot == 13 ? inv.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
			}

			@Override
			public int getSlotLimit(int slot) {
				return 64;
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return BlastFurnaceBlockEntity.this.isItemValidForSlot(slot, stack);
			}
		};
	}

	@Override
	public int getRedstoneOverride() {
		return level != null && !level.isClientSide && this.getRecipe() == null ? 15 : 0;
	}

	@Override
	public void addTemperature(int temp) {
		temperature += temp;
	}

	@Override
	public int getTemperature() {
		return temperature;
	}

	@Override
	public void setTemperature(int temp) {
		temperature = temp;
	}

	@Override
	public int getMaxTemperature() {
		return MAXTEMP;
	}

	@Override
	public boolean allowExternalHeating() {
		return true;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public boolean canBeFrictionHeated() {
		return true;
	}

	@Override
	public float getMultiplier() {
		return 1;
	}

	@Override
	public void resetAmbientTemperatureTimer() {
		tempTimer = 0;
	}

	@Override
	public void onOverheat(Level world, BlockPos pos) {

	}

	public void toggleLock(int slot) {
		if (slot >= 0 && slot < PATTERN_SLOT) {
			lockedSlots[slot] = !lockedSlots[slot];
			this.syncAllData();
		}
	}

	public void setLeaveLastItem(boolean leave) {
		leaveLastItem = leave;
		this.syncAllData();
	}

	/** Original allowHeatExtraction (HeatConduction): TileEntityBlastFurnace, true. */
	@Override
	public boolean allowHeatExtraction() {
		return true;
	}
}
