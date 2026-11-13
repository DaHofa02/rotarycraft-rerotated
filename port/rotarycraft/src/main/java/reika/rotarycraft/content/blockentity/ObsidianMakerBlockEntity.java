/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
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
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Production.TileEntityObsidianMaker - mixes lava and water from its two 320 000 mB tanks
 * (filled from the four sides) into its 9 slots: obsidian between 550 and 750 deg C (1000 mB lava, 2500 mB water), cobblestone below
 * 500 or above 900 deg C (1000 mB water, no lava), in between obsidian with a falling chance. Needs 32768 W and 2048 rad/s from below;
 * one item every max(1, 800 - 60*log2(omega+1)) ticks (several per tick from 2^14 rad/s). Every second the temperature drifts 1 deg C to
 * the ambient one and lava heats it by 3 (to at most 600 with water in it, else 1000; water without lava cools it by 2). At 1000 deg C it
 * melts into lava (with block damage on).
 */
public class ObsidianMakerBlockEntity extends PowerReceiverBlockEntity implements TemperatureMachine, PipeConnector, net.minecraft.world.Clearable {

	public static final int CAPACITY = 320000;
	public static final int MAXTEMP = 1000;
	public static final int MIN_OBSIDIAN_TEMP_100 = 550, MAX_OBSIDIAN_TEMP_100 = 750;
	private static final int MIN_OBSIDIAN_TEMP_0 = 500, MAX_OBSIDIAN_TEMP_0 = 900;
	/** Original DurationRegistry.OBSIDIAN. */
	private static final int DURATION_BASE = 800, DURATION_SCALE = 60;

	public int temperature;
	private int temptick;
	public boolean idle;
	public float overred, overgreen;

	private final FluidTank lava = new FluidTank(CAPACITY, fs -> fs.is(Fluids.LAVA));
	private final FluidTank water = new FluidTank(CAPACITY, fs -> fs.is(Fluids.WATER));

	private final ItemStackHandler inv = new ItemStackHandler(9) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return false;
		}

		@Override
		protected void onContentsChanged(int slot) {
			ObsidianMakerBlockEntity.this.setChanged();
		}
	};

	public ObsidianMakerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.OBSIDIAN_MAKER.get(), pos, state, 1, 2048, 32768);
	}

	@Override
	public String getName() {
		return "Obsidian Factory";
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public int getWater() {
		return water.getFluidAmount();
	}

	public int getLava() {
		return lava.getFluidAmount();
	}

	public void addWater(int amt) {
		water.fill(new FluidStack(Fluids.WATER, amt), IFluidHandler.FluidAction.EXECUTE);
	}

	public void addLava(int amt) {
		lava.fill(new FluidStack(Fluids.LAVA, amt), IFluidHandler.FluidAction.EXECUTE);
	}

	public int getWaterScaled(int s) {
		return this.getWater()*s/CAPACITY;
	}

	public int getLavaScaled(int s) {
		return this.getLava()*s/CAPACITY;
	}

	public int getOperationTime() {
		return (int)Math.max(1, DURATION_BASE-DURATION_SCALE*ReikaMathLibrary.logbase(Math.max(0, omega)+1, 2));
	}

	public int getNumberConsecutiveOperations() {
		int threshold = (int)ReikaMathLibrary.intpow(2, 1+DURATION_BASE/DURATION_SCALE);
		return 1+(omega >= threshold ? (int)(ReikaMathLibrary.logbase(omega, 2)-DURATION_BASE/DURATION_SCALE) : 0);
	}

	/** Original getProducedItem. */
	public boolean makesObsidian() {
		if (temperature >= MIN_OBSIDIAN_TEMP_100 && temperature <= MAX_OBSIDIAN_TEMP_100)
			return true;
		if (temperature > MAX_OBSIDIAN_TEMP_0 || temperature < MIN_OBSIDIAN_TEMP_0)
			return false;
		float f = temperature > MAX_OBSIDIAN_TEMP_100 ? (temperature-MAX_OBSIDIAN_TEMP_100)/150F : (MIN_OBSIDIAN_TEMP_100-temperature)/50F;
		return rand.nextDouble() < 1-f;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		tickcount++;
		read = Direction.DOWN;
		this.readPower();
		if (++temptick >= 20) {
			temptick = 0;
			this.updateTemperature(world, pos);
			if (this.isRemoved() || world.getBlockEntity(pos) != this)
				return;
		}
		if (power >= MINPOWER && omega >= MINSPEED && !water.isEmpty() && !lava.isEmpty()) {
			idle = this.getNonFullStack(new ItemStack(Blocks.OBSIDIAN)) < 0 && this.getNonFullStack(new ItemStack(Blocks.COBBLESTONE)) < 0;
			int n = this.getNumberConsecutiveOperations();
			for (int i = 0; i < n; i++) {
				if (n > 1 || tickcount >= this.getOperationTime()) {
					tickcount = 0;
					this.mix((ServerLevel)world, pos);
				}
			}
		}
		else
			idle = true;
	}

	private void updateTemperature(Level world, BlockPos pos) {
		overred = overgreen = 0;
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		int max = MAXTEMP;
		if (temperature > Tamb)
			temperature--;
		if (temperature < Tamb)
			temperature++;
		if (!water.isEmpty()) {
			max = 600;
			if (lava.isEmpty())
				temperature = Math.max(temperature-2, Tamb);
		}
		if (!lava.isEmpty())
			temperature = Math.min(temperature+3, max);
		if (temperature > 800)
			overred = 0.25F;
		if (temperature > 890.909)
			{ overred = 0.4F; overgreen = 0.1F; }
		if (temperature > 963.636)
			{ overred = 0.55F; overgreen = 0.2F; }
		this.syncAllData();
		if (temperature >= MAXTEMP && RotaryConfig.BLOCKDAMAGE.get())
			this.overheat(world, pos);
	}

	public void overheat(Level world, BlockPos pos) {
		world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 3, 1);
		world.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
	}

	private void mix(ServerLevel world, BlockPos pos) {
		boolean obsid = this.makesObsidian();
		ItemStack is = new ItemStack(obsid ? Blocks.OBSIDIAN : Blocks.COBBLESTONE);
		int slot = this.getNonFullStack(is);
		if (slot < 0)
			return;
		int lavaamt = obsid ? 1000 : 50;
		if (lava.getFluidAmount() < lavaamt || water.getFluidAmount() < 1000)
			return;
		ItemStack out = inv.getStackInSlot(slot);
		inv.setStackInSlot(slot, out.isEmpty() ? is : out.copyWithCount(out.getCount()+1));
		if (obsid)
			lava.drain(lavaamt, IFluidHandler.FluidAction.EXECUTE);
		water.drain(obsid ? 2500 : 1000, IFluidHandler.FluidAction.EXECUTE);
		world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F+0.5F*rand.nextFloat(), 0.7F+0.3F*rand.nextFloat());
		world.sendParticles(ParticleTypes.SMOKE, pos.getX()+0.5, pos.getY()+0.75, pos.getZ()+0.5, 9, 0.25, 0, 0.25, 0);
		this.syncAllData();
	}

	private int getNonFullStack(ItemStack is) {
		for (int k = 0; k < inv.getSlots(); k++) {
			ItemStack in = inv.getStackInSlot(k);
			if (in.isEmpty() || ItemStack.isSameItem(in, is) && in.getCount() < in.getMaxStackSize())
				return k;
		}
		return -1;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public int getRedstoneOverride() {
		return idle ? 15 : 0;
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
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return side.getAxis() != Direction.Axis.Y;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side.getAxis() != Direction.Axis.Y ? Flow.INPUT : Flow.NONE;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (from != null && from.getAxis() == Direction.Axis.Y)
			return 0;
		int amt = resource.is(Fluids.WATER) ? water.fill(resource, action) : resource.is(Fluids.LAVA) ? lava.fill(resource, action) : 0;
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
				return t == 0 ? lava.getFluid() : water.getFluid();
			}

			@Override
			public int getTankCapacity(int t) {
				return CAPACITY;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return t == 0 ? stack.is(Fluids.LAVA) : stack.is(Fluids.WATER);
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return ObsidianMakerBlockEntity.this.fill(side, resource, action);
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

	/** Nothing goes in; the products come out from any side. */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return inv;
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
		lava.setFluid(FluidStack.EMPTY);
		water.setFluid(FluidStack.EMPTY);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("lava", lava.writeToNBT(registries, new CompoundTag()));
		NBT.put("water", water.writeToNBT(registries, new CompoundTag()));
		NBT.putInt("temp", temperature);
		NBT.putFloat("red", overred);
		NBT.putFloat("green", overgreen);
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		lava.setFluid(FluidStack.EMPTY);
		lava.readFromNBT(registries, NBT.getCompound("lava"));
		water.setFluid(FluidStack.EMPTY);
		water.readFromNBT(registries, NBT.getCompound("water"));
		temperature = NBT.getInt("temp");
		overred = NBT.getFloat("red");
		overgreen = NBT.getFloat("green");
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
