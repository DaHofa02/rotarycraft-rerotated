/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.content.extractor.ExtractorOres;
import reika.rotarycraft.content.extractor.ExtractorOres.Bonus;
import reika.rotarycraft.content.extractor.ExtractorOres.Ore;
import reika.rotarycraft.content.extractor.ExtractorOres.OreRarity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.lib.data.InventoryHelper;
import reika.rotarycraft.lib.math.RandomHelper;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.TileEntities.Processing.TileEntityExtractor. Four stages run in parallel, each with its own minimum
 * torque/speed/power (PowerReceivers.EXTRACTOR) and duration (DurationRegistry.EXTRACTOR), powered from below: ore -> dust (drill),
 * dust -> slurry and slurry -> solution (125 mB water each), solution -> flakes (with ExtractorBonus by-products in slot 8). Every
 * step has a chance to double its output (50 %, 90 % for rare ores), so one ore gives up to 16 flakes; a bedrock drill doubles stage 1.
 * Slots: 0-3 inputs, 4-7 outputs (moved on to the next input automatically), 8 bonus, 9 drill (only with "Extractor Drill Wears Down").
 */
public class ExtractorBlockEntity extends PowerReceiverBlockEntity {

	public static final int oreCopy = 50; //50% chance of doubling -> 1.5^4 = 5.1
	public static final int oreCopyNether = 80; //80% chance of doubling -> 1.8^4 = 10.5 (nether mod ores, not ported yet)
	public static final int oreCopyRare = 90; //90% chance of doubling -> 1.9^4 = 13.1

	/** Original PowerReceivers.EXTRACTOR per stage. */
	public static final int[] MIN_TORQUE = {512, 1, 1, 256};
	public static final int[] MIN_SPEED = {1, 2048, 8192, 1};
	public static final long[] MIN_POWER = {65536, 16384, 32768, 65536};
	/** Original DurationRegistry.EXTRACTOR: DurationFormula(base, scale) per stage. */
	private static final int[][] DURATION = {{900, 60}, {400, 20}, {600, 30}, {1200, 80}};

	/** The number of ticks that the current item has been cooking for */
	private int[] extractorCookTime = new int[4];

	public static final int DRILL_LIFE = 4096;
	private int drillTime = RotaryConfig.EXTRACTORMAINTAIN.get() ? 0 : DRILL_LIFE;
	private boolean bedrock = false;

	public static final int CAPACITY = 16000;

	public boolean idle = false;

	private final FluidTank tank = new FluidTank(CAPACITY, fs -> fs.is(Fluids.WATER));

	private final ItemStackHandler inv = new ItemStackHandler(10) {
		@Override
		protected void onContentsChanged(int slot) {
			ExtractorBlockEntity.this.setChanged();
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return ExtractorBlockEntity.this.isItemValidForSlot(slot, stack);
		}
	};

	public ExtractorBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.EXTRACTOR.get(), pos, state, 0, 0, 0);
	}

	@Override
	public String getName() {
		return "Extractor";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	/** Original upgrade(): right-click with a bedrock drill. */
	public boolean upgrade(Level world) {
		if (bedrock)
			return false;
		bedrock = true;
		ItemStack old = inv.getStackInSlot(9);
		if (!old.isEmpty())
			net.minecraft.world.Containers.dropItemStack(world, worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5, old.copy());
		inv.setStackInSlot(9, new ItemStack(RotaryItems.part(CraftingPart.BEDROCKDRILL)));
		this.syncAllData();
		return true;
	}

	public boolean isBedrock() {
		return bedrock;
	}

	public int getCookTime(int stage) {
		return extractorCookTime[stage];
	}

	public void testIdle() {
		boolean works = false;
		for (int i = 0; i < 4; i++) {
			if (this.canProcess(i))
				works = true;
		}
		idle = !works;
	}

	private int getSmeltNumber(int stage, @Nullable Ore ore) {
		if (bedrock && stage == 0)
			return 2;
		if (ore != null && ore.rarity == OreRarity.RARE) {
			return RandomHelper.doWithChance(oreCopyRare/100D, rand) ? 2 : 1;
		}
		return RandomHelper.doWithChance(oreCopy/100D, rand) ? 2 : 1;
	}

	private void throughPut() {
		for (int i = 1; i < 4; i++) {
			ItemStack out = inv.getStackInSlot(i+3);
			if (!out.isEmpty()) {
				ItemStack in = inv.getStackInSlot(i);
				if (in.isEmpty()) {
					inv.setStackInSlot(i, out);
					inv.setStackInSlot(i+3, ItemStack.EMPTY);
				}
				else if (in.getCount() < in.getMaxStackSize()) {
					if (ItemStack.isSameItemSameComponents(in, out)) {
						int amt = Math.min(out.getCount(), in.getMaxStackSize()-in.getCount());
						amt = Math.min(amt, this.getNumberConsecutiveOperations(i));
						if (amt > 0) {
							inv.setStackInSlot(i, in.copyWithCount(in.getCount()+amt));
							InventoryHelper.decrStack(inv, i+3, amt);
						}
					}
				}
			}
		}
	}

	public int getCookProgressScaled(int par1, int i) {
		int j = i+1;
		int time = -1;
		switch (j) {
			case 1 -> time = 30*(30-(int)(2*ReikaMathLibrary.logbase(omega, 2)));
			case 2 -> time = (800-(int)(40*ReikaMathLibrary.logbase(omega, 2)))/2;
			case 3 -> time = 600-(int)(30*ReikaMathLibrary.logbase(omega, 2));
			case 4 -> time = 1200-(int)(80*ReikaMathLibrary.logbase(omega, 2));
		}
		if (time == -1)
			return 0;
		if (time <= 0)
			time = 1;
		return (extractorCookTime[i]*par1)/2/time;
	}

	public int getDrillLifeScaled(int a) {
		return bedrock ? a : drillTime*a/DRILL_LIFE;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		read = Direction.DOWN; //original getPowerBelow
		if (world.isClientSide)
			return;
		this.readPower();
		this.testIdle();
		this.throughPut();
		if (!bedrock) {
			if (RotaryConfig.EXTRACTORMAINTAIN.get()) {
				if (drillTime <= 0 && inv.getStackInSlot(9).is(RotaryItems.part(CraftingPart.DRILL))) {
					InventoryHelper.decrStack(inv, 9, 1);
					drillTime = DRILL_LIFE;
				}
			}
			else {
				drillTime = DRILL_LIFE;
				inv.setStackInSlot(9, ItemStack.EMPTY);
			}
		}
		for (int i = 0; i < 4; i++) {
			int n = this.getNumberConsecutiveOperations(i);
			for (int k = 0; k < n; k++)
				this.doOperation(i);
		}
	}

	private boolean doOperation(int i) {
		if (this.canProcess(i)) {
			extractorCookTime[i]++;
			int time = this.getOperationTime(i);
			if (extractorCookTime[i] >= time) {
				extractorCookTime[i] = 0;
				this.processItem(i);
			}
			return true;
		}
		else {
			extractorCookTime[i] = 0;
			return false;
		}
	}

	private boolean canProcess(int i) {
		if (power < MIN_POWER[i] || omega < MIN_SPEED[i] || torque < MIN_TORQUE[i])
			return false;

		if (i == 0 && !bedrock && drillTime <= 0 && RotaryConfig.EXTRACTORMAINTAIN.get())
			return false;

		if ((i == 1 || i == 2) && tank.isEmpty())
			return false;

		ItemStack in = inv.getStackInSlot(i);
		if (in.isEmpty())
			return false;
		ItemStack out = inv.getStackInSlot(i+4);
		if (!out.isEmpty() && out.getCount()+1 >= out.getMaxStackSize())
			return false;
		ItemStack b = inv.getStackInSlot(8);
		if (!b.isEmpty()) {
			if (b.getCount()+1 > b.getMaxStackSize())
				return false;
			if (!inv.getStackInSlot(3).isEmpty()) {
				Bonus bonus = Bonus.getBonusForIngredient(inv.getStackInSlot(3));
				if (bonus != null) {
					if (!ItemStack.isSameItemSameComponents(bonus.getBonusItem(), b))
						return false;
				}
			}
		}
		Ore ore = ExtractorOres.getOre(in);
		if (ore == null)
			return false;
		if (ExtractorOres.getInputStage(in) != i) //original: the slot only accepts its own stage
			return false;

		ItemStack itemstack = ExtractorOres.getExtractionResult(in);
		if (itemstack.isEmpty())
			return false;
		if (out.isEmpty())
			return true;
		if (!ItemStack.isSameItem(out, itemstack))
			return false;
		return out.getCount() < out.getMaxStackSize();
	}

	private void processItem(int i) {
		ItemStack in = inv.getStackInSlot(i);
		ItemStack itemstack = ExtractorOres.getExtractionResult(in);
		Ore ore = ExtractorOres.getOre(in);
		int num = this.getSmeltNumber(i, ore);
		ItemStack out = inv.getStackInSlot(i+4);
		if (out.isEmpty())
			inv.setStackInSlot(i+4, itemstack.copyWithCount(itemstack.getCount()*num));
		else if (ItemStack.isSameItemSameComponents(out, itemstack))
			inv.setStackInSlot(i+4, out.copyWithCount(out.getCount()+num));

		if (i == 0 && !bedrock && drillTime > 0 && RotaryConfig.EXTRACTORMAINTAIN.get()) {
			drillTime--;
		}
		if (i == 3) {
			this.bonusItems(in);
		}

		InventoryHelper.decrStack(inv, i, 1);
		if (i == 1 || i == 2)
			tank.drain(125, IFluidHandler.FluidAction.EXECUTE);
	}

	private void bonusItems(ItemStack is) {
		Bonus e = Bonus.getBonusForIngredient(is);
		if (e != null && e.doBonus(rand)) {
			InventoryHelper.addOrSetStack(e.getBonusItem(), inv, 8);
		}
	}

	/** Original isItemValidForSlot. */
	public boolean isItemValidForSlot(int slot, ItemStack is) {
		if (slot > 3 && slot < 9)
			return false;
		if (slot == 9)
			return !bedrock && RotaryConfig.EXTRACTORMAINTAIN.get() && is.is(RotaryItems.part(CraftingPart.DRILL));
		if (slot == 0)
			return is.is(net.neoforged.neoforge.common.Tags.Items.ORES); //original ReikaBlockHelper.isOre (ore dictionary "ore*")
		return ExtractorOres.getInputStage(is) == slot;
	}

	@Override
	public int getRedstoneOverride() {
		int bits = 0b1111;
		for (int i = 0; i < 4; i++) {
			if (this.canProcess(i))
				bits &= ~(1 << i);
		}
		return bits;
	}

	/** Original DurationRegistry.EXTRACTOR.getOperationTime: max(1, base - scale*log2(omega+1)). */
	public int getOperationTime(int stage) {
		int[] d = DURATION[stage];
		return (int)Math.max(1, d[0]-d[1]*ReikaMathLibrary.logbase(Math.max(0, omega)+1, 2));
	}

	/** Original DurationFormula.getOverrun: above 2^(1+base/scale) rad/s, several operations per tick. */
	public int getNumberConsecutiveOperations(int stage) {
		int[] d = DURATION[stage];
		int threshold = (int)ReikaMathLibrary.intpow(2, 1+d[0]/d[1]);
		return 1+(omega >= threshold ? (int)(ReikaMathLibrary.logbase(omega, 2)-d[0]/d[1]) : 0);
	}

	public int getWater() {
		return tank.getFluidAmount();
	}

	public void addWater(int amt) {
		tank.fill(new FluidStack(Fluids.WATER, amt), IFluidHandler.FluidAction.EXECUTE);
	}

	/** Original canReceiveFrom: water from the sides (not top or bottom); nothing can be drained. */
	@Nullable
	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		if (side != null && side.getAxis() == Direction.Axis.Y)
			return null;
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
				int amt = tank.fill(resource, action);
				if (amt > 0 && action.execute())
					ExtractorBlockEntity.this.setChanged();
				return amt;
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

	/** Original ISidedInventory: insertion by isItemValidForSlot, extraction from the last output (7) and the bonus slot (8). */
	public IItemHandler getAutomationHandler() {
		return new IItemHandler() {
			@Override
			public int getSlots() {
				return 9;
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
				return slot == 7 || slot == 8 ? inv.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
			}

			@Override
			public int getSlotLimit(int slot) {
				return 64;
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return ExtractorBlockEntity.this.isItemValidForSlot(slot, stack);
			}
		};
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putIntArray("CookTime", extractorCookTime);
		NBT.putInt("drill", drillTime);
		NBT.putBoolean("bedrock", bedrock);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		int[] ct = NBT.getIntArray("CookTime");
		extractorCookTime = ct.length == 4 ? ct : new int[4];
		drillTime = NBT.getInt("drill");
		bedrock = NBT.getBoolean("bedrock");
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
		for (int i = 0; i < inv.getSlots(); i++) //original BlockTEBase.breakBlock: the whole inventory, including the drill slot
			net.minecraft.world.Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}

	@Override
	public void onEMP() {}
}
