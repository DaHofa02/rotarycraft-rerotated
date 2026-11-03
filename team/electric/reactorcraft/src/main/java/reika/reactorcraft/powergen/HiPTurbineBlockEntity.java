/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.powergen;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import reika.reactorcraft.content.block.TurbineMultiBlock;
import reika.reactorcraft.reactor.HydrogenExplosion;
import reika.reactorcraft.reactor.ReactorType;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.registry.ReactorFluids;
import reika.rotarycraft.content.blockentity.ReservoirBlockEntity;
import reika.rotarycraft.registry.RotaryFluids;

/**
 * Original: Reika.ReactorCraft.TileEntities.PowerGen.TileEntityHiPTurbine - the high pressure turbine: up to 7 stages inside the
 * turbine multiblock (it only runs while the multiblock is formed; losing it above 2048 rad/s blows the core apart). The first stage
 * takes steam from the steam line behind it: an eighth (+1) of what the line holds, weighted by where the steam came from (fission and
 * breeder 1, fusion 1.5, HTGR 0.35, solar 0.15), up to 3250; it accelerates while the line offers well more than it takes. Up to
 * 131072 rad/s and 65536 Nm (131072 with ammonia); torque factor rises as a cosine up to half full and +25 % per efficiency point above 1
 * (ammonia); stage efficiencies 1.25 / 2.5 / 7.5 / 12.5 / 25 / 50 / 100 %. Lubricant (128000 mB; 100 mB per use) comes from the steam
 * injectors around the inlet; a redstone signal on an injector stops it. The last stage lets the condensed steam drip as low pressure
 * water (or ammonia) into RotaryCraft reservoirs up to 4 blocks below the housing (32 mB per drip). No blade collision checks.
 */
public class HiPTurbineBlockEntity extends TurbineCoreBlockEntity {

	public static final int GEN_OMEGA = 131072;
	public static final int FLUID_PER_RESERVOIR = reika.reactorcraft.reactor.ReactorBoilerBlockEntity.WATER_PER_STEAM*131/20/24*6/10;
	public static final int MAX_STEAM = 3250;

	private WorkingFluid fluid = WorkingFluid.EMPTY;
	private int dripBuffer;

	public HiPTurbineBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.HIP_TURBINE.get(), pos, state);
	}

	@Override
	public String getName() {
		return "High-Pressure Turbine";
	}

	@Override
	public boolean needsMultiblock() {
		return true;
	}

	@Override
	public void setHasMultiBlock(boolean has) {
		if (hasMultiBlock && !has && omega > 2048 && level instanceof ServerLevel sl) {
			sl.removeBlock(worldPosition, false);
			HydrogenExplosion.explode(sl, worldPosition, 4);
			return;
		}
		super.setHasMultiBlock(has);
	}

	@Override
	protected int getLubricantCapacity() {
		return 128000;
	}

	@Override
	public int getMaxTorque() {
		return fluid.efficiency > 1 ? 131072 : 65536;
	}

	@Override
	public int getMaxSpeed() {
		return GEN_OMEGA;
	}

	@Override
	protected int getMaxStage() {
		return 6;
	}

	@Override
	protected boolean canCollideCheck() {
		return false;
	}

	@Override
	protected int getConsumedLubricant() {
		return 100;
	}

	@Override
	protected double getAnimationSpeed() {
		return 0.5F;
	}

	@Override
	protected double getEfficiency() {
		return switch (this.getNumberStagesTotal()) {
			case 1 -> 0.0125;
			case 2 -> 0.025;
			case 3 -> 0.075;
			case 4 -> 0.125;
			case 5 -> 0.25;
			case 6 -> 0.5;
			case 7 -> 1;
			default -> 0;
		};
	}

	@Override
	protected void copyDataFrom(TurbineCoreBlockEntity tile) {
		super.copyDataFrom(tile);
		if (tile instanceof HiPTurbineBlockEntity te) {
			fluid = te.fluid;
			dripBuffer = te.dripBuffer;
			te.dripBuffer = 0;
		}
	}

	/** Original getInjectors: the eight positions around the block behind the first stage. */
	private BlockPos[] getInjectors(BlockPos behind) {
		Direction dir = this.getSteamMovement();
		BlockPos[] li = new BlockPos[8];
		int n = 0;
		for (int a = -1; a <= 1; a++)
			for (int b = -1; b <= 1; b++)
				if (a != 0 || b != 0)
					li[n++] = dir.getAxis() == Direction.Axis.Z ? behind.offset(b, a, 0) : behind.offset(0, a, b);
		return li;
	}

	@Override
	protected void intakeLubricant(Level world, BlockPos pos) {
		if (this.getStage() != 0)
			return;
		for (BlockPos p : this.getInjectors(pos.relative(this.getSteamMovement().getOpposite()))) {
			if (world.getBlockEntity(p) instanceof SteamInjectorBlockEntity te) {
				int rem = Math.min(te.getLubricant(), tank.getSpace());
				if (rem > 0) {
					te.remove(rem);
					tank.fill(new FluidStack(RotaryFluids.LUBRICANT.get(), rem), IFluidHandler.FluidAction.EXECUTE);
				}
			}
		}
	}

	@Override
	protected boolean enabled(Level world, BlockPos pos) {
		if (tank.isEmpty())
			return false;
		for (BlockPos p : this.getInjectors(pos.relative(this.getSteamMovement().getOpposite())))
			if (world.hasNeighborSignal(p))
				return false;
		return super.enabled(world, pos);
	}

	@Override
	protected boolean intakeSteam(Level world, BlockPos pos) {
		BlockPos behind = pos.relative(this.getSteamMovement().getOpposite());
		boolean flag = false;
		if (world.getBlockEntity(behind) instanceof SteamLineBlockEntity te) {
			int s = te.getSteam();
			if (s > 8 && this.canTakeIn(te.getWorkingFluid())) {
				s = this.getEffectiveUsable(s, te.getSourceReactorTypes());
				if (s > 0) {
					int rm = s/8+1;
					if (steam < MAX_STEAM) {
						int rm2 = Math.min(rm, MAX_STEAM-steam);
						steam += rm2;
						fluid = te.getWorkingFluid();
						te.removeSteam(rm2);
						dripBuffer += rm2*1000;
					}
					flag = s > rm+32 && steam >= MAX_STEAM/15;
				}
			}
		}
		else if (world.getBlockEntity(behind) instanceof HiPTurbineBlockEntity te) {
			fluid = te.fluid;
		}
		if (steam == 0)
			fluid = WorkingFluid.EMPTY;
		return flag;
	}

	private int getEffectiveUsable(int s, Map<ReactorType, Double> source) {
		double sum = source.values().stream().mapToDouble(Double::doubleValue).sum();
		if (sum <= 0)
			return 0;
		float ret = 0;
		for (Map.Entry<ReactorType, Double> e : source.entrySet())
			ret += e.getValue()/sum*s*e.getKey().getHPTurbineMultiplier();
		return (int)ret;
	}

	private boolean canTakeIn(WorkingFluid f) {
		return fluid == WorkingFluid.EMPTY || f == fluid;
	}

	@Override
	protected float getTorqueFactor() {
		float base = super.getTorqueFactor();
		if (steam < MAX_STEAM/2) {
			float f = steam/(float)MAX_STEAM; //stops at 0.5, the peak of the cosine
			base *= cosInterpolation(0, 1, f);
		}
		if (fluid.efficiency > 1)
			base *= 1+(fluid.efficiency-1)*0.25F;
		return base;
	}

	/** Original ReikaMathLibrary.cosInterpolation(min, max, val, 0, 1). */
	private static double cosInterpolation(double min, double max, double val) {
		if (val < min || val > max)
			return 0;
		double size = (max-min)/2D;
		double mid = min+size;
		if (val == mid)
			return 1;
		return 0.5+0.5*Math.cos(Math.toRadians((val-mid)/size*180));
	}

	private Fluid getLowPressureFluid() {
		return ReactorFluids.get(fluid == WorkingFluid.AMMONIA ? "low_pressure_ammonia" : "low_pressure_water");
	}

	@Override
	protected void dumpSteam(Level world, BlockPos pos) {
		if (dripBuffer <= 0 || fluid == WorkingFluid.EMPTY || this.getStage() != this.getNumberStagesTotal()-1)
			return;
		Direction s = this.getSteamMovement();
		Direction dir = s.getCounterClockWise();
		int th = (int)this.getRadius();
		if (!world.isClientSide) {
			outer:
			for (int d = 0; d <= 1; d++) {
				for (int dy = 2; dy < 5; dy++) {
					int ty = pos.getY()-th-dy;
					for (int i = -th; i <= th; i++) {
						BlockPos t = new BlockPos(pos.getX()+dir.getStepX()*i+s.getStepX()*d, ty, pos.getZ()+dir.getStepZ()*i+s.getStepZ()*d);
						if (world.getBlockEntity(t) instanceof ReservoirBlockEntity res) {
							res.addLiquid(FLUID_PER_RESERVOIR, this.getLowPressureFluid());
							dripBuffer -= FLUID_PER_RESERVOIR;
							break;
						}
					}
					if (dripBuffer <= 0)
						break outer;
				}
			}
		}
		else {
			double ax = s.getStepX() > 0 ? 1.2 : -0.2;
			double az = s.getStepZ() > 0 ? 1.2 : -0.2;
			int d = -s.getStepX()+s.getStepZ();
			for (int i = 0; i < 4*12; i++) { //original: RotaryCraft "Sprinkler Particle Density" (default 4) * 12
				double px = pos.getX()+(-th+rand.nextDouble()*th*2+d)*dir.getStepX();
				double pz = pos.getZ()+(-th+rand.nextDouble()*th*2+d)*dir.getStepZ();
				world.addParticle(ParticleTypes.RAIN, px+ax, pos.getY()-th+1+rand.nextInt(Math.max(1, th*2)), pz+az, 0, 0, 0);
			}
		}
	}

	/** Original breakBlock: the multiblock around it falls apart. */
	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		for (Direction d : Direction.values())
			TurbineMultiBlock.breakMultiBlock(world, pos.relative(d));
	}

	public WorkingFluid getWorkingFluid() {
		return fluid;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		fluid.saveToNBT(NBT);
		NBT.putInt("dripb", dripBuffer);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		fluid = WorkingFluid.getFromNBT(NBT);
		dripBuffer = NBT.getInt("dripb");
	}
}
