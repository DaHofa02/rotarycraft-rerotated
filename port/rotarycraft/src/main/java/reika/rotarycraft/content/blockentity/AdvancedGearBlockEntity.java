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
 * </ul>
 * Outputs above the global torque/speed limit are capped, with crit particles and a blaze-hit sound. Input from a shaft, a power provider,
 * a complex IO machine, a shaft power emitter or a splitter that is not splitting; split or cross-shaft inputs give nothing.
 */
public class AdvancedGearBlockEntity extends OneDTransmitterBlockEntity implements PipeConnector {

	public enum Kind {
		WORM(64),
		HIGH(256);

		public final int ratio;

		Kind(int r) {
			ratio = r;
		}

		public boolean needsLubricant() {
			return this == HIGH;
		}
	}

	public static final int LUBE_CAPACITY = 20000;

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
		this.transferPower(world, pos, this.getBlockMetadata());
		power = (long)omega*(long)torque;
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
		return 0;
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
		NBT.putInt("lube", lubricant.getFluidAmount());
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		torquemode = !NBT.contains("trq") || NBT.getBoolean("trq");
		lubricant.setFluid(NBT.getInt("lube") > 0 ? new FluidStack(RotaryFluids.LUBRICANT.get(), NBT.getInt("lube")) : FluidStack.EMPTY);
	}
}
