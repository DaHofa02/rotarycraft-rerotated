/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.Comparator;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.lib.world.AmbientTemperature;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * The boiler, written for the port from the original's behaviour: shaft power from below heats it (0.3125*log2(power) C a second, falling
 * back towards ambient by 1/40 of the difference) and is stored as heat; above 100 C it turns water (in from the sides, 27000 mB) into
 * steam of another mod (the first fluid in c:steam), 8 mB of steam per mB of water, as far as the stored heat pays for it
 * (4.18 J per mB and C up to 100 C plus 2260 J per mB). The steam goes out at the top, into a tank or pipe above. Snow and ice next to it
 * melt above 50 C. Without any c:steam fluid installed it makes nothing.
 * Changes: own energy model (the original used Railcraft's steam table).
 */
public class BoilerBlockEntity extends PowerReceiverBlockEntity implements PipeConnector {

	public static final int CAPACITY = 27000;
	public static final int MAXTEMP = 500;
	public static final int STEAM_PER_WATER = 8;
	/** Energy one mB of steam carries: water from 25 C boiled (4.18*75 + 2260 J) spread over 8 mB of steam. */
	public static final double JOULES_PER_STEAM = (4.18*75+2260)/STEAM_PER_WATER;
	public static final TagKey<Fluid> STEAM = FluidTags.create(ResourceLocation.fromNamespaceAndPath("c", "steam"));

	private final FluidTank water = new FluidTank(CAPACITY, fs -> fs.is(Fluids.WATER));
	private final FluidTank steam = new FluidTank(CAPACITY*STEAM_PER_WATER);
	private int temperature = Integer.MIN_VALUE;
	private double heat;
	public long steamMade;

	public BoilerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.BOILER.get(), pos, state, 1, 1, 1);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public static boolean isSteam(Fluid f) {
		return f != Fluids.EMPTY && f.is(STEAM);
	}

	/** The steam fluid the boiler makes: the source fluid of the first c:steam entry by registry name, or null. */
	@Nullable
	public static Fluid steamFluid() {
		return BuiltInRegistries.FLUID.getTag(STEAM).flatMap(set -> set.stream().map(Holder::value)
				.map(f -> f instanceof FlowingFluid ff ? ff.getSource() : f)
				.min(Comparator.comparing(f -> BuiltInRegistries.FLUID.getKey(f).toString()))).orElse(null);
	}

	public int getTemperature() {
		return temperature;
	}

	public void setTemperature(int t) {
		temperature = t;
	}

	public FluidTank getWaterTank() {
		return water;
	}

	public FluidTank getSteamTank() {
		return steam;
	}

	/** Heat (J) to turn 1 mB of water at the ambient temperature into steam. */
	public double joulesPerMB(int ambient) {
		return 4.18*Math.max(0, 100-ambient)+2260;
	}

	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 2;
			}

			@Override
			public FluidStack getFluidInTank(int i) {
				return (i == 0 ? water : steam).getFluid();
			}

			@Override
			public int getTankCapacity(int i) {
				return (i == 0 ? water : steam).getCapacity();
			}

			@Override
			public boolean isFluidValid(int i, FluidStack fs) {
				return i == 0 && fs.is(Fluids.WATER);
			}

			@Override
			public int fill(FluidStack fs, FluidAction action) {
				return side != null && side.getAxis() == Direction.Axis.Y ? 0 : water.fill(fs, action);
			}

			@Override
			public FluidStack drain(FluidStack fs, FluidAction action) {
				return side == null || side == Direction.UP ? steam.drain(fs, action) : FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				return side == null || side == Direction.UP ? steam.drain(max, action) : FluidStack.EMPTY;
			}
		};
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		read = Direction.DOWN;
		this.readPower();
		int amb = AmbientTemperature.getAmbientTemperatureAt(world, pos);
		if (temperature == Integer.MIN_VALUE)
			temperature = amb;
		if (this.getTicksExisted()%20 == 0)
			this.updateTemperature(world, pos, amb);
		if (temperature > 100)
			heat += power/20D;
		Fluid out = steamFluid();
		if (out != null && temperature > 100 && !water.isEmpty()) {
			int space = (steam.getCapacity()-steam.getFluidAmount())/STEAM_PER_WATER;
			if (!steam.isEmpty() && !steam.getFluid().is(out))
				space = 0;
			int mb = (int)Math.min(Math.min(space, water.getFluidAmount()), heat/this.joulesPerMB(amb));
			if (mb > 0) {
				water.drain(mb, IFluidHandler.FluidAction.EXECUTE);
				steam.fill(new FluidStack(out, mb*STEAM_PER_WATER), IFluidHandler.FluidAction.EXECUTE);
				heat -= mb*this.joulesPerMB(amb);
				steamMade += mb*STEAM_PER_WATER;
			}
		}
		heat = Math.min(heat, 1e9);
		if (!steam.isEmpty()) {
			IFluidHandler up = world.getCapability(Capabilities.FluidHandler.BLOCK, pos.above(), Direction.DOWN);
			if (up != null) {
				int n = up.fill(steam.getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
				if (n > 0)
					steam.drain(n, IFluidHandler.FluidAction.EXECUTE);
			}
		}
	}

	private void updateTemperature(Level world, BlockPos pos, int amb) {
		if (power > 0)
			temperature += (int)(0.3125*ReikaMathLibrary.logbase(power, 2));
		temperature -= (temperature-amb)/40;
		if (temperature > amb && temperature-amb <= 40)
			temperature--;
		if (temperature < amb && amb-temperature <= 40)
			temperature++;
		temperature = Math.min(temperature, MAXTEMP);
		if (temperature > 50)
			for (Direction d : Direction.values()) {
				BlockState s = world.getBlockState(pos.relative(d));
				if (s.is(Blocks.SNOW) || s.is(Blocks.SNOW_BLOCK))
					world.setBlockAndUpdate(pos.relative(d), Blocks.AIR.defaultBlockState());
				else if (s.is(Blocks.ICE))
					world.setBlockAndUpdate(pos.relative(d), Blocks.WATER.defaultBlockState());
			}
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return steam.getFluidAmount() >= steam.getCapacity() || water.isEmpty() ? 15 : 0;
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p) && side != Direction.DOWN;
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side == Direction.UP ? Flow.OUTPUT : side == Direction.DOWN ? Flow.NONE : Flow.INPUT;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack fs, IFluidHandler.FluidAction action) {
		return from != null && from.getAxis() == Direction.Axis.Y ? 0 : water.fill(fs, action);
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		return from == null || from == Direction.UP ? steam.drain(maxDrain, action) : FluidStack.EMPTY;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("water", water.getFluidAmount());
		NBT.putInt("steam", steam.getFluidAmount());
		NBT.putInt("temp", temperature);
		NBT.putDouble("heat", heat);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		int w = NBT.getInt("water");
		water.setFluid(w > 0 ? new FluidStack(Fluids.WATER, w) : FluidStack.EMPTY);
		Fluid sf = steamFluid();
		int st = NBT.getInt("steam");
		steam.setFluid(st > 0 && sf != null ? new FluidStack(sf, st) : FluidStack.EMPTY);
		temperature = NBT.contains("temp") ? NBT.getInt("temp") : Integer.MIN_VALUE;
		heat = NBT.getDouble("heat");
	}
}
