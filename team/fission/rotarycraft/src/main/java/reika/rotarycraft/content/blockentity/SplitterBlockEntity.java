/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.api.interfaces.ComplexIO;
import reika.rotarycraft.api.power.PowerTracker;
import reika.rotarycraft.api.power.ShaftMerger;
import reika.rotarycraft.api.power.ShaftPowerEmitter;
import reika.rotarycraft.content.power.PowerSourceList;
import reika.rotarycraft.content.power.PowerSourceTracker;
import reika.rotarycraft.content.power.SimpleProvider;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryComponents;

/**
 * Original: Reika.RotaryCraft.TileEntities.Transmission.TileEntitySplitter (the Shaft Junction).
 * Meta 0-7 merge two inputs into one output, meta 8-15 split one input into two outputs.
 */
public class SplitterBlockEntity extends TransmissionMachineBlockEntity implements ShaftMerger {

	private int torquein2;
	private int omegain2;
	private int splitmode = 1;
	private boolean processingSecondary;

	public boolean failed;
	private boolean bedrock;

	private int overloadTick = 0;

	public int torqueOut1;
	public int torqueOut2;

	public SplitterBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.SHAFT_JUNCTION.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Shaft Junction";
	}

	public int getRatioFromMode() {
		return splitmode;
	}

	public void setMode(int mode) {
		splitmode = mode;
		this.syncAllData();
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();

		this.getIOSides(this.getBlockMetadata());
		if (failed) {
			omega = torque = 0;
		}
		else {
			this.transferPower(world, pos, true, true);
		}
		power = (long)omega*(long)torque;
	}

	public void getIOSides(int metadata) {
		switch(metadata) {
			case 0 -> { read = Direction.EAST; read2 = Direction.NORTH; write = Direction.WEST; write2 = null; } //-z and +x -> -x
			case 1 -> { read = Direction.EAST; read2 = Direction.SOUTH; write = Direction.NORTH; write2 = null; } //+z and +x -> -z
			case 2 -> { read = Direction.SOUTH; read2 = Direction.WEST; write = Direction.EAST; write2 = null; } //+z and -x -> +x
			case 3 -> { read = Direction.NORTH; read2 = Direction.WEST; write = Direction.SOUTH; write2 = null; } //-z and -x -> +z
			case 4 -> { read = Direction.EAST; read2 = Direction.SOUTH; write = Direction.WEST; write2 = null; } //+z and +x -> -x
			case 5 -> { read = Direction.WEST; read2 = Direction.SOUTH; write = Direction.NORTH; write2 = null; } //+z and -x -> -z
			case 6 -> { read = Direction.NORTH; read2 = Direction.WEST; write = Direction.EAST; write2 = null; } //-z and -x -> +x
			case 7 -> { read = Direction.NORTH; read2 = Direction.EAST; write = Direction.SOUTH; write2 = null; } //-z and +x -> +z
			case 8 -> { read2 = null; read = Direction.WEST; write = Direction.EAST; write2 = Direction.NORTH; } //-z and +x <- -x
			case 9 -> { read = Direction.NORTH; read2 = null; write = Direction.SOUTH; write2 = Direction.EAST; } //+z and +x <- -z
			case 10 -> { read = Direction.EAST; read2 = null; write = Direction.WEST; write2 = Direction.SOUTH; } //+z and -x <- +x
			case 11 -> { read = Direction.SOUTH; read2 = null; write = Direction.NORTH; write2 = Direction.WEST; } //-z and -x <- +z
			case 12 -> { read = Direction.WEST; read2 = null; write = Direction.EAST; write2 = Direction.SOUTH; } //+z and +x <- -x
			case 13 -> { read = Direction.NORTH; read2 = null; write = Direction.SOUTH; write2 = Direction.WEST; } //+z and -x <- -z
			case 14 -> { read = Direction.EAST; read2 = null; write = Direction.WEST; write2 = Direction.NORTH; } //-z and -x <- +x
			case 15 -> { read = Direction.SOUTH; read2 = null; write = Direction.NORTH; write2 = Direction.EAST; } //-z and +x <- +z
		}
	}

	/** Original: x == spl.getWriteX() && z == spl.getWriteZ() */
	public boolean isInlineTarget(BlockPos p) {
		return write != null && p.getX() == worldPosition.getX()+write.getStepX() && p.getZ() == worldPosition.getZ()+write.getStepZ();
	}

	/** Original: x == spl.getWriteX2() && z == spl.getWriteZ2() */
	public boolean isBendTarget(BlockPos p) {
		return write2 != null && p.getX() == worldPosition.getX()+write2.getStepX() && p.getZ() == worldPosition.getZ()+write2.getStepZ();
	}

	@Override
	protected void readFromCross(ShaftBlockEntity cross) {
		if (cross.isWritingTo(this)) {
			omega = cross.readomega[0];
			torque = cross.readtorque[0];
		}
		else if (cross.isWritingTo2(this)) {
			omega = cross.readomega[1];
			torque = cross.readtorque[1];
		}
	}

	private void mergeReadFromCross(ShaftBlockEntity cross, int side) {
		if (cross.isWritingTo(this)) {
			if (side == 0) {
				omegain = cross.readomega[0];
				torquein = cross.readtorque[0];
			}
			if (side == 1) {
				omegain2 = cross.readomega[0];
				torquein2 = cross.readtorque[0];
			}
		}
		else if (cross.isWritingTo2(this)) {
			if (side == 0) {
				omegain = cross.readomega[1];
				torquein = cross.readtorque[1];
			}
			if (side == 1) {
				omegain2 = cross.readomega[1];
				torquein2 = cross.readtorque[1];
			}
		}
	}

	protected void transferPower(Level world, BlockPos pos, boolean check1, boolean check2) {
		if (world.isClientSide)
			return;
		if (check1 && check2)
			omegain = torquein = 0;
		if (!this.isSplitting()) {
			BlockEntity te = this.getAdjacentTileEntity(read);
			BlockEntity te2 = this.getAdjacentTileEntity(read2);
			if (check1) {
				if (this.isProvider(te)) {
					if (te instanceof ShaftBlockEntity devicein) {
						if (devicein.isCross()) {
							this.mergeReadFromCross(devicein, 0);
						}
						else if (devicein.isWritingTo(this)) {
							torquein = devicein.torque;
							omegain = devicein.omega;
						}
					}
					if (te instanceof SimpleProvider) {
						IOMachineBlockEntity devicein = (IOMachineBlockEntity)te;
						if (devicein.isWritingTo(this)) {
							torquein = devicein.torque;
							omegain = devicein.omega;
						}
						else if (devicein.isWritingTo2(this)) {
							torquein = devicein.torque;
							omegain = devicein.omega;
						}
						else
							torquein = omegain = 0;
					}
					if (te instanceof ComplexIO pwr) {
						Direction dir = this.getInputForgeDirection().getOpposite();
						omegain = pwr.getSpeedToSide(dir);
						torquein = pwr.getTorqueToSide(dir);
					}
					if (te instanceof ShaftPowerEmitter sp) {
						if (sp.isEmitting() && sp.canWriteTo(read.getOpposite())) {
							torquein = sp.getTorque();
							omegain = sp.getOmega();
						}
						else
							torquein = omegain = 0;
					}
					if (te instanceof SplitterBlockEntity devicein) {
						if (devicein.isSplitting()) {
							processingSecondary = false;
							this.readFromSplitter(world, pos, devicein);
						}
						else if (devicein.isWritingTo(this)) {
							torquein = devicein.torque;
							omegain = devicein.omega;
						}
					}
				}
				else {
					omegain = 0;
					torquein = 0;
				}
			}
			if (check2) {
				if (this.isProvider(te2)) {
					if (te2 instanceof ShaftBlockEntity devicein2) {
						if (devicein2.isCross()) {
							this.mergeReadFromCross(devicein2, 1);
						}
						else if (devicein2.isWritingTo(this)) {
							torquein2 = devicein2.torque;
							omegain2 = devicein2.omega;
						}
					}
					if (te2 instanceof SimpleProvider) {
						IOMachineBlockEntity devicein = (IOMachineBlockEntity)te2;
						if (devicein.isWritingTo(this)) {
							torquein2 = devicein.torque;
							omegain2 = devicein.omega;
						}
						else
							torquein2 = omegain2 = 0;
					}
					if (te2 instanceof ComplexIO pwr) {
						Direction dir = this.getInputForgeDirection().getOpposite();
						omegain2 = pwr.getSpeedToSide(dir);
						torquein2 = pwr.getTorqueToSide(dir);
					}
					if (te2 instanceof ShaftPowerEmitter sp) {
						if (sp.isEmitting() && sp.canWriteTo(read2.getOpposite())) {
							torquein2 = sp.getTorque();
							omegain2 = sp.getOmega();
						}
						else
							torquein2 = omegain2 = 0;
					}
					if (te2 instanceof SplitterBlockEntity devicein2) {
						if (devicein2.isSplitting()) {
							processingSecondary = true;
							this.readFromSplitter(world, pos, devicein2);
						}
						else if (devicein2.isWritingTo(this)) {
							torquein2 = devicein2.torque;
							omegain2 = devicein2.omega;
						}
					}
				}
				else {
					omegain2 = 0;
					torquein2 = 0;
				}
			}
			if (!check1 || !check2)
				return;

			PowerSourceList in1 = null;
			PowerSourceList in2 = null;
			if (read != null && read2 != null) {
				in1 = PowerSourceList.getAllFrom(world, read, pos.relative(read), this, this);
				in2 = PowerSourceList.getAllFrom(world, read2, pos.relative(read2), this, this);

				if (this.isLoopingPower(in1, in2)) {
					omega = torque = 0;
					power = 0;
					this.fail();
				}
			}

			boolean overload = false;
			if (!this.canCombine(in1, in2, torquein, torquein2)) {
				overloadTick++;
				overload = true;
				if (overloadTick > 20) {
					this.fail();
					overloadTick = 0;
				}
				else {
					this.critEffect(world, pos);
				}
			}
			else if (omegain == omegain2) {
				omega = omegain;
				torque = torquein+torquein2;
			}
			else {
				omega = omegain == 0 || omegain2 == 0 ? Math.max(omegain, omegain2) : rand.nextInt(Math.max(1+omegain, 1+omegain2));
				if (omegain == 0 || omegain2 == 0) {
					if (omega == omegain)
						torque = torquein;
					else
						torque = torquein2;
				}
				else {
					torque = rand.nextInt(Math.min(1+torquein, 1+torquein2));
				}
				if (omegain != 0 && omegain2 != 0) {
					this.critEffect(world, pos);
				}
			}
			if (!overload)
				overloadTick = 0;
			this.basicPowerReceiver();
		}
		else {
			BlockEntity te = this.getAdjacentTileEntity(read);
			if (this.isProvider(te)) {
				if (te instanceof ShaftBlockEntity devicein) {
					if (devicein.isCross()) {
						this.readFromCross(devicein);
						return;
					}
					if (devicein.isWritingTo(this)) {
						torque = devicein.torque;
						omega = devicein.omega;
					}
					else
						torque = omega = 0;
				}
				if (te instanceof SimpleProvider) {
					IOMachineBlockEntity devicein = (IOMachineBlockEntity)te;
					if (devicein.isWritingTo(this)) {
						torque = devicein.torque;
						omega = devicein.omega;
					}
					else
						torque = omega = 0;
				}
				if (te instanceof ShaftPowerEmitter sp) {
					if (sp.isEmitting() && sp.canWriteTo(read.getOpposite())) {
						torque = sp.getTorque();
						omega = sp.getOmega();
					}
					else
						torque = omega = 0;
				}
				if (te instanceof ComplexIO pwr) {
					Direction dir = this.getInputForgeDirection().getOpposite();
					omega = pwr.getSpeedToSide(dir);
					torque = pwr.getTorqueToSide(dir);
				}
				if (te instanceof SplitterBlockEntity devicein) {
					if (devicein.isSplitting()) {
						processingSecondary = false;
						this.readFromSplitter(world, pos, devicein);
						torque = torquein;
						omega = omegain;
					}
					else if (devicein.isWritingTo(this)) {
						torque = devicein.torque;
						omega = devicein.omega;
					}
					else
						torque = omega = 0;
				}
			}
			else {
				omega = 0;
				torque = 0;
			}
			power = (long)torque*(long)omega;
			this.writeToReceiver();
		}
	}

	private void critEffect(Level world, BlockPos pos) {
		if (world instanceof ServerLevel sl)
			sl.sendParticles(ParticleTypes.CRIT, pos.getX()+rand.nextFloat(), pos.getY()+rand.nextFloat(), pos.getZ()+rand.nextFloat(), 1, -0.5+rand.nextFloat(), rand.nextFloat(), -0.5+rand.nextFloat(), 0);
		world.playSound(null, pos, SoundEvents.BLAZE_HURT, SoundSource.BLOCKS, 0.1F, 1F);
	}

	private boolean canCombine(@Nullable PowerSourceList in1, @Nullable PowerSourceList in2, int t1, int t2) {
		if (in1 == null || in2 == null)
			return true;
		PowerSourceList combo = PowerSourceList.combine(in1, in2, this);
		if (combo.isLooping())
			return false;
		if (t1 == 0 || t2 == 0)
			return true;
		return bedrock || !combo.isEngineSpam();
	}

	@Override
	public void fail() {
		level.explode(null, worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5, 4, Level.ExplosionInteraction.NONE);
		failed = true;
	}

	public boolean isSplitting() {
		return this.getBlockMetadata() >= 8;
	}

	public boolean isBedrock() {
		return bedrock;
	}

	private boolean isLoopingPower(PowerSourceList in1, PowerSourceList in2) {
		int st = torquein+torquein2;
		int so = omegain+omegain2;
		return st > 0 && so > 0 && (in1.passesThrough(this) || in2.passesThrough(this));
	}

	private void writeToReceiver() {
		int t1;
		int t2;

		int ratio = this.getRatioFromMode();
		if (ratio == 0)
			return;
		boolean favorbent = false;
		if (ratio < 0) {
			favorbent = true;
			ratio = -ratio;
		}
		if (ratio == 1) { //Even split, favorbent irrelevant
			t1 = torque/2;
		}
		else if (favorbent) {
			t1 = torque/ratio;
		}
		else {
			t1 = (int)(torque*((ratio-1D)/(ratio)));
		}
		if (ratio == 1) {
			t2 = torque/2;
		}
		else if (favorbent) {
			t2 = (int)(torque*((ratio-1D)/(ratio)));
		}
		else {
			t2 = torque/ratio;
		}
		this.writeToPowerReceiver(write, omega, t1);
		this.writeToPowerReceiver(write2, omega, t2);

		torqueOut1 = t1;
		torqueOut2 = t2;
	}

	@Override
	protected void readFromSplitter(Level world, BlockPos pos, SplitterBlockEntity spl) { //Complex enough to deserve its own function
		this.assignOmega(spl.omega); //omegain always constant
		int ratio = spl.getRatioFromMode();
		if (ratio == 0)
			return;
		boolean favorbent = false;
		if (ratio < 0) {
			favorbent = true;
			ratio = -ratio;
		}
		if (spl.isInlineTarget(pos)) { //We are the inline
			if (ratio == 1) { //Even split, favorbent irrelevant
				this.assignTorque(spl.torque/2);
				return;
			}
			if (favorbent) {
				this.assignTorque(spl.torque/ratio);
			}
			else {
				this.assignTorque((int)(spl.torque*((ratio-1D)/(ratio))));
			}
		}
		else if (spl.isBendTarget(pos)) { //We are the bend
			this.assignOmega(spl.omega); //omegain always constant
			if (ratio == 1) { //Even split, favorbent irrelevant
				this.assignTorque(spl.torque/2);
				return;
			}
			if (favorbent) {
				this.assignTorque((int)(spl.torque*((ratio-1D)/(ratio))));
			}
			else {
				this.assignTorque(spl.torque/ratio);
			}
		}
		else { //We are not one of its write-to blocks
			this.assignTorque(0);
			this.assignOmega(0);
		}
	}

	private void assignTorque(int val) {
		if (processingSecondary)
			torquein2 = val;
		else
			torquein = val;
	}

	private void assignOmega(int val) {
		if (processingSecondary)
			omegain2 = val;
		else
			omegain = val;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putInt("mode", splitmode);
		NBT.putBoolean("fail", failed);
		NBT.putBoolean("bedrock", bedrock);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		splitmode = NBT.contains("mode") ? NBT.getInt("mode") : 1;
		failed = NBT.getBoolean("fail");
		bedrock = NBT.getBoolean("bedrock");
	}

	@Override
	protected void applyImplicitComponents(DataComponentInput input) {
		super.applyImplicitComponents(input);
		bedrock = input.getOrDefault(RotaryComponents.BEDROCK_JUNCTION.get(), false);
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (bedrock)
			components.set(RotaryComponents.BEDROCK_JUNCTION.get(), true);
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
		PowerSourceList pwr = new PowerSourceList();
		if (caller == null)
			caller = this;
		if (!this.isSplitting()) { //merge
			if (read != null) {
				pwr.addAll(PowerSourceList.getAllFrom(level, read, worldPosition.relative(read), this, caller));
			}
			if (read2 != null) {
				pwr.addAll(PowerSourceList.getAllFrom(level, read2, worldPosition.relative(read2), this, caller));
			}
			return pwr;
		}
		else {
			return PowerSourceList.getAllFrom(level, read, worldPosition.relative(read), this, caller);
		}
	}

	public String getRatioForDisplay() {
		return switch(splitmode) {
			case 32 -> "31:1 Inline";
			case -32 -> "1:31 Bend";
			case 16 -> "15:1 Inline";
			case -16 -> "1:15 Bend";
			case 8 -> "7:1 Inline";
			case -8 -> "1:7 Bend";
			case 4 -> "3:1 Inline";
			case -4 -> "1:3 Bend";
			case 1 -> "1:1 Even";
			default -> "ERROR";
		};
	}

	/** Original getWriteY() returned the junction's own y; outputs are always horizontal. */
	@Override
	@Nullable
	public BlockPos getWritePos() {
		return write != null ? new BlockPos(worldPosition.getX()+write.getStepX(), worldPosition.getY(), worldPosition.getZ()+write.getStepZ()) : null;
	}

	@Override
	@Nullable
	public BlockPos getWritePos2() {
		return write2 != null ? new BlockPos(worldPosition.getX()+write2.getStepX(), worldPosition.getY(), worldPosition.getZ()+write2.getStepZ()) : null;
	}

	public void setBedrock() {
		bedrock = true;
	}

	public int getInputTorque1() {
		return torquein;
	}

	public int getInputTorque2() {
		return torquein2;
	}

	public int getInputSpeed1() {
		return omegain;
	}

	public int getInputSpeed2() {
		return omegain2;
	}

	@Override
	public void onPowerLooped(PowerTracker pwr) {
		if (power > 0)
			this.fail();
	}

	@Override
	public final void getAllOutputs(Collection<BlockEntity> c, Direction dir) {
		c.add(this.getAdjacentTileEntity(write));
		if (this.isSplitting()) {
			c.add(this.getAdjacentTileEntity(write2));
		}
	}
}
