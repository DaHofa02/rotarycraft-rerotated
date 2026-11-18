/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.rotarycraft.api.power.PowerGenerator;
import reika.rotarycraft.api.power.ShaftMerger;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.content.power.PowerSourceList;
import reika.rotarycraft.content.power.PowerSourceTracker;
import reika.rotarycraft.content.power.SimpleProvider;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryFluids;

/**
 * The fuel engine, written for the port from the original's behaviour: burns a liquid fuel from another mod (anything in the fluid tag
 * rotarycraft:engine_fuels: c:diesel, c:gasoline, c:fuel, c:biodiesel and the like; the original took BuildCraft fuel) for 2048 Nm at up to
 * 256 rad/s. It needs lubricant (1 mB per 32 ticks while running) and uses 4 mB of fuel per 36 ticks, plus 1 mB per tick while speeding up.
 * Running heats it by 5 C a second; above 450 C its speed falls to max(16, 706 - T) rad/s, at 750 C it explodes. Water in its third tank
 * cools it by (T - ambient)/100 more a second, using 20 mB a second. Fluids go in at any side but the front.
 */
public class FuelEngineBlockEntity extends IOMachineBlockEntity implements SimpleProvider, PowerGenerator, PipeConnector {

	public static final int GEN_OMEGA = 256;
	public static final int GEN_TORQUE = 2048;
	public static final int CAPACITY = 24000;
	public static final int MAXTEMP = 750;
	public static final TagKey<Fluid> FUELS = FluidTags.create(ResourceLocation.fromNamespaceAndPath("rotarycraft", "engine_fuels"));
	/** Extra fluids treated as fuel (GameTests run without a fuel mod). */
	public static final Set<Fluid> TEST_FUELS = new HashSet<>();
	public static boolean allowOverheat = true;

	private final FluidTank fuel = new FluidTank(CAPACITY, fs -> isFuel(fs.getFluid()));
	private final FluidTank water = new FluidTank(CAPACITY, fs -> fs.is(Fluids.WATER));
	private final FluidTank lube = new FluidTank(CAPACITY, fs -> fs.is(RotaryFluids.LUBRICANT.get()));
	private final IFluidHandler handler = new IFluidHandler() {
		private FluidTank tank(int i) {
			return i == 0 ? fuel : i == 1 ? water : lube;
		}

		@Override
		public int getTanks() {
			return 3;
		}

		@Override
		public FluidStack getFluidInTank(int i) {
			return tank(i).getFluid();
		}

		@Override
		public int getTankCapacity(int i) {
			return CAPACITY;
		}

		@Override
		public boolean isFluidValid(int i, FluidStack fs) {
			return tank(i).isFluidValid(fs);
		}

		@Override
		public int fill(FluidStack fs, FluidAction action) {
			return FuelEngineBlockEntity.this.fill(null, fs, action);
		}

		@Override
		public FluidStack drain(FluidStack fs, FluidAction action) {
			return FluidStack.EMPTY;
		}

		@Override
		public FluidStack drain(int max, FluidAction action) {
			return FluidStack.EMPTY;
		}
	};
	private int temperature = Integer.MIN_VALUE;

	public FuelEngineBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.FUEL_ENGINE.get(), pos, state);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public static boolean isFuel(Fluid f) {
		return f != Fluids.EMPTY && (f.is(FUELS) || TEST_FUELS.contains(f));
	}

	public int getFuel() {
		return fuel.getFluidAmount();
	}

	public int getWater() {
		return water.getFluidAmount();
	}

	public int getLubricant() {
		return lube.getFluidAmount();
	}

	public int getTemperature() {
		return temperature;
	}

	public void setTemperature(int t) {
		temperature = t;
	}

	/** Speed the engine runs up to at its temperature. */
	public int getGenOmega() {
		return temperature <= 450 ? GEN_OMEGA : Math.max(16, 706-temperature);
	}

	public void getIOSides(int meta) {
		write = switch (meta) {
			case 0 -> Direction.WEST;
			case 1 -> Direction.EAST;
			case 2 -> Direction.NORTH;
			default -> Direction.SOUTH;
		};
		read = write.getOpposite();
	}

	@Nullable
	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return side != null && side == write ? null : handler;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSides(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		if (temperature == Integer.MIN_VALUE)
			temperature = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		if (this.getTicksExisted()%20 == 0)
			this.updateTemperature(world, pos);
		boolean run = !fuel.isEmpty() && !lube.isEmpty();
		int target = run ? this.getGenOmega() : 0;
		if (run) {
			if (this.getTicksExisted()%36 == 0)
				fuel.drain(4, IFluidHandler.FluidAction.EXECUTE);
			torque = GEN_TORQUE;
		}
		if (target >= omega && run) {
			if (omega < target) {
				omega = Math.min(target, omega+4*(int)ReikaMathLibrary.logbase(target, 2));
				fuel.drain(1, IFluidHandler.FluidAction.EXECUTE);
			}
		}
		else if (omega > 0)
			omega -= omega/256+1;
		if (omega <= 0) {
			omega = 0;
			if (!run)
				torque = 0;
		}
		power = (long)omega*torque;
		if (power > 0 && world.getGameTime()%32 == 0)
			lube.drain(1, IFluidHandler.FluidAction.EXECUTE);
		this.basicPowerReceiver();
	}

	private void updateTemperature(Level world, BlockPos pos) {
		int amb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		if (temperature > amb) {
			temperature--;
			if (!water.isEmpty()) {
				temperature -= (temperature-amb)/100;
				water.drain(20, IFluidHandler.FluidAction.EXECUTE);
			}
		}
		if (power > 0)
			temperature += 5;
		if (temperature > MAXTEMP && allowOverheat) {
			world.removeBlock(pos, false);
			world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 4F, true, Level.ExplosionInteraction.BLOCK);
		}
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public boolean canProvidePower() {
		return true;
	}

	@Override
	public PowerSourceList getPowerSources(PowerSourceTracker io, ShaftMerger caller) {
		PowerSourceList psl = new PowerSourceList();
		if (power > 0)
			psl.addSource(this);
		return psl;
	}

	@Override
	public void getAllOutputs(Collection<BlockEntity> c, Direction dir) {
		c.add(this.getAdjacentTileEntity(write));
	}

	@Override
	public long getMaxPower() {
		return (long)GEN_OMEGA*GEN_TORQUE;
	}

	@Override
	public long getCurrentPower() {
		return power;
	}

	@Override
	public BlockPos getEmittingPos() {
		return worldPosition.relative(write == null ? Direction.WEST : write);
	}

	@Override
	public int getRedstoneOverride() {
		return 15*fuel.getFluidAmount()/CAPACITY;
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe() || p == PipeType.HOSE || p == PipeType.FUEL_LINE;
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p) && side != write;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side == write ? Flow.NONE : Flow.INPUT;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack fs, IFluidHandler.FluidAction action) {
		if (fs.is(RotaryFluids.LUBRICANT.get()))
			return lube.fill(fs, action);
		if (fs.is(Fluids.WATER))
			return water.fill(fs, action);
		return isFuel(fs.getFluid()) ? fuel.fill(fs, action) : 0;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		return FluidStack.EMPTY;
	}

	private static FluidStack read(CompoundTag NBT, String key, Fluid fallback, HolderLookup.Provider registries) {
		if (!NBT.contains(key))
			return FluidStack.EMPTY;
		CompoundTag t = NBT.getCompound(key);
		Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(t.getString("id")));
		int amt = t.getInt("amt");
		return amt > 0 && f != Fluids.EMPTY ? new FluidStack(f, amt) : FluidStack.EMPTY;
	}

	private static CompoundTag write(FluidTank t) {
		CompoundTag c = new CompoundTag();
		c.putString("id", BuiltInRegistries.FLUID.getKey(t.getFluid().getFluid()).toString());
		c.putInt("amt", t.getFluidAmount());
		return c;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("fuel", write(fuel));
		NBT.put("water", write(water));
		NBT.put("lube", write(lube));
		NBT.putInt("temp", temperature);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		fuel.setFluid(read(NBT, "fuel", null, registries));
		water.setFluid(read(NBT, "water", null, registries));
		lube.setFluid(read(NBT, "lube", null, registries));
		temperature = NBT.contains("temp") ? NBT.getInt("temp") : Integer.MIN_VALUE;
	}
}
