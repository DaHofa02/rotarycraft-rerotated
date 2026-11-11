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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotarySounds;

/**
 * Original: Reika.RotaryCraft.TileEntities.Production.TileEntityLavaMaker - the rock melter. Shaft power from below (no minimum) heats
 * it: every second the temperature rises by log2(power) and loses 1/64 of its difference to the ambient temperature (at most 1800 deg C;
 * overheating does nothing). While it is at least as hot as the first recipe item in its 9 slots needs, the power is collected as energy
 * (otherwise 15 % of it is lost per tick); an item melts into the 64000 mB tank once 20 times its melting energy is stored - one item
 * per tick. The liquid leaves through the four sides (pipes and fuel lines); items only go in. Above 50 deg C it melts one adjacent
 * snow block and one ice block per second. Signals 15 when the tank is full or nothing in it can be melted.
 */
public class LavaMakerBlockEntity extends PowerReceiverBlockEntity implements PipeConnector, TemperatureMachine, net.minecraft.world.Clearable {

	public static final int CAPACITY = 64000;
	public static final int MAXTEMP = 1800;

	private int temperature;
	private long energy;
	private int tempTimer;
	private int soundTick;

	private final FluidTank tank = new FluidTank(CAPACITY) {
		@Override
		protected void onContentsChanged() {
			LavaMakerBlockEntity.this.setChanged();
		}
	};

	private final ItemStackHandler inv = new ItemStackHandler(9) {
		@Override
		public boolean isItemValid(int slot, ItemStack is) {
			return LavaMakerRecipes.isValidFuel(is);
		}

		@Override
		protected void onContentsChanged(int slot) {
			LavaMakerBlockEntity.this.setChanged();
		}
	};

	public LavaMakerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.LAVA_MAKER.get(), pos, state, 1, 1, 1);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public ItemStackHandler getInventory() {
		return inv;
	}

	public FluidTank getTank() {
		return tank;
	}

	public long getEnergy() {
		return energy;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		read = Direction.DOWN;
		if (world.isClientSide) {
			if (omega > 0 && power > 0)
				world.addParticle(ParticleTypes.CRIT, pos.getX()+rand.nextDouble(), pos.getY(), pos.getZ()+rand.nextDouble(),
						-0.2+0.4*rand.nextDouble(), 0.4*rand.nextDouble(), -0.2+0.4*rand.nextDouble());
			return;
		}
		this.readPower();
		tempTimer++;
		if (tempTimer >= 20) {
			tempTimer = 0;
			this.updateTemperature(world, pos);
		}
		if (temperature >= this.getMeltingTemperature())
			energy += power;
		else
			energy = (long)(energy*0.85);
		soundTick++;
		if (omega > 0 && power > 0 && soundTick > 98) {
			RotarySounds.play(world, pos, RotarySounds.FRICTION.get(), MachineSounds.isMuffled(world, pos) ? 0.1F : 0.5F, 0.5F);
			soundTick = 0;
		}
		for (int i = 0; i < inv.getSlots(); i++) {
			ItemStack is = inv.getStackInSlot(i);
			LavaMakerRecipes.Recipe r = LavaMakerRecipes.get(is);
			if (r != null && this.canMake(r.output()) && energy >= r.energy()*20L) {
				tank.fill(r.output(), IFluidHandler.FluidAction.EXECUTE);
				is.shrink(1);
				inv.setStackInSlot(i, is.isEmpty() ? ItemStack.EMPTY : is);
				energy -= r.energy()*20L;
				this.syncAllData();
				return;
			}
		}
	}

	/** The temperature the first meltable item needs; nothing to melt: unreachable. */
	public int getMeltingTemperature() {
		for (int i = 0; i < inv.getSlots(); i++) {
			LavaMakerRecipes.Recipe r = LavaMakerRecipes.get(inv.getStackInSlot(i));
			if (r != null)
				return r.temperature();
		}
		return Integer.MAX_VALUE;
	}

	private boolean canMake(FluidStack fs) {
		if (tank.isEmpty())
			return true;
		return FluidStack.isSameFluid(tank.getFluid(), fs) && tank.getFluidAmount()+fs.getAmount() <= CAPACITY;
	}

	private void updateTemperature(Level world, BlockPos pos) {
		int Tamb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		if (power > 0)
			temperature = (int)(temperature+ReikaMathLibrary.logbase(power, 2));
		if (temperature > Tamb)
			temperature -= (temperature-Tamb)/64;
		else
			temperature += (temperature-Tamb)/64;
		if (temperature-Tamb <= 64 && temperature > Tamb)
			temperature--;
		if (temperature > MAXTEMP)
			temperature = MAXTEMP;
		if (temperature > 50) {
			for (Direction d : Direction.values()) {
				if (world.getBlockState(pos.relative(d)).is(Blocks.SNOW_BLOCK)) {
					world.setBlockAndUpdate(pos.relative(d), Blocks.AIR.defaultBlockState());
					break;
				}
			}
			for (Direction d : Direction.values()) {
				if (world.getBlockState(pos.relative(d)).is(Blocks.ICE)) {
					world.setBlockAndUpdate(pos.relative(d), Blocks.WATER.defaultBlockState());
					break;
				}
			}
		}
		this.syncAllData();
	}

	public boolean hasStone() {
		for (int i = 0; i < inv.getSlots(); i++)
			if (!inv.getStackInSlot(i).isEmpty())
				return true;
		return false;
	}

	@Override
	public int getRedstoneOverride() {
		if (tank.getFluidAmount() >= CAPACITY)
			return 15;
		for (int i = 0; i < inv.getSlots(); i++) {
			LavaMakerRecipes.Recipe r = LavaMakerRecipes.get(inv.getStackInSlot(i));
			if (r != null && this.canMake(r.output()))
				return 0;
		}
		return 15;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		phi += (float)Math.pow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
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

	private static boolean canOutputTo(@Nullable Direction side) {
		return side != null && side.getAxis().isHorizontal();
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe() || p == PipeType.FUEL_LINE;
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return canOutputTo(side) && this.canConnectToPipe(p);
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return canOutputTo(side) ? Flow.OUTPUT : Flow.NONE;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		return 0;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (!canOutputTo(from))
			return FluidStack.EMPTY;
		FluidStack fs = tank.drain(maxDrain, action);
		if (!fs.isEmpty() && action.execute())
			this.syncAllData();
		return fs;
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
				return false;
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return 0;
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				return FluidStack.isSameFluid(resource, tank.getFluid()) ? LavaMakerBlockEntity.this.drain(side, resource.getAmount(), action) : FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				return LavaMakerBlockEntity.this.drain(side, max, action);
			}
		};
	}

	/** Meltable items go in from any side; nothing comes out (original canExtractItem). */
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
				return ItemStack.EMPTY;
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
		NBT.putLong("e", energy);
		NBT.putInt("temp", temperature);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.put("inv", inv.serializeNBT(registries));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		energy = NBT.getLong("e");
		temperature = NBT.getInt("temp");
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		if (NBT.contains("inv"))
			inv.deserializeNBT(registries, NBT.getCompound("inv"));
	}
}
