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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.api.interfaces.Shockable;
import reika.rotarycraft.content.blockentity.FrictionHeatable;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.world.AmbientTemperature;

/**
 * Original: Reika.ReactorCraft.TileEntities.Processing.TileEntityElectrolyzer - runs only on discharges from a RotaryCraft Van de Graaff
 * generator next to it (from 4096 C): every discharge advances its step timer by sqrt(charge-4096)/16 (at least 1), and at 50 one
 * electrolysis runs. Salt (any c:dusts/salt or RotaryCraft salt, used up) at 801 C or more gives 100 mB chlorine (top tank) and 100 mB
 * sodium (bottom tank); 100 mB heavy water gives 100 mB deuterium (top) and 50 mB oxygen (bottom). The input tank (12000 mB) fills
 * from the sides, the top tank drains upwards, the bottom one downwards (6000 mB each). Every second its temperature moves one degree
 * (two when far off) towards the surroundings (halved next to water, quartered by ice, which melts; +200 by fire, +600 by lava; snow and
 * ice melt above 100 C), up to 1200 C; friction heaters heat it at half rate.
 */
public class ElectrolyzerBlockEntity extends RotaryCraftBlockEntity implements PipeConnector, FrictionHeatable, Shockable, net.minecraft.world.Clearable {

	public static final int SODIUM_MELT = 98;
	public static final int SALT_MELT = 801;
	public static final int CAPACITY = 6000;
	public static final int MAXTEMP = 1200;
	public static final int TIMER_CAP = 50;

	private static final TagKey<Item> SALT_DUST = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "dusts/salt"));
	private static final TagKey<Item> SALT_TAG = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "salt"));

	private final FluidTank tankL = new FluidTank(CAPACITY) {
		@Override
		protected void onContentsChanged() {
			ElectrolyzerBlockEntity.this.setChanged();
		}
	};
	private final FluidTank tankH = new FluidTank(CAPACITY) {
		@Override
		protected void onContentsChanged() {
			ElectrolyzerBlockEntity.this.setChanged();
		}
	};
	private final FluidTank input = new FluidTank(CAPACITY*2) {
		@Override
		protected void onContentsChanged() {
			ElectrolyzerBlockEntity.this.setChanged();
		}
	};

	private final ItemStackHandler inv = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			for (Electrolysis e : Electrolysis.values())
				if (e.uses(is))
					return true;
			return false;
		}

		@Override
		protected void onContentsChanged(int slot) {
			ElectrolyzerBlockEntity.this.setChanged();
		}
	};

	private int timer;
	private int tempTimer;
	private int temperature;

	@Nullable
	private Electrolysis recipe;

	public ElectrolyzerBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.ELECTROLYZER.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Electrolyzer";
	}

	public enum Electrolysis {
		SALT(null, 0, is -> is.is(SALT_DUST) || is.is(SALT_TAG) || is.is(rotaryItem("salt")), true,
				() -> ReactorFluids.get("chlorine"), 100, () -> ReactorFluids.get("sodium"), 100, SALT_MELT),
		HEAVYWATER(() -> ReactorFluids.get("heavy_water"), 100, null, false, () -> ReactorFluids.get("deuterium"), 100, () -> ReactorFluids.get("oxygen"), 50, 0);

		@Nullable
		private final Supplier<Fluid> requiredFluid;
		public final int requiredAmount;
		@Nullable
		private final Predicate<ItemStack> requiredItem;
		public final boolean consumeItem;
		private final Supplier<Fluid> upperOutput;
		public final int upperAmount;
		private final Supplier<Fluid> lowerOutput;
		public final int lowerAmount;
		public final int requiredTemperature;

		Electrolysis(@Nullable Supplier<Fluid> in, int amt, @Nullable Predicate<ItemStack> item, boolean consume, Supplier<Fluid> out1, int amt1, Supplier<Fluid> out2, int amt2,
				int temp) {
			requiredFluid = in;
			requiredAmount = amt;
			requiredItem = item;
			consumeItem = consume;
			upperOutput = out1;
			upperAmount = amt1;
			lowerOutput = out2;
			lowerAmount = amt2;
			requiredTemperature = temp;
		}

		private static Item rotaryItem(String id) {
			return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("rotarycraft", id));
		}

		public boolean requirementsMet(ElectrolyzerBlockEntity te) {
			if (requiredFluid != null) {
				if (te.input.isEmpty() || !te.input.getFluid().getFluid().isSame(requiredFluid.get()) || te.input.getFluidAmount() < requiredAmount)
					return false;
			}
			if (requiredItem != null) {
				ItemStack is = te.inv.getStackInSlot(0);
				if (is.isEmpty() || !requiredItem.test(is))
					return false;
			}
			if (te.tankL.getFluidAmount() >= te.tankL.getCapacity() || (!te.tankL.isEmpty() && !te.tankL.getFluid().getFluid().isSame(upperOutput.get())))
				return false;
			if (te.tankH.getFluidAmount() >= te.tankH.getCapacity() || (!te.tankH.isEmpty() && !te.tankH.getFluid().getFluid().isSame(lowerOutput.get())))
				return false;
			return te.temperature >= requiredTemperature;
		}

		/** Original run: the outputs are added up to the tanks' capacity (HybridTank.addLiquid clamps). */
		private void run(ElectrolyzerBlockEntity te) {
			if (requiredFluid != null)
				te.input.drain(requiredAmount, IFluidHandler.FluidAction.EXECUTE);
			if (requiredItem != null && consumeItem)
				te.inv.extractItem(0, 1, false);
			te.tankL.fill(new FluidStack(upperOutput.get(), upperAmount), IFluidHandler.FluidAction.EXECUTE);
			te.tankH.fill(new FluidStack(lowerOutput.get(), lowerAmount), IFluidHandler.FluidAction.EXECUTE);
		}

		public boolean uses(Fluid f) {
			return requiredFluid != null && requiredFluid.get().isSame(f);
		}

		public boolean uses(ItemStack is) {
			return requiredItem != null && requiredItem.test(is);
		}
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		tempTimer++;
		if (tempTimer >= 20) {
			tempTimer = 0;
			if (!world.isClientSide)
				this.updateTemperature(world, pos);
		}
		if (world.isClientSide)
			return;
		if (recipe == null)
			recipe = this.findRecipe();
		if (recipe != null && recipe.requirementsMet(this)) {
			if (timer >= TIMER_CAP) {
				timer = 0;
				recipe.run(this);
				this.syncAllData();
			}
		}
		else {
			recipe = null;
			timer = 0;
		}
	}

	@Nullable
	private Electrolysis findRecipe() {
		for (Electrolysis e : Electrolysis.values())
			if (e.requirementsMet(this))
				return e;
		return null;
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
				if (world.getBlockState(p).is(Blocks.SNOW)) {
					world.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
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
	}

	@Override
	public void onDischarge(int charge, double range) {
		if (recipe != null) {
			int extra = charge-this.getMinDischarge();
			int n = extra > 0 ? (int)Math.sqrt(extra)/16 : 1;
			if (n == 0)
				n = 1;
			timer += n;
		}
	}

	@Override
	public int getMinDischarge() {
		return 4096;
	}

	@Override
	public boolean canDischargeLongRange() {
		return false;
	}

	@Override
	public float getAimX() {
		return 0.5F;
	}

	@Override
	public float getAimY() {
		return 0.9375F;
	}

	@Override
	public float getAimZ() {
		return 0.5F;
	}

	/** Original addHeavyWater: a heavy water bucket by hand. */
	public boolean addHeavyWater(int amt) {
		if (input.getSpace() < amt || (!input.isEmpty() && !input.getFluid().getFluid().isSame(ReactorFluids.get("heavy_water"))))
			return false;
		input.fill(new FluidStack(ReactorFluids.get("heavy_water"), amt), IFluidHandler.FluidAction.EXECUTE);
		return true;
	}

	public int getTimerScaled(int d) {
		return d*timer/TIMER_CAP;
	}

	public int getTimer() {
		return timer;
	}

	public FluidTank getHeavyTank() {
		return tankH;
	}

	public FluidTank getLightTank() {
		return tankL;
	}

	public FluidTank getInputTank() {
		return input;
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
	public boolean allowExternalHeating() {
		return true;
	}

	@Override
	public boolean canBeFrictionHeated() {
		return true;
	}

	@Override
	public float getMultiplier() {
		return 0.5F;
	}

	@Override
	public void resetAmbientTemperatureTimer() {
		tempTimer = 0;
	}

	@Override
	public void onOverheat(Level world, BlockPos pos) {}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	/** Items in from every side, nothing out (original canRemoveItem false). */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return new IItemHandler() {
			@Override
			public int getSlots() {
				return 1;
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
				return 3;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return (t == 0 ? tankH : t == 1 ? tankL : input).getFluid();
			}

			@Override
			public int getTankCapacity(int t) {
				return t == 2 ? CAPACITY*2 : CAPACITY;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return t == 2 && canFill(stack.getFluid());
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return ElectrolyzerBlockEntity.this.fill(side, resource, action);
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				FluidTank t = side == Direction.DOWN ? tankH : side == Direction.UP ? tankL : null;
				if (t == null || t.isEmpty() || !t.getFluid().getFluid().isSame(resource.getFluid()))
					return FluidStack.EMPTY;
				return t.drain(resource.getAmount(), action);
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				return ElectrolyzerBlockEntity.this.drain(side, max, action);
			}
		};
	}

	private static boolean canFill(Fluid f) {
		for (Electrolysis e : Electrolysis.values())
			if (e.uses(f))
				return true;
		return false;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (from == null || from.getAxis().isVertical() || resource.isEmpty() || !canFill(resource.getFluid()))
			return 0;
		return input.fill(resource, action);
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (from == Direction.DOWN)
			return tankH.drain(maxDrain, action);
		if (from == Direction.UP)
			return tankL.drain(maxDrain, action);
		return FluidStack.EMPTY;
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p);
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side.getAxis().isVertical() ? Flow.OUTPUT : Flow.INPUT;
	}

	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		net.minecraft.world.Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(0).copy());
	}

	@Override
	public void clearContent() {
		inv.setStackInSlot(0, ItemStack.EMPTY);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("heavytank", tankH.writeToNBT(registries, new CompoundTag()));
		NBT.put("lighttank", tankL.writeToNBT(registries, new CompoundTag()));
		NBT.put("input", input.writeToNBT(registries, new CompoundTag()));
		NBT.put("inv", inv.serializeNBT(registries));
		NBT.putInt("temp", temperature);
		NBT.putInt("time", timer);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		tankH.setFluid(FluidStack.EMPTY);
		tankH.readFromNBT(registries, NBT.getCompound("heavytank"));
		tankL.setFluid(FluidStack.EMPTY);
		tankL.readFromNBT(registries, NBT.getCompound("lighttank"));
		input.setFluid(FluidStack.EMPTY);
		input.readFromNBT(registries, NBT.getCompound("input"));
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
		temperature = NBT.getInt("temp");
		timer = NBT.getInt("time");
	}

	/** Original TileEntityReactorBase.allowHeatExtraction (HeatConduction): true. */
	@Override
	public boolean allowHeatExtraction() {
		return true;
	}
}
