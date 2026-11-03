/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.content.recipe.PulseJetRecipe;
import reika.rotarycraft.lib.data.InventoryHelper;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.lib.world.TemperatureEnvironment;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryFluids;
import reika.rotarycraft.registry.RotaryItems;
import reika.rotarycraft.registry.RotaryRecipeTypes;
import reika.rotarycraft.registry.RotarySounds;

/**
 * Original: Reika.RotaryCraft.TileEntities.Processing.TileEntityPulseFurnace. Powered from below at 131072 rad/s or more, it burns jet
 * fuel (100 mB per 100 ticks while smelting) and heats up towards 1000 °C; water cools it. A {@code rotarycraft:pulse_jet} recipe runs
 * once the furnace is hot enough: a "fire" phase (100 ticks, shorter above 900/950/980 °C) followed by 20 ticks of smelting. Above
 * 1000 °C it explodes into scrap. It heats its surroundings and burns what touches it (temperature/100 damage).
 * Slots: 0 input, 2 output. Not ported: the oxygen accelerant (ReactorCraft fluid), heat-sink fins.
 */
public class PulseJetFurnaceBlockEntity extends PowerReceiverBlockEntity implements TemperatureMachine, PipeConnector, net.minecraft.world.Clearable {

	public static final int CAPACITY = 3000;
	public static final int MAXFUEL = 8000;
	public static final int MAXTEMP = 1000;
	public static final int SMELTTICKS_BASE = 100;

	public int pulseFurnaceCookTime;
	public boolean idle = false;
	public int temperature;
	private int tickcount2 = 0;
	public int smelttick = 0;
	private int soundtick = 2000;

	private final FluidTank fuel = new FluidTank(MAXFUEL, fs -> fs.is(RotaryFluids.JET_FUEL.get()));
	private final FluidTank water = new FluidTank(CAPACITY, fs -> fs.is(Fluids.WATER));

	private final ItemStackHandler inv = new ItemStackHandler(3) {
		@Override
		protected void onContentsChanged(int slot) {
			PulseJetFurnaceBlockEntity.this.setChanged();
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return PulseJetFurnaceBlockEntity.this.isItemValidForSlot(slot, stack);
		}
	};

	public PulseJetFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.PULSE_JET.get(), pos, state, 1, 131072, 1);
	}

	@Override
	public String getName() {
		return "Pulse Jet Furnace";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public int getCookProgressScaled(int par1) {
		return Math.min(par1, (pulseFurnaceCookTime * par1) / this.getOperationTime());
	}

	public int getFuelScaled(int par1) {
		return (fuel.getFluidAmount() * par1) / MAXFUEL;
	}

	public int getTempScaled(int par1) {
		return (temperature * par1) / MAXTEMP;
	}

	public int getWaterScaled(int par1) {
		return (water.getFluidAmount() * par1) / CAPACITY;
	}

	public int getFireScaled(int par1) {
		return Math.min(par1, (smelttick * par1) / this.getSmeltingDuration());
	}

	private void heatAmbient(Level world, BlockPos pos) {
		if (fuel.getFluidAmount() > 0 && this.canHeatUp())
			temperature += Math.max((MAXTEMP-temperature)/8, 4);
		int dT = 2;
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		if (Tamb < -40)
			dT = 8;
		else if (Tamb < -5)
			dT = 6;
		else if (Tamb < 5)
			dT = 4;
		if (Tamb >= 300 && this.canHeatUp())
			dT = -1;
		else if (Tamb >= temperature)
			dT = 0;
		else if (Tamb > 30)
			dT = 1;
		if (water.getFluidAmount() > 0) {
			if (rand.nextInt(3) == 0) {
				int rem = (temperature*2/MAXTEMP)*50;
				if (Tamb >= 180)
					rem *= 2;
				if (Tamb >= 90)
					rem *= 2;
				if (rem > 0)
					water.drain(rem, IFluidHandler.FluidAction.EXECUTE);
			}
			if (Tamb >= 300)
				temperature -= temperature/256;
			else
				temperature -= temperature/64;
		}
		if (dT != 0) {
			if (dT > 0)
				temperature = Math.max(Tamb, temperature-dT);
			else
				temperature -= dT;
		}
	}

	private boolean canHeatUp() {
		return power >= MINPOWER && omega >= MINSPEED && !fuel.isEmpty();
	}

	public void updateTemperature(Level world, BlockPos pos) {
		this.heatAmbient(world, pos);
		if (temperature > 915 && world instanceof ServerLevel sl) {
			RotaryCraft.LOGGER.warn("WARNING: {} at {} is reaching very high temperature!", this.getName(), pos);
			sl.sendParticles(ParticleTypes.LAVA, pos.getX()+rand.nextFloat(), pos.getY()+rand.nextFloat(), pos.getZ()+rand.nextFloat(), 1, 0, 0, 0, 0);
		}
		TemperatureEnvironment.temperatureEnvironment(world, pos, temperature);
		if (temperature > MAXTEMP)
			this.overheat(world, pos);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		read = Direction.DOWN; //original getPowerBelow
		if (world.isClientSide)
			return;
		this.readPower();
		PulseJetRecipe recipe = this.getRecipe();
		idle = recipe == null && omega > MINSPEED;
		soundtick++;
		if (tickcount >= 20) {
			this.updateTemperature(world, pos);
			if (world.getBlockEntity(pos) != this)
				return;
			tickcount = 0;
		}
		if (soundtick >= 18 && this.canHeatUp()) {
			soundtick = 0;
			RotarySounds.play(world, pos, RotarySounds.PULSEJET.get(), 1, 1);
		}
		if (recipe != null && tickcount2 >= 100) {
			fuel.drain(100, IFluidHandler.FluidAction.EXECUTE);
			tickcount2 = 0;
		}
		tickcount++;
		tickcount2++;
		int tick = 1;
		int duration = this.getSmeltingDuration();
		if (recipe != null)
			smelttick += tick;
		else
			smelttick = 0;
		if (recipe != null && smelttick >= duration) {
			pulseFurnaceCookTime += tick;
			if (pulseFurnaceCookTime >= this.getOperationTime()) {
				pulseFurnaceCookTime = 0;
				this.smeltItem(recipe);
				smelttick = 0;
			}
		}
		else {
			pulseFurnaceCookTime = 0;
		}
	}

	private int getSmeltingDuration() {
		if (temperature >= 980) //8x speed if about to fail
			return SMELTTICKS_BASE/8;
		else if (temperature >= 950) //4x speed if running uncooled and very hot
			return SMELTTICKS_BASE/4;
		else if (temperature >= 900) //2x speed if running uncooled
			return SMELTTICKS_BASE/2;
		else
			return SMELTTICKS_BASE;
	}

	private Optional<RecipeHolder<PulseJetRecipe>> findRecipe(ItemStack is) {
		if (level == null || is.isEmpty())
			return Optional.empty();
		return level.getRecipeManager().getRecipeFor(RotaryRecipeTypes.PULSE_JET.get(), new SingleRecipeInput(is), level);
	}

	/** Original getRecipe: power, input, fuel, temperature and room in the output. */
	@Nullable
	private PulseJetRecipe getRecipe() {
		if (power <= 0 || omega < MINSPEED)
			return null;
		ItemStack in = inv.getStackInSlot(0);
		if (in.isEmpty() || fuel.isEmpty())
			return null;
		PulseJetRecipe rec = this.findRecipe(in).map(RecipeHolder::value).orElse(null);
		if (rec == null || rec.temperature() > temperature)
			return null;
		ItemStack out = inv.getStackInSlot(2);
		ItemStack res = rec.result();
		if (!out.isEmpty() && (!ItemStack.isSameItemSameComponents(out, res) || out.getCount()+res.getCount() > Math.min(64, res.getMaxStackSize())))
			return null;
		return rec;
	}

	private void smeltItem(PulseJetRecipe rec) {
		InventoryHelper.addOrSetStack(rec.result().copy(), inv, 2);
		InventoryHelper.decrStack(inv, 0, 1);
	}

	/** Original overheat: ReikaWorldHelper.overheat - the furnace explodes (strength 12) and leaves up to 17 scrap. */
	public void overheat(Level world, BlockPos pos) {
		world.removeBlock(pos, false);
		world.explode(null, pos.getX(), pos.getY(), pos.getZ(), 12F, false, RotaryConfig.BLOCKDAMAGE.get() ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
		int n = rand.nextInt(18);
		for (int i = 0; i < n; i++) {
			ItemEntity ent = new ItemEntity(world, pos.getX()+rand.nextFloat(), pos.getY()+0.5, pos.getZ()+rand.nextFloat(), new ItemStack(RotaryItems.part(CraftingPart.SCRAP)));
			ent.setDeltaMovement(-0.2+0.4*rand.nextFloat(), 0.5*rand.nextFloat(), -0.2+0.4*rand.nextFloat());
			world.addFreshEntity(ent);
		}
	}

	public boolean isItemValidForSlot(int slot, ItemStack is) {
		return slot == 0 && this.findRecipe(is).isPresent();
	}

	public int getThermalDamage() {
		return temperature/100;
	}

	@Override
	public int getRedstoneOverride() {
		return this.getRecipe() == null ? 15 : 0;
	}

	public int getOperationTime() {
		return 20;
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

	public int getWater() {
		return water.getFluidAmount();
	}

	public int getFuel() {
		return fuel.getFluidAmount();
	}

	public void addFuel(int amt) {
		fuel.fill(new FluidStack(RotaryFluids.JET_FUEL.get(), amt), IFluidHandler.FluidAction.EXECUTE);
	}

	public void addWater(int amt) {
		water.fill(new FluidStack(Fluids.WATER, amt), IFluidHandler.FluidAction.EXECUTE);
	}

	@Override
	public boolean canConnectToPipe(PipeType m) {
		return m == PipeType.FUEL_LINE || m.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return side.getAxis() != Direction.Axis.Y;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side.getAxis() != Direction.Axis.Y ? Flow.INPUT : Flow.NONE;
	}

	/** Original fill: water and jet fuel from the four sides. */
	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (from == null || from.getAxis() == Direction.Axis.Y)
			return 0;
		int amt = 0;
		if (resource.is(Fluids.WATER))
			amt = water.fill(resource, action);
		else if (resource.is(RotaryFluids.JET_FUEL.get()))
			amt = fuel.fill(resource, action);
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
				return 2;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return t == 0 ? water.getFluid() : fuel.getFluid();
			}

			@Override
			public int getTankCapacity(int t) {
				return t == 0 ? CAPACITY : MAXFUEL;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return t == 0 ? stack.is(Fluids.WATER) : stack.is(RotaryFluids.JET_FUEL.get());
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return PulseJetFurnaceBlockEntity.this.fill(side, resource, action);
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
				return PulseJetFurnaceBlockEntity.this.isItemValidForSlot(slot, stack);
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
		NBT.putShort("CookTime", (short)pulseFurnaceCookTime);
		NBT.putInt("smelt", smelttick);
		NBT.put("water", water.writeToNBT(registries, new CompoundTag()));
		NBT.put("fuel", fuel.writeToNBT(registries, new CompoundTag()));
		NBT.putInt("temp", temperature);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		pulseFurnaceCookTime = NBT.getShort("CookTime");
		smelttick = NBT.getInt("smelt");
		water.readFromNBT(registries, NBT.getCompound("water"));
		fuel.readFromNBT(registries, NBT.getCompound("fuel"));
		temperature = NBT.getInt("temp");
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

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}
}
