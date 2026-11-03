/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.reactor;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.reactorcraft.content.entity.NeutronEntity;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.math.RandomHelper;

/**
 * Original: Reika.ReactorCraft.TileEntities.Fission.Thorium.TileEntityThoriumCore - a liquid-fuelled core: LiFBe fuel (or preheated fuel)
 * comes in at the top (fuel lines, 4000 mB), hot LiFBe leaves at the bottom (4000 mB), liquid waste (1000 mB) leaves at the sides into a
 * waste pipe. It never emits neutrons by itself. A fission neutron (not a breeder one) reacts with a chance that peaks in the middle
 * of 400-1200 C (cosine), is lost to poisoning with 87.5 % * (waste fill)^1.6, and then with 50 % down to 10 % (by temperature) burns
 * 100 mB fuel into hot LiFBe: +50 C, 3 thorium neutrons, 5 % chance of 50 mB liquid waste. Below 400 C nothing reacts. It cools faster
 * towards its resting temperature (ambient, 250 C with preheated fuel) when hotter and slower when colder; it passes heat to other
 * reactor types at a quarter and to fission cores in halves, and boilers and fuel cores take its heat at 75 % / 100 % (12.5 % / 20 %
 * near resting temperature). Cores beside each other level their tanks; a stack passes its liquids down. From 1100 C a fuel dump
 * below drains its fuel.
 */
public class ThoriumCoreBlockEntity extends NuclearCoreBlockEntity implements PipeConnector {

	private static final int CYCLE_AMOUNT = 100;
	public static final int FUEL_DUMP_TEMPERATURE = 1100;
	public static final int MIN_TEMPERATURE = 400;

	private final FluidTank fuelTank = new FluidTank(4000, fs -> isFuel(fs.getFluid()));
	private final FluidTank fuelTankOut = new FluidTank(4000);
	private final FluidTank wasteTank = new FluidTank(1000);
	private int timer2;

	public ThoriumCoreBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.THORIUM_CORE.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Thorium Reactor Core";
	}

	private static boolean isFuel(Fluid f) {
		return f.isSame(ReactorFluids.get("lifbe_fuel")) || f.isSame(ReactorFluids.get("lifbe_fuel_preheat"));
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		super.updateEntity(world, pos, state);
		if (this.isRemoved() || world.isClientSide)
			return;
		timer2++;
		if (timer2 >= 5) {
			timer2 = 0;
			for (Direction dir : Direction.Plane.HORIZONTAL)
				if (world.getBlockEntity(pos.relative(dir)) instanceof ThoriumCoreBlockEntity te)
					this.balanceLiquidsWith(te);
		}
		this.feedFluid(world, pos);
	}

	private void balanceLiquidsWith(ThoriumCoreBlockEntity te) {
		balanceTanks(wasteTank, te.wasteTank);
		balanceTanks(fuelTank, te.fuelTank);
		balanceTanks(fuelTankOut, te.fuelTankOut);
	}

	private static void balanceTanks(FluidTank from, FluidTank to) {
		if (!to.isEmpty() && !to.getFluid().getFluid().isSame(from.getFluid().getFluid()))
			return;
		int dl = from.getFluidAmount()-to.getFluidAmount();
		if (dl > 1) {
			int amt = Math.min(from.getFluidAmount()/4, Math.max(1, dl/8+1));
			if (amt > 0) {
				to.fill(new FluidStack(from.getFluid().getFluid(), amt), IFluidHandler.FluidAction.EXECUTE);
				from.drain(amt, IFluidHandler.FluidAction.EXECUTE);
			}
		}
	}

	/** Original feedFluid: each tank passes what fits into the same tank of the core below. */
	private void feedFluid(Level world, BlockPos pos) {
		if (world.getBlockEntity(pos.below()) instanceof ThoriumCoreBlockEntity tile) {
			fuelTank.drain(tile.feedFluidIn(fuelTank.getFluid(), tile.fuelTank), IFluidHandler.FluidAction.EXECUTE);
			fuelTankOut.drain(tile.feedFluidIn(fuelTankOut.getFluid(), tile.fuelTankOut), IFluidHandler.FluidAction.EXECUTE);
			wasteTank.drain(tile.feedFluidIn(wasteTank.getFluid(), tile.wasteTank), IFluidHandler.FluidAction.EXECUTE);
		}
	}

	private int feedFluidIn(FluidStack is, FluidTank tank) {
		if (is.isEmpty())
			return 0;
		if (!tank.isEmpty() && !tank.getFluid().getFluid().isSame(is.getFluid()))
			return 0;
		int add = Math.min(tank.getSpace(), is.getAmount());
		tank.fill(new FluidStack(is.getFluid(), add), IFluidHandler.FluidAction.EXECUTE);
		return add;
	}

	/** For the fuel dump below: takes up to max mB of fuel out. */
	public int dumpFuel(int max) {
		int amt = Math.min(max, fuelTank.getFluidAmount());
		if (amt > 0)
			fuelTank.drain(amt, IFluidHandler.FluidAction.EXECUTE);
		return amt;
	}

	@Override
	protected int getRestingTemperature(Level world, BlockPos pos) {
		return fuelTank.getFluid().getFluid().isSame(ReactorFluids.get("lifbe_fuel_preheat")) ? 250 : super.getRestingTemperature(world, pos);
	}

	@Override
	protected int getAmbientHeatLossFactor(int base, int Tamb) {
		return Tamb < temperature ? base*4 : base/2;
	}

	@Override
	protected int getDecayNeutronChance() {
		return 30;
	}

	@Override
	protected int getWarningTemperature() {
		return 900;
	}

	@Override
	protected float getHeatConductionThroughput(ReactorBlockEntity other) {
		if (other instanceof ReactorTyped t && t.getReactorType() != ReactorType.THORIUM)
			return 0.25F;
		return super.getHeatConductionThroughput(other);
	}

	@Override
	protected int getHeatConductionFraction(ReactorBlockEntity other) {
		return other instanceof ReactorTyped t && t.getReactorType() == ReactorType.FISSION ? 2 : super.getHeatConductionFraction(other);
	}

	@Override
	protected float getHeatConductionEfficiency(ReactorBlockEntity other) {
		boolean rest = level != null && temperature-this.getRestingTemperature(level, worldPosition) < 50;
		if (other instanceof ReactorBoilerBlockEntity)
			return rest ? 0.125F : 0.75F;
		if (other instanceof FuelRodBlockEntity)
			return rest ? 0.2F : 1F;
		return super.getHeatConductionEfficiency(other);
	}

	@Override
	public boolean canDumpHeatInto(LiquidStates liq) {
		return liq == LiquidStates.LITHIUM;
	}

	@Override
	protected boolean isItemValidForSlot(int i, ItemStack is) {
		return false;
	}

	@Override
	protected boolean canRemoveItem(int slot, ItemStack is) {
		return false;
	}

	@Override
	public boolean onNeutron(NeutronEntity e, Level world, BlockPos pos) {
		super.onNeutron(e, world, pos);
		if (world.isClientSide)
			return false;
		if (e.getNeutronType().canTriggerFission(rand) && chance(e.getNeutronSpeed().getInteractionMultiplier(), rand)
				&& e.getNeutronType() != NeutronEntity.NeutronType.BREEDER && RandomHelper.doWithChance(this.getNeutronInteractionChance(), rand)) {
			if (this.checkPoisonedChance())
				return true;
			if (chance(this.getNeutronChance(), rand) && this.hasFuel()) {
				fuelTank.drain(CYCLE_AMOUNT, IFluidHandler.FluidAction.EXECUTE);
				fuelTankOut.fill(new FluidStack(ReactorFluids.get("hot_lifbe"), CYCLE_AMOUNT), IFluidHandler.FluidAction.EXECUTE);
				temperature += 50;
				this.spawnNeutronBurst(world, pos);
				if (chance(5, rand))
					this.addWaste();
				this.setChanged();
			}
			return true;
		}
		return false;
	}

	/** Original getNeutronInteractionChance (a fraction): (1-f) + f * cosine bump over 400-1200 C, f 0.5 below the middle, 0.75 above. */
	public double getNeutronInteractionChance() {
		int min = MIN_TEMPERATURE, max = this.getMaxTemperature();
		int midT = (min+max)/2;
		double f = temperature <= midT ? 0.5 : 0.75;
		return (1-f)+f*cosInterpolation(min, max, temperature);
	}

	/** Original ReikaMathLibrary.cosInterpolation. */
	private static double cosInterpolation(double min, double max, double val) {
		if (val < min || val > max)
			return 0;
		double size = (max-min)/2D;
		double mid = min+size;
		if (val == mid)
			return 1;
		return 0.5+0.5*Math.cos(Math.toRadians((val-mid)/size*180));
	}

	/** Original getNeutronChance (percent): 50 at 400 C down to 10 at 1200 C; NaN (never) below 400 C. */
	public double getNeutronChance() {
		return 50-40*Math.sqrt((temperature-MIN_TEMPERATURE)/(double)(this.getMaxTemperature()-MIN_TEMPERATURE));
	}

	public boolean hasFuel() {
		return fuelTank.getFluidAmount() >= CYCLE_AMOUNT;
	}

	@Override
	protected boolean checkPoisonedChance() {
		return RandomHelper.doWithChance(0.875*Math.pow(wasteTank.getFluidAmount()/(double)wasteTank.getCapacity(), 1.6), rand);
	}

	@Override
	protected void addWaste() {
		wasteTank.fill(new FluidStack(ReactorFluids.get("nuclear_waste"), CYCLE_AMOUNT/2), IFluidHandler.FluidAction.EXECUTE);
	}

	@Override
	public boolean isFissile() {
		return false;
	}

	@Override
	public int getMaxTemperature() {
		return 1200;
	}

	@Override
	public ReactorType getReactorType() {
		return ReactorType.THORIUM;
	}

	public FluidTank getFuelTank() {
		return fuelTank;
	}

	public FluidTank getOutputTank() {
		return fuelTankOut;
	}

	public FluidTank getWasteTank() {
		return wasteTank;
	}

	private boolean isWastePipe(@Nullable Direction from) {
		return from != null && level != null && level.getBlockEntity(worldPosition.relative(from)) instanceof reika.reactorcraft.waste.WastePipeBlockEntity;
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe() || p == PipeType.FUEL_LINE;
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return side == Direction.UP ? p == PipeType.FUEL_LINE : p.isStandardPipe();
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side == Direction.UP ? Flow.INPUT : Flow.OUTPUT;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		if (from != Direction.UP || !isFuel(resource.getFluid()))
			return 0;
		int n = fuelTank.fill(resource, action);
		if (n > 0 && action.execute())
			this.setChanged();
		return n;
	}

	/** Original drain: the sides give waste into a waste pipe (nothing otherwise), the bottom gives hot LiFBe. */
	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (from == null || from == Direction.UP)
			return FluidStack.EMPTY;
		FluidStack d = from.getAxis() != Direction.Axis.Y ? this.isWastePipe(from) ? wasteTank.drain(maxDrain, action) : FluidStack.EMPTY : fuelTankOut.drain(maxDrain, action);
		if (!d.isEmpty() && action.execute())
			this.setChanged();
		return d;
	}

	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 3;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return (t == 0 ? fuelTank : t == 1 ? fuelTankOut : wasteTank).getFluid();
			}

			@Override
			public int getTankCapacity(int t) {
				return t == 2 ? 1000 : 4000;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return t == 0 && isFuel(stack.getFluid());
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return ThoriumCoreBlockEntity.this.fill(side, resource, action);
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				FluidStack can = ThoriumCoreBlockEntity.this.drain(side, resource.getAmount(), FluidAction.SIMULATE);
				if (can.isEmpty() || !can.getFluid().isSame(resource.getFluid()))
					return FluidStack.EMPTY;
				return ThoriumCoreBlockEntity.this.drain(side, resource.getAmount(), action);
			}

			@Override
			public FluidStack drain(int max, FluidAction action) {
				return ThoriumCoreBlockEntity.this.drain(side, max, action);
			}
		};
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("fuel", fuelTank.writeToNBT(registries, new CompoundTag()));
		NBT.put("fuelout", fuelTankOut.writeToNBT(registries, new CompoundTag()));
		NBT.put("waste", wasteTank.writeToNBT(registries, new CompoundTag()));
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		fuelTank.setFluid(FluidStack.EMPTY);
		fuelTank.readFromNBT(registries, NBT.getCompound("fuel"));
		fuelTankOut.setFluid(FluidStack.EMPTY);
		fuelTankOut.readFromNBT(registries, NBT.getCompound("fuelout"));
		wasteTank.setFluid(FluidStack.EMPTY);
		wasteTank.readFromNBT(registries, NBT.getCompound("waste"));
	}
}
