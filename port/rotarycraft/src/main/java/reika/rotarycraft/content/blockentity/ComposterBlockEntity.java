/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.TileEntities.Farming.TileEntityComposter - the composter; needs no power. Between 40 and 70 deg C it
 * turns plant and animal matter (slot 0) with yeast (slot 1) into compost (slot 2): its timer advances by 1 + (T - 40)/4 per tick and
 * at 100 one item becomes 1-4 compost by its kind; the yeast is used up with a chance of 1 in (75 - T). Every second the temperature
 * moves by 1 towards the ambient temperature (by 2 while it is more than twice / less than half of it), which water beside it lowers
 * by 5 and ice by 15 and fire raises by 50 and lava by 200. Only compost comes out.
 */
public class ComposterBlockEntity extends RotaryCraftBlockEntity implements TemperatureMachine, net.minecraft.world.Clearable {

	public static final int MINTEMP = 40;
	public static final int KILLTEMP = 70;
	public static final int MAXTEMP = 100;
	/** Original StepTimer(100). */
	public static final int COOKTIME = 100;

	private int temperature;
	private int tempTimer;
	public int composterCookTime;

	private final ItemStackHandler inv = new ItemStackHandler(3) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			if (slot == 2)
				return false;
			return slot == 1 ? is.is(RotaryItems.YEAST.get()) : getCompostValue(is) > 0;
		}

		@Override
		protected void onContentsChanged(int slot) {
			ComposterBlockEntity.this.setChanged();
		}
	};

	public ComposterBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.COMPOSTER.get(), pos, state);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	/**
	 * Original enum CompostMatter: the compost one item gives. The 1.7.10 blocks with subtypes are their 1.21 tags (saplings, small
	 * flowers, leaves; tall grass = short grass, fern and dead bush; double plants = tall flowers, tall grass and large fern), raw and
	 * cooked "fish" are all four raw fish and the two cooked ones.
	 */
	public static int getCompostValue(ItemStack is) {
		if (is.isEmpty())
			return 0;
		if (is.is(Items.EGG) || is.is(Items.COOKIE) || is.is(Items.WHEAT) || is.is(RotaryItems.CANOLA_SEEDS.get()) || is.is(RotaryItems.CANOLA_HUSKS.get()))
			return 1;
		if (is.is(Items.SUGAR_CANE))
			return 2;
		if (is.is(ItemTags.SAPLINGS) || is.is(Items.LILY_PAD) || is.is(ItemTags.SMALL_FLOWERS) || is.is(Items.BROWN_MUSHROOM) || is.is(Items.RED_MUSHROOM))
			return 1;
		if (is.is(ItemTags.LEAVES) || is.is(Items.GRASS_BLOCK) || is.is(Items.VINE) || is.is(Items.SHORT_GRASS) || is.is(Items.FERN) || is.is(Items.DEAD_BUSH)
				|| is.is(ItemTags.TALL_FLOWERS) || is.is(Items.TALL_GRASS) || is.is(Items.LARGE_FERN))
			return 2;
		if (is.is(Items.BEEF) || is.is(Items.COOKED_BEEF) || is.is(Items.COOKED_PORKCHOP) || is.is(Items.PORKCHOP) || is.is(Items.COOKED_CHICKEN) || is.is(Items.CHICKEN))
			return 4;
		if (is.is(Items.COOKED_COD) || is.is(Items.COOKED_SALMON) || is.is(Items.COD) || is.is(Items.SALMON) || is.is(Items.TROPICAL_FISH) || is.is(Items.PUFFERFISH))
			return 3;
		if (is.is(Items.POTATO) || is.is(Items.CARROT) || is.is(Items.BAKED_POTATO) || is.is(Items.POISONOUS_POTATO) || is.is(Items.BREAD) || is.is(Items.APPLE)
				|| is.is(Items.MELON_SLICE))
			return 2;
		if (is.is(Items.ROTTEN_FLESH) || is.is(Items.SPIDER_EYE))
			return 3;
		return 0;
	}

	public int getScaledTimer(int a) {
		return a*composterCookTime/COOKTIME;
	}

	public int getScaledTemperature(int a) {
		return a*temperature/100;
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
		int value = this.getCompostValue();
		if (value > 0) {
			composterCookTime += 1+(temperature-MINTEMP)/4;
			if (composterCookTime >= COOKTIME) {
				inv.extractItem(0, 1, false);
				ItemStack out = inv.getStackInSlot(2);
				inv.setStackInSlot(2, new ItemStack(RotaryItems.part(CraftingPart.COMPOST), out.getCount()+value));
				if (rand.nextInt(75-temperature) == 0)
					inv.extractItem(1, 1, false);
				composterCookTime = 0;
			}
			this.setChanged();
		}
	}

	/** The compost the item in slot 0 gives now; 0 while too cold or too hot, without yeast or without room for it. */
	public int getCompostValue() {
		if (temperature < MINTEMP || temperature > KILLTEMP)
			return 0;
		if (inv.getStackInSlot(0).isEmpty() || !inv.getStackInSlot(1).is(RotaryItems.YEAST.get()))
			return 0;
		int value = getCompostValue(inv.getStackInSlot(0));
		ItemStack out = inv.getStackInSlot(2);
		if (value == 0 || out.isEmpty())
			return value;
		if (!out.is(RotaryItems.part(CraftingPart.COMPOST)))
			return 0;
		return out.getCount()+value <= out.getMaxStackSize() ? value : 0;
	}

	private static boolean isNextTo(Level world, BlockPos pos, java.util.function.Predicate<BlockState> test) {
		for (Direction d : Direction.values())
			if (test.test(world.getBlockState(pos.relative(d))))
				return true;
		return false;
	}

	private void updateTemperature(Level world, BlockPos pos) {
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		if (isNextTo(world, pos, s -> s.getFluidState().is(FluidTags.WATER)))
			Tamb -= 5;
		if (isNextTo(world, pos, s -> s.is(Blocks.ICE)))
			Tamb -= 15;
		if (isNextTo(world, pos, s -> s.is(BlockTags.FIRE)))
			Tamb += 50;
		if (isNextTo(world, pos, s -> s.getFluidState().is(FluidTags.LAVA)))
			Tamb += 200;
		int old = temperature;
		if (temperature > Tamb)
			temperature--;
		if (temperature > Tamb*2)
			temperature--;
		if (temperature < Tamb)
			temperature++;
		if (temperature*2 < Tamb)
			temperature++;
		if (old != temperature)
			this.syncAllData();
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public int getRedstoneOverride() {
		return 0;
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
	public boolean canBeCooledWithFins() {
		return true;
	}

	@Override
	public boolean allowExternalHeating() {
		return true;
	}

	/** Matter and yeast go into their slots from any side; only compost comes out (original canExtractItem). */
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
				if (!inv.getStackInSlot(slot).is(RotaryItems.part(CraftingPart.COMPOST)))
					return ItemStack.EMPTY;
				return inv.extractItem(slot, amount, simulate);
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
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("temperature", temperature);
		NBT.putInt("timer", composterCookTime);
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		temperature = NBT.getInt("temperature");
		composterCookTime = NBT.getInt("timer");
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
