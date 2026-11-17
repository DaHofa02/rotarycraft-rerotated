/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft design). Private NeoForge 1.21.1 port; not for distribution.
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.api.power.ShaftPowerEmitter;
import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.power.SimpleProvider;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryFluids;

/**
 * The fixed-ratio "advanced gears" of RotaryCraft, written for the port from the original's behaviour:
 * <ul>
 * <li><b>Worm gear</b>: 64:1 reduction (torque x64, speed /64) with friction losses - the speed comes out at
 * (128 - 4*log2(input speed))% of the ideal value. Needs no lubricant.</li>
 * <li><b>High-ratio gear</b>: 256:1, either as a torque multiplier (torque x256, speed /256, the default) or, after a screwdriver click while
 * sneaking, as a speed multiplier (speed x256, torque /256). It needs lubricant (20 B tank, filled by hose): on four ticks out of eight
 * while it turns it uses log2(max(speed, torque)) mB, and without lubricant nothing comes out.</li>
 * <li><b>Industrial coil</b> (and its bedrock version): stores the energy put into it while it has no redstone signal - but only if the
 * input reaches the charging torque and power, which grow with the stored energy: power CEIL2((log2 E)^4), torque CEIL2((log2 E)^3)/2
 * (E in joules = stored W*ticks / 20; the bedrock coil needs at least 16 Nm). With a redstone signal it gives the speed and torque set in its
 * GUI (speed at most 1024 rad/s, bedrock 4096; torque at most the next "pseudo power of two" above sqrt(E)/4) until it is empty. Above
 * 720 MJ (bedrock 240 TJ) it explodes. Its comparator output follows the charge.</li>
 * </ul>
 * Outputs above the global torque/speed limit are capped, with crit particles and a blaze-hit sound. Input from a shaft, a power provider,
 * a complex IO machine, a shaft power emitter or a splitter that is not splitting; split or cross-shaft inputs give nothing.
 */
public class AdvancedGearBlockEntity extends OneDTransmitterBlockEntity implements PipeConnector, reika.rotarycraft.api.power.PowerGenerator {

	public enum Kind {
		WORM(64),
		HIGH(256),
		COIL(1),
		BEDROCK_COIL(1);

		public final int ratio;

		Kind(int r) {
			ratio = r;
		}

		public boolean needsLubricant() {
			return this == HIGH;
		}

		public boolean storesEnergy() {
			return this == COIL || this == BEDROCK_COIL;
		}
	}

	public static final int LUBE_CAPACITY = 20000;

	private long energy;
	private int releaseTorque;
	private int releaseOmega;
	private boolean releasing;

	public final Kind kind;
	public boolean torquemode = true;
	private final FluidTank lubricant = new FluidTank(LUBE_CAPACITY, fs -> fs.is(RotaryFluids.LUBRICANT.get()));

	public AdvancedGearBlockEntity(BlockPos pos, BlockState state) {
		this(pos, state, state.getBlock() instanceof reika.rotarycraft.content.block.AdvancedGearBlock b ? b.kind : Kind.WORM);
	}

	public AdvancedGearBlockEntity(BlockPos pos, BlockState state, Kind kind) {
		super(RotaryBlockEntities.ADVANCED_GEAR.get(), pos, state);
		this.kind = kind;
		ratio = kind.ratio;
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public int getLubricant() {
		return lubricant.getFluidAmount();
	}

	public void addLubricant(int amt) {
		lubricant.fill(new FluidStack(RotaryFluids.LUBRICANT.get(), amt), IFluidHandler.FluidAction.EXECUTE);
	}

	/** Worm gear efficiency at this input speed, 1 = lossless. */
	public static double wormEfficiency(int speed) {
		return speed <= 0 ? 0 : (128-4*ReikaMathLibrary.logbase(speed, 2))/100D;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSides(world, pos, this.getBlockMetadata()%4, false);
		if (kind.storesEnergy())
			this.store(world, pos);
		else
			this.transferPower(world, pos, this.getBlockMetadata());
		power = (long)omega*(long)torque;
	}

	// ---- industrial coil ----

	/** Smallest power of two >= v. */
	public static int ceilPow2(int v) {
		return v <= 1 ? Math.max(v, 0) == 0 ? 0 : 1 : Integer.highestOneBit(v-1) << 1;
	}

	/** Rounds up to the next value of 1, 2, 3, 4, 6, 8, 12, 16, 24, ... (powers of two and one and a half times them). */
	public static int ceilPseudoPow2(int v) {
		int pow = ceilPow2(v);
		int prev = pow/2;
		if (prev >= v)
			return prev;
		return prev*3/2 >= v ? prev*3/2 : pow;
	}

	public long getMaxStorage() {
		return kind == Kind.BEDROCK_COIL ? 240_000_000_000_000L : 720_000_000L;
	}

	public int getMaximumEmission() {
		return kind == Kind.BEDROCK_COIL ? 4096 : 1024;
	}

	public long getEnergy() {
		return energy;
	}

	public void setEnergy(long e) {
		energy = Math.max(0, e);
	}

	private int log2Joules(long div) {
		return (int)(63-Long.numberOfLeadingZeros(Math.max(1, energy/div)));
	}

	public long getChargingPower() {
		return energy >= 20 ? ceilPow2((int)Math.min(Integer.MAX_VALUE, (long)Math.pow(this.log2Joules(20), 4))) : 1;
	}

	public int getChargingTorque() {
		int base = energy >= 20 ? ceilPow2((int)Math.pow(this.log2Joules(20), 3))/2 : 1;
		if (kind == Kind.BEDROCK_COIL)
			base = Math.max(base, energy >= 20 ? 16*ceilPow2((int)Math.pow(this.log2Joules(80), 3))/2 : 16);
		return Math.max(base, kind == Kind.BEDROCK_COIL ? 16 : 1);
	}

	public int getTorqueCap() {
		return ceilPseudoPow2((int)Math.ceil(Math.sqrt(energy/20D)/4));
	}

	public int getReleaseTorque() {
		return releaseTorque;
	}

	public int getReleaseOmega() {
		return releaseOmega;
	}

	public void setReleaseTorque(int t) {
		releaseTorque = Math.max(0, Math.min(this.getTorqueCap(), Math.min(this.getMaximumEmission(), t)));
		this.syncAllData();
	}

	public void setReleaseOmega(int w) {
		releaseOmega = Math.max(0, Math.min(this.getMaximumEmission(), w));
		this.syncAllData();
	}

	public boolean isReleasing() {
		return releasing;
	}

	private void store(Level world, BlockPos pos) {
		if (world.isClientSide)
			return;
		this.readInput();
		releasing = world.hasNeighborSignal(pos);
		if (this.isOvercharged()) {
			this.overcharge(world, pos);
			return;
		}
		omega = torque = 0;
		if (!releasing) {
			long pwr = (long)torquein*(long)omegain;
			if (pwr > 0 && torquein >= this.getChargingTorque() && pwr >= this.getChargingPower())
				energy += pwr;
		}
		else if (energy > 0 && releaseTorque > 0 && releaseOmega > 0) {
			releaseTorque = Math.min(releaseTorque, this.getTorqueCap());
			torque = releaseTorque;
			omega = releaseOmega;
			energy = Math.max(0, energy-(long)torque*(long)omega);
			if (this.getTicksExisted()%26 == 0)
				world.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.6F, 0.5F);
		}
	}

	/** Above its capacity the coil explodes on its next tick. */
	public boolean isOvercharged() {
		return kind.storesEnergy() && energy/20 >= this.getMaxStorage();
	}

	private void overcharge(Level world, BlockPos pos) {
		world.removeBlock(pos, false);
		boolean bed = kind == Kind.BEDROCK_COIL;
		int n = bed ? 24 : 3, r = bed ? 9 : 1;
		for (int i = 0; i < n; i++)
			world.explode(null, pos.getX()+world.random.nextInt(2*r+1)-r, pos.getY()+world.random.nextInt(2*r+1)-r, pos.getZ()+world.random.nextInt(2*r+1)-r, 8, Level.ExplosionInteraction.BLOCK);
		world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, bed ? 12 : 8, Level.ExplosionInteraction.BLOCK);
	}

	@Override
	public long getMaxPower() {
		return kind.storesEnergy() && power > 0 ? (long)releaseOmega*releaseTorque : 0;
	}

	@Override
	public long getCurrentPower() {
		return power;
	}

	@Override
	public BlockPos getEmittingPos() {
		return write == null ? worldPosition : worldPosition.relative(write);
	}

	@Override
	public reika.rotarycraft.content.power.PowerSourceList getPowerSources(reika.rotarycraft.content.power.PowerSourceTracker io, reika.rotarycraft.api.power.ShaftMerger caller) {
		return kind.storesEnergy() ? new reika.rotarycraft.content.power.PowerSourceList().addSource(this) : super.getPowerSources(io, caller);
	}

	private void readInput() {
		omegain = torquein = 0;
		BlockEntity te = this.getAdjacentTileEntity(read);
		if (!this.isProvider(te))
			return;
		if (te instanceof ShaftBlockEntity sh) {
			if (!sh.isCross() && sh.isWritingTo(this)) {
				torquein = sh.torque;
				omegain = sh.omega;
			}
		}
		else if (te instanceof SplitterBlockEntity spl) {
			if (!spl.isSplitting() && spl.isWritingTo(this)) {
				torquein = spl.torque;
				omegain = spl.omega;
			}
		}
		else if (te instanceof SimpleProvider)
			this.copyStandardPower(te);
		else if (te instanceof reika.rotarycraft.api.interfaces.ComplexIO pwr) {
			Direction dir = read.getOpposite();
			omegain = pwr.getSpeedToSide(dir);
			torquein = pwr.getTorqueToSide(dir);
		}
		else if (te instanceof ShaftPowerEmitter sp && sp.isEmitting() && sp.canWriteTo(read.getOpposite())) {
			torquein = sp.getTorque();
			omegain = sp.getOmega();
		}
	}

	@Override
	protected void transferPower(Level world, BlockPos pos, int meta) {
		if (world.isClientSide || read == null)
			return;
		this.readInput();
		omega = torque = 0;
		if (omegain <= 0 || torquein <= 0)
			return;
		int lim = RotaryConfig.torquelimit;
		switch (kind) {
			case WORM -> {
				omega = (int)(omegain/kind.ratio*wormEfficiency(omegain));
				torque = this.multiply(world, pos, torquein, kind.ratio, lim);
			}
			case HIGH -> {
				if (lubricant.isEmpty())
					return;
				if (torquemode) {
					torque = this.multiply(world, pos, torquein, kind.ratio, lim);
					omega = omegain/kind.ratio;
				}
				else {
					omega = this.multiply(world, pos, omegain, kind.ratio, lim);
					torque = torquein/kind.ratio;
				}
				if (omega > 0 && (world.getGameTime() & 4) == 4)
					lubricant.drain((int)ReikaMathLibrary.logbase(Math.max(omega, torque), 2), IFluidHandler.FluidAction.EXECUTE);
			}
		}
	}

	private int multiply(Level world, BlockPos pos, int value, int factor, int lim) {
		if (value <= lim/factor)
			return value*factor;
		if (world instanceof ServerLevel sl)
			sl.sendParticles(ParticleTypes.CRIT, pos.getX()+world.random.nextFloat(), pos.getY()+world.random.nextFloat(), pos.getZ()+world.random.nextFloat(), 1, 0, 0, 0, 0.5);
		world.playSound(null, pos, SoundEvents.BLAZE_HURT, SoundSource.BLOCKS, 0.1F, 1F);
		return lim;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public int getRedstoneOverride() {
		return kind.storesEnergy() ? (int)Math.min(15, 15*((energy+power)/20)/this.getMaxStorage()) : 0;
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return kind.needsLubricant() && p == PipeType.HOSE;
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p);
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return kind.needsLubricant() ? Flow.INPUT : Flow.NONE;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		return kind.needsLubricant() ? lubricant.fill(resource, action) : 0;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		return FluidStack.EMPTY;
	}

	/** Capability: lubricant in (high-ratio gear only), nothing out. */
	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 1;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return lubricant.getFluid();
			}

			@Override
			public int getTankCapacity(int t) {
				return LUBE_CAPACITY;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return kind.needsLubricant() && stack.is(RotaryFluids.LUBRICANT.get());
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return AdvancedGearBlockEntity.this.fill(side, resource, action);
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

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("trq", torquemode);
		NBT.putLong("energy", energy);
		NBT.putInt("rtorque", releaseTorque);
		NBT.putInt("romega", releaseOmega);
		NBT.putInt("lube", lubricant.getFluidAmount());
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		torquemode = !NBT.contains("trq") || NBT.getBoolean("trq");
		energy = NBT.getLong("energy");
		releaseTorque = NBT.getInt("rtorque");
		releaseOmega = NBT.getInt("romega");
		lubricant.setFluid(NBT.getInt("lube") > 0 ? new FluidStack(RotaryFluids.LUBRICANT.get(), NBT.getInt("lube")) : FluidStack.EMPTY);
	}
}
