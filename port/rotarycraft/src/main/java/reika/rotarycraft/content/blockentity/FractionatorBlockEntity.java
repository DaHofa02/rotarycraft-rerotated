/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.data.InventoryHelper;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.lib.world.AmbientPressure;
import reika.rotarycraft.registry.DifficultyEffects;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.TileEntities.Production.TileEntityFractionator. Turns ethanol into jet fuel with the six ingredients
 * (blaze powder, coal dust, magma cream, pink dye, netherrack dust, tar; one of each in slots 0-5) and a ghast tear (slot 6, not used
 * up). Powered from below, min. 8192 rad/s and 65536 W; time max(1, 800 - 40*log2(omega+1)). The internal pressure rises with the
 * 20-tick average torque (+1.8*sqrt per second) and falls towards the ambient pressure; the yield follows the original curve
 * (0.1 at 180 kPa, 1 at 720 kPa, 2.5 at 1000 kPa). Per operation (difficulty CONSUMEFRAC/PRODUCEFRAC): 250 mB ethanol and on
 * average 1.5 ingredients (weighted) are used, yield * 1000-2200 mB jet fuel made (medium). Ethanol enters from the sides,
 * jet fuel leaves at the top (fuel lines and bedrock pipes).
 */
public class FractionatorBlockEntity extends PowerReceiverBlockEntity implements PipeConnector, net.minecraft.world.Clearable {

	public static final int CAPACITY = 240000;
	public static final int INPUT_CAPACITY = 16000;
	public static int MAXPRESSURE = 1000; //10atm
	/** Original DurationRegistry.FRACTIONATOR. */
	private static final int DURATION_BASE = 800, DURATION_SCALE = 40;

	/** Original ingredients map with the weights used to pick what is consumed. */
	private static final List<Ingredient> INGREDIENTS = new ArrayList<>();

	private record Ingredient(java.util.function.Supplier<Item> item, float weight) {}

	static {
		INGREDIENTS.add(new Ingredient(() -> Items.BLAZE_POWDER, 1.5F));
		INGREDIENTS.add(new Ingredient(() -> RotaryItems.part(CraftingPart.COALDUST), 1F));
		INGREDIENTS.add(new Ingredient(() -> Items.MAGMA_CREAM, 0.75F));
		INGREDIENTS.add(new Ingredient(() -> Items.PINK_DYE, 0.5F));
		INGREDIENTS.add(new Ingredient(() -> RotaryItems.part(CraftingPart.NETHERRACKDUST), 2F));
		INGREDIENTS.add(new Ingredient(() -> RotaryItems.part(CraftingPart.TAR), 1.5F));
	}

	/** Original yield Interpolation (linear): pressure in kPa -> yield factor. */
	private static final double[][] YIELD = {{0, 0.01}, {100, 0.05}, {180, 0.1}, {500, 0.4}, {720, 1}, {850, 1.5}, {1000, 2.5}};

	public int mixTime;
	private int pressure;
	public boolean idle = false;

	/** Original MovingAverage(20), starting with zeros. */
	private final ArrayDeque<Double> torqueInput = new ArrayDeque<>();

	private final FluidTank input = new FluidTank(INPUT_CAPACITY, fs -> fs.is(RotaryFluids.ETHANOL.get()));
	private final FluidTank output = new FluidTank(CAPACITY, fs -> fs.is(RotaryFluids.JET_FUEL.get()));

	private final ItemStackHandler inv = new ItemStackHandler(INGREDIENTS.size()+2) {
		@Override
		protected void onContentsChanged(int slot) {
			FractionatorBlockEntity.this.setChanged();
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return FractionatorBlockEntity.this.isItemValidForSlot(slot, stack);
		}
	};

	public FractionatorBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.FRACTIONATOR.get(), pos, state, 1, 8192, 65536);
		for (int i = 0; i < 20; i++)
			torqueInput.add(0D);
	}

	@Override
	public String getName() {
		return "Fractionation Unit";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	private static int ingredientIndex(ItemStack is) {
		for (int i = 0; i < INGREDIENTS.size(); i++)
			if (is.is(INGREDIENTS.get(i).item().get()))
				return i;
		return -1;
	}

	public static boolean isJetFuelIngredient(ItemStack is) {
		return ingredientIndex(is) >= 0;
	}

	public void testIdle() {
		idle = !this.hasAllIngredients();
	}

	public int getFuelScaled(int par1) {
		return (output.getFluidAmount()*par1)/CAPACITY;
	}

	public int getEthanolScaled(int par1) {
		return (input.getFluidAmount()*par1)/INPUT_CAPACITY;
	}

	public int getPressureScaled(int par1) {
		return (pressure*par1)/MAXPRESSURE;
	}

	private double getAverageTorque() {
		double sum = 0;
		for (double d : torqueInput)
			sum += d;
		return sum/torqueInput.size();
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		read = Direction.DOWN; //original getPowerBelow
		if (world.isClientSide)
			return;
		this.readPower();
		power = (long)omega * (long)torque;
		torqueInput.add((double)torque);
		torqueInput.remove();
		if (this.getTicksExisted()%20 == 0)
			this.updatePressure(world, pos);
		this.testIdle();
		if (power < MINPOWER || omega < MINSPEED) {
			mixTime = 0;
			return;
		}
		int n = this.getNumberConsecutiveOperations();
		for (int i = 0; i < n; i++)
			this.doOperation(n > 1);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	public void updatePressure(Level world, BlockPos pos) {
		int local = pressure;
		double Pamb = AmbientPressure.getAmbientPressureAt(world, pos);
		if (Pamb > 101.3)
			Pamb = 101.3+0.02*(Pamb-101.3);
		int dp = local-(int)Pamb;
		int sub = (int)(Math.signum(dp)*Math.max(1, Math.abs(dp/16)));
		int avg = (int)this.getAverageTorque();
		if (avg <= 0)
			sub *= 8;
		local -= sub;
		if (avg > 0)
			local += 1.8*Math.sqrt(avg);
		if (local > MAXPRESSURE)
			local = MAXPRESSURE; //original overpressure() does nothing
		//Gain pressure slowly, but lose it quickly
		if (pressure < local)
			pressure += Math.max(1, Math.min(6-13+rand.nextInt(27), (local-pressure)/4));
		else
			pressure = local;
	}

	private void doOperation(boolean multiple) {
		if (this.process()) {
			mixTime++;
			if (multiple || mixTime >= this.getOperationTime()) {
				mixTime = 0;
				this.make();
			}
		}
		else {
			mixTime = 0;
		}
	}

	private void make() {
		float consume = INGREDIENTS.size()*DifficultyEffects.CONSUMEFRAC.getChance();
		List<Integer> slots = new ArrayList<>();
		List<Float> weights = new ArrayList<>();
		for (int i = 0; i < INGREDIENTS.size(); i++) {
			slots.add(i);
			weights.add(INGREDIENTS.get(ingredientIndex(inv.getStackInSlot(i))).weight());
		}
		while (consume > 0 && !slots.isEmpty()) {
			float total = 0;
			for (float w : weights)
				total += w;
			double d = rand.nextDouble()*total;
			int pick = weights.size()-1;
			double acc = 0;
			for (int k = 0; k < weights.size(); k++) {
				acc += weights.get(k);
				if (d < acc) {
					pick = k;
					break;
				}
			}
			int slot = slots.get(pick);
			if (consume >= 1 || rand.nextDouble() < consume)
				InventoryHelper.decrStack(inv, slot, 1);
			slots.remove(pick);
			weights.remove(pick);
			consume -= 1;
		}
		float amt = 1000*DifficultyEffects.CONSUMEFRAC.getChance();
		input.drain((int)amt, IFluidHandler.FluidAction.EXECUTE);
		output.fill(new FluidStack(RotaryFluids.JET_FUEL.get(), (int)(this.getYieldRatio()*DifficultyEffects.PRODUCEFRAC.getInt())), IFluidHandler.FluidAction.EXECUTE);
		this.setChanged();
	}

	public float getYieldRatio() {
		return (float)yieldAt(pressure);
	}

	static double yieldAt(double key) {
		if (key <= YIELD[0][0])
			return YIELD[0][1];
		for (int i = 1; i < YIELD.length; i++) {
			if (key <= YIELD[i][0]) {
				double x1 = YIELD[i-1][0], x2 = YIELD[i][0], d1 = YIELD[i-1][1], d2 = YIELD[i][1];
				return d1+(key-x1)/(x2-x1)*(d2-d1);
			}
		}
		return YIELD[YIELD.length-1][1];
	}

	private boolean process() {
		if (output.getFluidAmount()+DifficultyEffects.PRODUCEFRAC.getMaxAmount() >= CAPACITY)
			return false;
		if (!inv.getStackInSlot(INGREDIENTS.size()).is(Items.GHAST_TEAR)) //need a ghast tear as fuel solvent
			return false;
		return this.hasAllIngredients();
	}

	private boolean hasAllIngredients() {
		Set<Integer> check = new HashSet<>();
		for (int i = 0; i < INGREDIENTS.size(); i++)
			check.add(i);
		for (int i = 0; i < INGREDIENTS.size(); i++) {
			ItemStack is = inv.getStackInSlot(i);
			if (is.isEmpty())
				return false;
			int k = ingredientIndex(is);
			if (!check.contains(k))
				return false;
			check.remove(k);
		}
		return input.getFluidAmount() >= 1000*DifficultyEffects.CONSUMEFRAC.getChance();
	}

	public boolean isItemValidForSlot(int slot, ItemStack is) {
		if (slot == INGREDIENTS.size()+1)
			return is.is(Items.BUCKET);
		if (slot == INGREDIENTS.size())
			return is.is(Items.GHAST_TEAR);
		if (!isJetFuelIngredient(is))
			return false;
		//original: an ingredient goes into the slot that already holds it, or any slot if none does
		for (int i = 0; i < INGREDIENTS.size(); i++)
			if (i != slot && ItemStack.isSameItem(inv.getStackInSlot(i), is))
				return false;
		return true;
	}

	@Override
	public int getRedstoneOverride() {
		return (int)(this.getYieldRatio()/YIELD[YIELD.length-1][1]*15);
	}

	public int getOperationTime() {
		return (int)Math.max(1, DURATION_BASE-DURATION_SCALE*ReikaMathLibrary.logbase(Math.max(0, omega)+1, 2));
	}

	public int getNumberConsecutiveOperations() {
		int threshold = (int)ReikaMathLibrary.intpow(2, 1+DURATION_BASE/DURATION_SCALE);
		return 1+(omega >= threshold ? (int)(ReikaMathLibrary.logbase(omega, 2)-DURATION_BASE/DURATION_SCALE) : 0);
	}

	public int getPressure() {
		return pressure;
	}

	public void setPressure(int p) {
		pressure = p;
	}

	public int getInputLevel() {
		return input.getFluidAmount();
	}

	public int getFuelLevel() {
		return output.getFluidAmount();
	}

	public void addEthanol(int amt) {
		input.fill(new FluidStack(RotaryFluids.ETHANOL.get(), amt), IFluidHandler.FluidAction.EXECUTE);
	}

	public void removeFuel(int amt) {
		output.drain(amt, IFluidHandler.FluidAction.EXECUTE);
	}

	/** Original canReceiveFrom: ethanol from the four sides. */
	public boolean canReceiveFrom(@Nullable Direction from) {
		return from == null || from.getAxis() != Direction.Axis.Y;
	}

	/** Original canOutputTo: jet fuel to the top only. */
	public boolean canOutputTo(@Nullable Direction to) {
		return to == Direction.UP;
	}

	@Override
	public boolean canConnectToPipe(PipeType m) {
		return m == PipeType.FUEL_LINE || m == PipeType.BEDROCK_PIPE;
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return (this.canReceiveFrom(side) && this.canConnectToPipe(p)) || (this.canOutputTo(side) && this.canConnectToPipe(p)) && this.canConnectToPipe(p);
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		if (this.canOutputTo(side))
			return Flow.OUTPUT;
		else if (this.canReceiveFrom(side))
			return Flow.INPUT;
		return Flow.NONE;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (!this.canReceiveFrom(from) || !resource.is(RotaryFluids.ETHANOL.get()))
			return 0;
		int amt = input.fill(resource, action);
		if (amt > 0 && action.execute())
			this.setChanged();
		return amt;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (!this.canOutputTo(from))
			return FluidStack.EMPTY;
		FluidStack fs = output.drain(maxDrain, action);
		if (!fs.isEmpty() && action.execute())
			this.setChanged();
		return fs;
	}

	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 2;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return t == 0 ? input.getFluid() : output.getFluid();
			}

			@Override
			public int getTankCapacity(int t) {
				return t == 0 ? INPUT_CAPACITY : CAPACITY;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return t == 0 && stack.is(RotaryFluids.ETHANOL.get());
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return FractionatorBlockEntity.this.fill(side, resource, action);
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				return resource.is(RotaryFluids.JET_FUEL.get()) ? FractionatorBlockEntity.this.drain(side, resource.getAmount(), action) : FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int maxDrain, FluidAction action) {
				return FractionatorBlockEntity.this.drain(side, maxDrain, action);
			}
		};
	}

	/** Original ISidedInventory: insertion by isItemValidForSlot, no extraction (canExtractItem false). */
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
				return FractionatorBlockEntity.this.isItemValidForSlot(slot, stack);
			}
		};
	}

	@Override
	public void clearContent() {
		for (int i = 0; i < inv.getSlots(); i++)
			inv.setStackInSlot(i, ItemStack.EMPTY);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("mix", mixTime);
		NBT.putInt("press", pressure);
		NBT.put("in", input.writeToNBT(registries, new CompoundTag()));
		NBT.put("out", output.writeToNBT(registries, new CompoundTag()));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		mixTime = NBT.getInt("mix");
		pressure = NBT.getInt("press");
		input.readFromNBT(registries, NBT.getCompound("in"));
		output.readFromNBT(registries, NBT.getCompound("out"));
	}

	@Override
	protected void writeAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeAdditional(NBT, registries);
		NBT.put("inv", inv.serializeNBT(registries));
		double[] t = new double[torqueInput.size()];
		int i = 0;
		for (double d : torqueInput)
			t[i++] = d;
		CompoundTag tag = new CompoundTag();
		for (int k = 0; k < t.length; k++)
			tag.putDouble("v"+k, t[k]);
		NBT.put("torquevals", tag);
	}

	@Override
	protected void readAdditional(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readAdditional(NBT, registries);
		inv.deserializeNBT(registries, NBT.getCompound("inv"));
		if (NBT.contains("torquevals")) {
			CompoundTag tag = NBT.getCompound("torquevals");
			torqueInput.clear();
			for (int k = 0; k < 20; k++)
				torqueInput.add(tag.getDouble("v"+k));
		}
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < inv.getSlots(); i++)
			net.minecraft.world.Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}
}
