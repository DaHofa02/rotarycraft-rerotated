/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.blockentity;

import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
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

import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;
import reika.reactorcraft.registry.ReactorItems;
import reika.rotarycraft.content.blockentity.FrictionHeatable;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.world.AmbientTemperature;

/**
 * Original: Reika.ReactorCraft.TileEntities.Processing.TileEntitySynthesizer - heats up from its surroundings (or a friction heater) and,
 * above the recipe temperature, turns its input fluid into the product once per max(5, base - curve*(T - Tmin)/100) ticks:
 * <ul><li>ammonia: 250 mB water + one quicklime + one ammonium chloride -> 1000 mB ammonia, at 220 C (50 ticks);</li>
 * <li>thorium fuel preheat: 50 mB molten thorium fuel -> 50 mB preheated fuel, at 350 C (100 ticks, faster when hotter).</li></ul>
 * Temperature once a second towards the ambient (halved next to water, quartered next to ice which melts, +200 next to fire, +600 next
 * to lava), max. 1000 C; above 100 C it melts snow and ice next to it. Two 24000 mB tanks: pipes fill from the sides, take from top and
 * bottom (also fuel lines). A water bucket in slot 0 (or used on the block) adds 1000 mB.
 */
public class SynthesizerBlockEntity extends RotaryCraftBlockEntity implements FrictionHeatable, PipeConnector, net.minecraft.world.Clearable {

	public static final int AMMONIATEMP = 220;
	public static final int MAXTEMP = 1000;
	public static final int CAPACITY = 24000;

	public enum Synthesis {
		AMMONIA(() -> Fluids.WATER, () -> ReactorFluids.get("ammonia"), 250, 1000, AMMONIATEMP, 50, 0,
				is -> is.is(tag("dusts/quicklime")) || is.is(ReactorItems.raw(ReactorItems.Raw.LIME).get()),
				is -> is.is(tag("dusts/ammonium")) || is.is(ReactorItems.raw(ReactorItems.Raw.AMMONIUM).get())),
		HOTLIFBE(() -> ReactorFluids.get("lifbe_fuel"), () -> ReactorFluids.get("lifbe_fuel_preheat"), 50, 50, 350, 100, 5, null, null);

		private final Supplier<Fluid> input, output;
		public final int fluidConsumed, fluidProduced, minTemp, baseDuration, temperatureSpeedCurve;
		@Nullable
		private final Predicate<ItemStack> itemA, itemB;

		Synthesis(Supplier<Fluid> in, Supplier<Fluid> out, int amtin, int amtout, int temp, int time, int tc, @Nullable Predicate<ItemStack> a, @Nullable Predicate<ItemStack> b) {
			input = in;
			output = out;
			fluidConsumed = amtin;
			fluidProduced = amtout;
			minTemp = temp;
			baseDuration = time;
			temperatureSpeedCurve = tc;
			itemA = a;
			itemB = b;
		}

		public Fluid inputFluid() {
			return input.get();
		}

		public Fluid outputFluid() {
			return output.get();
		}

		public int getDuration(int temperature) {
			return Math.max(5, baseDuration-temperatureSpeedCurve*(temperature-minTemp)/100);
		}

		/** Original ReikaItemHelper.matchStacks(slot, ingredient): an empty slot matches a missing ingredient only. */
		private static boolean matches(@Nullable Predicate<ItemStack> p, ItemStack is) {
			return p == null ? is.isEmpty() : !is.isEmpty() && p.test(is);
		}

		@Nullable
		static Synthesis byInput(Fluid f) {
			for (Synthesis s : values())
				if (s.inputFluid().isSame(f))
					return s;
			return null;
		}
	}

	private static TagKey<net.minecraft.world.item.Item> tag(String path) {
		return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
	}

	private final FluidTank tank = new FluidTank(CAPACITY);
	private final FluidTank water = new FluidTank(CAPACITY);
	public int timer;
	private int temperature;
	private int tempTimer;
	@Nullable
	private Synthesis recipe;

	private final ItemStackHandler inv = new ItemStackHandler(3) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			if (slot == 0)
				return is.is(Items.WATER_BUCKET);
			for (Synthesis s : Synthesis.values()) {
				if (s.itemA != null && s.itemA.test(is))
					return slot == 1;
				if (s.itemB != null && s.itemB.test(is))
					return slot == 2;
			}
			return false;
		}

		@Override
		protected void onContentsChanged(int slot) {
			SynthesizerBlockEntity.this.setChanged();
		}
	};

	public SynthesizerBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.SYNTHESIZER.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Synthesizer";
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (world.isClientSide)
			return;
		this.getWaterBuckets();
		recipe = this.getRecipe();
		if (recipe != null && water.getFluidAmount() >= recipe.fluidConsumed && temperature >= recipe.minTemp && this.canTakeIn(recipe.outputFluid(), recipe.fluidProduced)) {
			timer++;
			if (timer >= recipe.getDuration(temperature)) {
				timer = 0;
				this.make();
			}
		}
		else {
			timer = 0;
		}
		tempTimer++;
		if (tempTimer >= 20) {
			tempTimer = 0;
			this.updateTemperature(world, pos);
		}
	}

	private boolean canTakeIn(Fluid f, int amt) {
		return (tank.isEmpty() || tank.getFluid().getFluid().isSame(f)) && tank.getFluidAmount()+amt <= CAPACITY;
	}

	@Nullable
	private Synthesis getRecipe() {
		if (water.isEmpty())
			return null;
		Synthesis s = Synthesis.byInput(water.getFluid().getFluid());
		if (s == null)
			return null;
		if (!Synthesis.matches(s.itemA, inv.getStackInSlot(1)) || !Synthesis.matches(s.itemB, inv.getStackInSlot(2)))
			return null;
		return s;
	}

	private void make() {
		if (recipe.itemA != null)
			inv.extractItem(1, 1, false);
		if (recipe.itemB != null)
			inv.extractItem(2, 1, false);
		water.drain(recipe.fluidConsumed, IFluidHandler.FluidAction.EXECUTE);
		tank.fill(new FluidStack(recipe.outputFluid(), recipe.fluidProduced), IFluidHandler.FluidAction.EXECUTE);
		this.setChanged();
	}

	/** Original updateTemperature. */
	private void updateTemperature(Level world, BlockPos pos) {
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		BlockPos waterside = null, iceside = null, fireside = null, lavaside = null;
		for (Direction d : Direction.values()) {
			BlockPos p = pos.relative(d);
			BlockState s = world.getBlockState(p);
			if (waterside == null && s.getFluidState().is(FluidTags.WATER))
				waterside = p;
			if (iceside == null && s.is(Blocks.ICE))
				iceside = p;
			if (fireside == null && s.is(Blocks.FIRE))
				fireside = p;
			if (lavaside == null && s.getFluidState().is(FluidTags.LAVA))
				lavaside = p;
		}
		if (waterside != null)
			Tamb /= 2;
		if (iceside != null) {
			if (Tamb > 0)
				Tamb /= 4;
			world.setBlockAndUpdate(iceside, Blocks.WATER.defaultBlockState());
		}
		if (fireside != null)
			Tamb += 200;
		if (lavaside != null)
			Tamb += 600;
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
			for (Direction d : Direction.values()) {
				BlockPos p = pos.relative(d);
				if (world.getBlockState(p).is(Blocks.SNOW) || world.getBlockState(p).is(Blocks.SNOW_BLOCK)) {
					world.removeBlock(p, false);
					break;
				}
			}
			for (Direction d : Direction.values()) {
				BlockPos p = pos.relative(d);
				if (world.getBlockState(p).is(Blocks.ICE)) {
					world.setBlockAndUpdate(p, Blocks.WATER.defaultBlockState());
					break;
				}
			}
		}
		this.setChanged();
	}

	/** Original getWaterBuckets: a single water bucket in slot 0 adds 1000 mB if there is room. */
	private void getWaterBuckets() {
		ItemStack is = inv.getStackInSlot(0);
		if (is.getCount() == 1 && is.is(Items.WATER_BUCKET) && this.canTakeWater(1000)) {
			water.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
			inv.setStackInSlot(0, new ItemStack(Items.BUCKET));
		}
	}

	private boolean canTakeWater(int amt) {
		return (water.isEmpty() || water.getFluid().getFluid().isSame(Fluids.WATER)) && water.getFluidAmount()+amt <= CAPACITY;
	}

	/** Original addWater (water bucket used on the block). */
	public boolean addWater(int amt) {
		if (this.canTakeWater(amt)) {
			water.fill(new FluidStack(Fluids.WATER, amt), IFluidHandler.FluidAction.EXECUTE);
			this.setChanged();
			return true;
		}
		return false;
	}

	public int getTimerScaled(int px) {
		Synthesis r = this.getRecipe();
		return r == null ? 0 : timer*px/r.getDuration(temperature);
	}

	public FluidTank getInputTank() {
		return water;
	}

	public FluidTank getOutputTank() {
		return tank;
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	@Override
	public int getTemperature() {
		return temperature;
	}

	@Override
	public void setTemperature(int T) {
		temperature = T;
	}

	@Override
	public void addTemperature(int T) {
		temperature += T;
	}

	@Override
	public int getMaxTemperature() {
		return MAXTEMP;
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
	public void onOverheat(Level world, BlockPos pos) {}

	/** Items in from every side; only empty buckets come out (original canRemoveItem). */
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
				return inv.getStackInSlot(slot).is(Items.BUCKET) ? inv.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
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

	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 2;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return (t == 0 ? water : tank).getFluid();
			}

			@Override
			public int getTankCapacity(int t) {
				return CAPACITY;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return t == 0 && Synthesis.byInput(stack.getFluid()) != null;
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return SynthesizerBlockEntity.this.fill(side, resource, action);
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				if (resource.isEmpty() || !tank.getFluid().getFluid().isSame(resource.getFluid()))
					return FluidStack.EMPTY;
				return SynthesizerBlockEntity.this.drain(side, resource.getAmount(), action);
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				return SynthesizerBlockEntity.this.drain(side, max, action);
			}
		};
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (Synthesis.byInput(resource.getFluid()) == null)
			return 0;
		int n = water.fill(resource, action);
		if (n > 0 && action.execute())
			this.setChanged();
		return n;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		FluidStack d = tank.drain(maxDrain, action);
		if (!d.isEmpty() && action.execute())
			this.setChanged();
		return d;
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe() || p == PipeType.FUEL_LINE;
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p);
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side.getAxis().isHorizontal() ? Flow.INPUT : Flow.OUTPUT;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (int i = 0; i < inv.getSlots(); i++)
			net.minecraft.world.Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i).copy());
	}

	@Override
	public void clearContent() {
		for (int i = 0; i < inv.getSlots(); i++)
			inv.setStackInSlot(i, ItemStack.EMPTY);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("time", timer);
		NBT.putInt("temp", temperature);
		NBT.put("water", water.writeToNBT(registries, new CompoundTag()));
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		timer = NBT.getInt("time");
		temperature = NBT.getInt("temp");
		water.readFromNBT(registries, NBT.getCompound("water"));
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
