/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.block.FermenterBlock;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.data.InventoryHelper;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryDataMaps;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.TileEntities.Production.TileEntityFermenter. Without a redstone signal it breeds yeast from sugar and
 * dirt; with one it ferments yeast and plant matter into sludge (amount = the plant's mulch value), 50 mB water per product. Needs
 * 32 rad/s and 1024 W; base time max(1, 480 - 35*log2(omega+1)), divided by a temperature factor (best 25 °C for yeast, 35 °C for
 * sludge, nothing useful below 20 or above 40 °C; at 60 °C the yeast dies). The temperature follows the surroundings every second
 * (water -5, ice -15, fire +50, lava +200 next to it). Slots: 0 sugar/yeast, 1 dirt/plant, 2 output.
 * Not ported: Forestry bee yeast (option "Use Forestry Bees To Produce Yeast"), heat-sink fins, other mods' plant materials.
 */
public class FermenterBlockEntity extends PowerReceiverBlockEntity implements TemperatureMachine, PipeConnector, net.minecraft.world.Clearable {

	public static final int MINUSEFULTEMP = 20;
	public static final int OPTMULTIPLYTEMP = 25;
	public static final int MAXUSEFULTEMP = 40;
	public static final int OPTFERMENTTEMP = 35;
	public static final int MAXTEMP = 60;

	public static final int CAPACITY = 4000;
	public static final int CONSUME_WATER = 50;

	/** Original DurationRegistry.FERMENTER. */
	private static final int DURATION_BASE = 480, DURATION_SCALE = 35;

	public int fermenterCookTime = 0;
	public int temperature;
	public boolean idle = false;
	private int temperaturetick = 0;

	private final FluidTank tank = new FluidTank(CAPACITY, fs -> fs.is(Fluids.WATER));

	private final ItemStackHandler inv = new ItemStackHandler(3) {
		@Override
		protected void onContentsChanged(int slot) {
			FermenterBlockEntity.this.setChanged();
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return FermenterBlockEntity.this.isItemValidForSlot(slot, stack);
		}
	};

	public FermenterBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.FERMENTER.get(), pos, state, 1, 32, 1024);
	}

	@Override
	public String getName() {
		return "Fermenter";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	private static boolean isDirt(ItemStack is) {
		//1.7.10 Blocks.dirt with any metadata: dirt, coarse dirt, podzol
		return is.is(Items.DIRT) || is.is(Items.COARSE_DIRT) || is.is(Items.PODZOL);
	}

	/** Original getRecipe: the product for the current inputs, or empty. */
	private ItemStack getRecipe() {
		ItemStack a = inv.getStackInSlot(0), b = inv.getStackInSlot(1);
		if (a.isEmpty() || b.isEmpty())
			return ItemStack.EMPTY;
		if (a.is(Items.SUGAR)) {
			if (this.hasWater() && isDirt(b))
				return new ItemStack(RotaryItems.YEAST.get());
		}
		if (a.is(RotaryItems.YEAST.get())) {
			int v = RotaryDataMaps.getPlantValue(b);
			if (v > 0 && this.hasWater())
				return new ItemStack(RotaryItems.part(CraftingPart.SLUDGE), v);
		}
		return ItemStack.EMPTY;
	}

	private boolean hasWater() {
		return !tank.isEmpty();
	}

	private float getFermentRate() {
		ItemStack r = this.getRecipe();
		if (r.isEmpty())
			return -1F;
		boolean fermenting = !r.is(RotaryItems.YEAST.get());
		if (temperature < MINUSEFULTEMP)
			return 1F/(MINUSEFULTEMP-temperature);
		if (temperature > MAXUSEFULTEMP)
			return 1F/(temperature-MAXUSEFULTEMP);
		float Tdiff = temperature-OPTMULTIPLYTEMP;
		if (fermenting)
			Tdiff = temperature-OPTFERMENTTEMP;
		if (Tdiff < 0)
			Tdiff = -Tdiff;
		return (float)Math.pow(1-Tdiff/16F, 0.2);
	}

	public void testIdle() {
		idle = this.getRecipe().isEmpty();
	}

	public boolean hasRedstoneSignal() {
		return level != null && level.hasNeighborSignal(worldPosition);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSidesDefault(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		temperaturetick++;
		tickcount++;
		this.readPower();
		if (temperaturetick >= 20) {
			temperaturetick = 0;
			this.updateTemperature(world, pos);
		}
		boolean active = power >= MINPOWER && omega >= MINSPEED && this.canMake();
		if (state.hasProperty(FermenterBlock.ACTIVE) && state.getValue(FermenterBlock.ACTIVE) != active)
			world.setBlock(pos, state.setValue(FermenterBlock.ACTIVE, active), 3);
		if (power < MINPOWER || omega < MINSPEED)
			return;
		int n = this.getNumberConsecutiveOperations();
		for (int i = 0; i < n; i++)
			this.doOperation(n > 1);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	private void doOperation(boolean multiple) {
		ItemStack product = this.getRecipe();
		if (tickcount >= 2+rand.nextInt(18)) {
			this.testYeastKill();
			tickcount = 0;
		}
		if (product.isEmpty()) {
			idle = true;
			fermenterCookTime = 0;
			return;
		}
		ItemStack out = inv.getStackInSlot(2);
		if (!out.isEmpty() && !out.is(product.getItem())) {
			fermenterCookTime = 0;
			return;
		}
		idle = false;
		if (!out.isEmpty() && out.getCount()+product.getCount() > out.getMaxStackSize()) {
			fermenterCookTime = 0;
			return;
		}
		fermenterCookTime++;
		if (multiple || fermenterCookTime >= this.getOperationTime()) {
			this.make(product);
			fermenterCookTime = 0;
		}
	}

	private boolean canMake() {
		ItemStack product = this.getRecipe();
		if (product.isEmpty())
			return false;
		ItemStack out = inv.getStackInSlot(2);
		if (!out.isEmpty()) {
			if (!out.is(product.getItem()))
				return false;
			if (out.getCount()+product.getCount() > out.getMaxStackSize())
				return false;
		}
		return true;
	}

	private void make(ItemStack product) {
		ItemStack out = inv.getStackInSlot(2);
		if (product.is(RotaryItems.YEAST.get())) {
			if (out.isEmpty())
				inv.setStackInSlot(2, new ItemStack(RotaryItems.YEAST.get()));
			else if (out.is(RotaryItems.YEAST.get())) {
				if (out.getCount() < out.getMaxStackSize())
					inv.setStackInSlot(2, out.copyWithCount(out.getCount()+1));
				else
					return;
			}
			else {
				fermenterCookTime = 0;
				return;
			}
			InventoryHelper.decrStack(inv, 0, 1);
			if (rand.nextInt(4) == 0)
				InventoryHelper.decrStack(inv, 1, 1);
		}
		if (product.is(RotaryItems.part(CraftingPart.SLUDGE))) {
			if (out.isEmpty())
				inv.setStackInSlot(2, product.copy());
			else if (out.is(product.getItem())) {
				if (out.getCount() < out.getMaxStackSize())
					inv.setStackInSlot(2, out.copyWithCount(out.getCount()+product.getCount()));
				else
					return;
			}
			else {
				fermenterCookTime = 0;
				return;
			}
			InventoryHelper.decrStack(inv, 1, 1);
			if (rand.nextInt(2) == 0)
				InventoryHelper.decrStack(inv, 0, 1);
		}
		this.setChanged();
		tank.drain(CONSUME_WATER, IFluidHandler.FluidAction.EXECUTE);
	}

	private static boolean hasAdjacent(Level world, BlockPos pos, java.util.function.Predicate<BlockState> test) {
		for (Direction d : Direction.values())
			if (test.test(world.getBlockState(pos.relative(d))))
				return true;
		return false;
	}

	/** Original updateTemperature, once per second. */
	public void updateTemperature(Level world, BlockPos pos) {
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		if (hasAdjacent(world, pos, s -> s.getFluidState().is(FluidTags.WATER) && s.getFluidState().isSource()))
			Tamb -= 5;
		if (hasAdjacent(world, pos, s -> s.is(Blocks.ICE)))
			Tamb -= 15;
		if (hasAdjacent(world, pos, s -> s.is(Blocks.FIRE)))
			Tamb += 50;
		if (hasAdjacent(world, pos, s -> s.getFluidState().is(FluidTags.LAVA) && s.getFluidState().isSource()))
			Tamb += 200;
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
	}

	/** Original testYeastKill: at 60 °C one yeast dies. */
	public void testYeastKill() {
		if (temperature < MAXTEMP)
			return;
		for (int i = 0; i < inv.getSlots(); i++) {
			if (inv.getStackInSlot(i).is(RotaryItems.YEAST.get())) {
				InventoryHelper.decrStack(inv, i, 1);
				level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 0.8F);
				return;
			}
		}
	}

	public int getCookProgressScaled(int par1) {
		return (fermenterCookTime * par1)/2 / this.getOperationTime();
	}

	public int getTemperatureScaled(int par1) {
		return (temperature * par1) / MAXTEMP;
	}

	/** Original: DurationRegistry.FERMENTER time divided by the temperature factor. */
	public int getOperationTime() {
		int base = (int)Math.max(1, DURATION_BASE-DURATION_SCALE*ReikaMathLibrary.logbase(Math.max(0, omega)+1, 2));
		return Math.max(1, (int)(base/this.getFermentRate()));
	}

	public int getNumberConsecutiveOperations() {
		int threshold = (int)ReikaMathLibrary.intpow(2, 1+DURATION_BASE/DURATION_SCALE);
		int ops = 1+(omega >= threshold ? (int)(ReikaMathLibrary.logbase(omega, 2)-DURATION_BASE/DURATION_SCALE) : 0);
		return (int)Math.max(1, this.getFermentRate()*ops);
	}

	/** Original isItemValidForSlot: the inputs of the current mode (redstone signal = sludge mode). */
	public boolean isItemValidForSlot(int i, ItemStack is) {
		if (i >= 2)
			return false;
		if (this.hasRedstoneSignal()) {
			return switch (i) {
				case 0 -> is.is(RotaryItems.YEAST.get());
				case 1 -> RotaryDataMaps.getPlantValue(is) > 0;
				default -> false;
			};
		}
		return switch (i) {
			case 0 -> is.is(Items.SUGAR);
			case 1 -> isDirt(is);
			default -> false;
		};
	}

	@Override
	public int getRedstoneOverride() {
		return this.canMake() ? 0 : 15;
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


	/** Original canBeCooledWithFins. */
	@Override
	public boolean canBeCooledWithFins() {
		return true;
	}

	@Override
	public int getMaxTemperature() {
		return MAXTEMP;
	}

	@Override
	public boolean allowExternalHeating() {
		return true;
	}

	public int getWater() {
		return tank.getFluidAmount();
	}

	public void addWater(int amt) {
		tank.fill(new FluidStack(Fluids.WATER, amt), IFluidHandler.FluidAction.EXECUTE);
	}

	@Override
	public boolean canConnectToPipe(PipeType m) {
		return true;
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return true;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return Flow.INPUT;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (!resource.is(Fluids.WATER))
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
				return FermenterBlockEntity.this.fill(side, resource, action);
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

	/** Original ISidedInventory: insertion by isItemValidForSlot, extraction from the output only. */
	public IItemHandler getAutomationHandler() {
		return new IItemHandler() {
			@Override
			public int getSlots() {
				return 3;
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
				return slot == 2 ? inv.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
			}

			@Override
			public int getSlotLimit(int slot) {
				return 64;
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return FermenterBlockEntity.this.isItemValidForSlot(slot, stack);
			}
		};
	}

	/** Original: the front faces the player (4-sided metadata), kept in the blockstate for the model. */
	@Override
	public void setBlockMetadata(int meta) {
		super.setBlockMetadata(meta);
		if (level != null && !level.isClientSide) {
			BlockState bs = this.getBlockState();
			if (bs.hasProperty(FermenterBlock.FACING))
				level.setBlock(worldPosition, bs.setValue(FermenterBlock.FACING, reika.rotarycraft.content.block.BlastFurnaceBlock.frontFromMeta(meta)), 3);
		}
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
		NBT.putShort("CookTime", (short)fermenterCookTime);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		temperature = NBT.getInt("temperature");
		fermenterCookTime = NBT.getShort("CookTime");
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
		for (int i = 0; i < inv.getSlots(); i++)
			net.minecraft.world.Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}
}
