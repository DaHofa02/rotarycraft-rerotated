/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.TileEntities.Processing.TileEntityCrystallizer - freezes the liquid in its 8000 mB tank into items
 * (RecipesCrystallizer: 1000 mB water -> ice, 200-999 mB water -> a snowball, 1000 mB lava -> stone, 1000 mB ethanol -> an ethanol
 * crystal) while it is at or below the liquid's freezing point, -273 + 0.9 * the liquid's temperature in K (water -3 deg C, ethanol
 * 33 deg C, lava 897 deg C). Needs 2048 W and 1024 rad/s; one item takes max(1, 400 - 24*log2(omega - 1024 + 1)) ticks. Every second
 * the temperature moves a quarter of the way (at least 1 deg C) towards the ambient temperature, lowered by 5 beside snow, 15 beside
 * water, 30 beside ice and 40 with dry ice in slot 1 (used up while it is more than 4 deg C warmer than that, or with a chance of 1/20).
 * The items go to slot 0, the only one that can be emptied. Not ported: the fan sound, Thermal Foundation cryotheum, other mods' molten
 * metals and liquids (ender, redstone, glowstone, coal).
 */
public class CrystallizerBlockEntity extends PowerReceiverBlockEntity implements TemperatureMachine, PipeConnector, net.minecraft.world.Clearable {

	public static final int CAPACITY = 8000;
	public static final int MAXTEMP = 1000;
	/** Original DurationRegistry.CRYSTALLIZER. */
	private static final int DURATION_BASE = 400, DURATION_SCALE = 24;

	private int temperature;
	private int tempTimer;
	public int freezeTick;

	private final FluidTank tank = new FluidTank(CAPACITY, fs -> isValidFluid(fs.getFluid()));

	private final ItemStackHandler inv = new ItemStackHandler(2) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return slot == 1 && is.is(RotaryItems.part(CraftingPart.DRYICE));
		}

		@Override
		protected void onContentsChanged(int slot) {
			CrystallizerBlockEntity.this.setChanged();
		}
	};

	public CrystallizerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.CRYSTALLIZER.get(), pos, state, 1, 1024, 2048);
	}

	@Override
	public String getName() {
		return "Crystallizer";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public FluidTank getTank() {
		return tank;
	}

	public static boolean isValidFluid(Fluid f) {
		return f.isSame(Fluids.WATER) || f.isSame(Fluids.LAVA) || f.isSame(RotaryFluids.ETHANOL.get());
	}

	/** Original RecipesCrystallizer.getFreezingResult: the recipe with the largest amount the tank holds. */
	public static ItemStack getFreezingResult(FluidStack fs) {
		if (fs.isEmpty())
			return ItemStack.EMPTY;
		if (fs.is(Fluids.WATER))
			return fs.getAmount() >= 1000 ? new ItemStack(Blocks.ICE) : fs.getAmount() >= 200 ? new ItemStack(Items.SNOWBALL) : ItemStack.EMPTY;
		if (fs.is(Fluids.LAVA))
			return fs.getAmount() >= 1000 ? new ItemStack(Blocks.STONE) : ItemStack.EMPTY;
		if (fs.getFluid().isSame(RotaryFluids.ETHANOL.get()))
			return fs.getAmount() >= 1000 ? new ItemStack(RotaryItems.ETHANOL.get()) : ItemStack.EMPTY;
		return ItemStack.EMPTY;
	}

	/** Original getRecipeConsumption. */
	public static int getRecipeConsumption(ItemStack out) {
		return out.is(Items.SNOWBALL) ? 200 : out.isEmpty() ? 0 : 1000;
	}

	public int getFreezingPoint() {
		return tank.isEmpty() ? 0 : getFreezingPoint(tank.getFluid());
	}

	public static int getFreezingPoint(FluidStack fs) {
		return -273+(int)(0.9*fs.getFluid().getFluidType().getTemperature(fs));
	}

	/** Original DurationRegistry.CRYSTALLIZER.getOperationTime(omega - MINSPEED). */
	public int getOperationTime() {
		return (int)Math.max(1, DURATION_BASE-DURATION_SCALE*ReikaMathLibrary.logbase(Math.max(0, omega-MINSPEED)+1, 2));
	}

	/** Original DurationFormula.getOverrun: above 2^(1+400/24) rad/s over the minimum, several operations per tick. */
	public int getNumberConsecutiveOperations() {
		int w = Math.max(0, omega-MINSPEED);
		int threshold = (int)ReikaMathLibrary.intpow(2, 1+DURATION_BASE/DURATION_SCALE);
		return 1+(w >= threshold ? (int)(ReikaMathLibrary.logbase(w, 2)-DURATION_BASE/DURATION_SCALE) : 0);
	}

	public int getProgressScaled(int s) {
		return s*freezeTick/this.getOperationTime();
	}

	public int getLiquidScaled(int s) {
		return s*tank.getFluidAmount()/CAPACITY;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSidesDefault(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		this.readPower();
		tempTimer++;
		if (tempTimer >= 20) {
			tempTimer = 0;
			this.updateTemperature(world, pos);
		}
		int n = this.getNumberConsecutiveOperations();
		for (int i = 0; i < n; i++)
			this.doOperation(n > 1);
	}

	private void doOperation(boolean multiple) {
		ItemStack toMake = getFreezingResult(tank.getFluid());
		if (!this.canOperate(toMake)) {
			freezeTick = 0;
			return;
		}
		freezeTick++;
		if (multiple || freezeTick >= this.getOperationTime()) {
			freezeTick = 0;
			ItemStack out = inv.getStackInSlot(0);
			inv.setStackInSlot(0, out.isEmpty() ? toMake : out.copyWithCount(out.getCount()+toMake.getCount()));
			tank.drain(getRecipeConsumption(toMake), IFluidHandler.FluidAction.EXECUTE);
			this.syncAllData();
		}
		this.setChanged();
	}

	private boolean canOperate(ItemStack toMake) {
		if (toMake.isEmpty() || power < MINPOWER || omega < MINSPEED)
			return false;
		if (temperature > this.getFreezingPoint())
			return false;
		ItemStack out = inv.getStackInSlot(0);
		return out.isEmpty() || ItemStack.isSameItemSameComponents(out, toMake) && out.getCount()+toMake.getCount() <= out.getMaxStackSize();
	}

	private static boolean isNextTo(Level world, BlockPos pos, java.util.function.Predicate<BlockState> test) {
		for (Direction d : Direction.values())
			if (test.test(world.getBlockState(pos.relative(d))))
				return true;
		return false;
	}

	private void updateTemperature(Level world, BlockPos pos) {
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		if (isNextTo(world, pos, s -> s.is(Blocks.SNOW_BLOCK)))
			Tamb -= 5;
		if (isNextTo(world, pos, s -> s.getFluidState().is(FluidTags.WATER)))
			Tamb -= 15;
		if (isNextTo(world, pos, s -> s.is(Blocks.ICE)))
			Tamb -= 30;
		if (inv.getStackInSlot(1).is(RotaryItems.part(CraftingPart.DRYICE))) {
			Tamb -= 40;
			if (temperature > Tamb+4 || rand.nextInt(20) == 0)
				inv.extractItem(1, 1, false);
		}
		int dT = Tamb-temperature;
		int old = temperature;
		temperature += Math.abs(dT) < 4 ? (int)Math.signum(dT) : dT/4;
		if (old != temperature)
			this.syncAllData();
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		if (level == null)
			phi = 0;
		else if (power >= MINPOWER && omega >= MINSPEED)
			phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
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

	@Override
	public boolean canConnectToPipe(PipeType p) {
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
				return isValidFluid(stack.getFluid());
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return CrystallizerBlockEntity.this.fill(side, resource, action);
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

	/** Dry ice goes into slot 1 from any side; only slot 0 can be emptied (original canExtractItem). */
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
				return slot == 0 ? inv.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
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
		NBT.putInt("temp", temperature);
		NBT.putInt("freeze", freezeTick);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		temperature = NBT.getInt("temp");
		freezeTick = NBT.getInt("freeze");
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
