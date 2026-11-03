/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.api.interfaces.ComplexIO;
import reika.rotarycraft.api.power.PowerGenerator;
import reika.rotarycraft.api.power.PowerTracker;
import reika.rotarycraft.api.power.ShaftMerger;
import reika.rotarycraft.api.power.ShaftPowerEmitter;
import reika.rotarycraft.content.power.PowerSourceList;
import reika.rotarycraft.content.power.PowerSourceTracker;
import reika.rotarycraft.content.power.SimpleProvider;
import reika.rotarycraft.content.power.TorqueUsage;
import reika.rotarycraft.lib.math.ReikaEngLibrary;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.Flywheels;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryComponents;
import reika.rotarycraft.registry.RotarySounds;

/**
 * Original: Reika.RotaryCraft.TileEntities.Transmission.TileEntityFlywheel. Stores rotational energy: speed follows the input with
 * the angular acceleration torque/inertia, and decays against the torque its consumers need. Not ported yet: FlywheelFailureEvent (API events).
 */
public class FlywheelBlockEntity extends TransmissionMachineBlockEntity implements SimpleProvider, PowerGenerator, ShaftMerger {

	public static final int MINTORQUERATIO = Flywheels.MINTORQUERATIO;

	private int decayTime;
	private int maxtorque;

	public boolean failed = false;
	private int soundtick = 0;

	private int oppTorque = 0;
	private int updateticks = 0;

	private Flywheels type = Flywheels.WOOD;

	public FlywheelBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.FLYWHEEL.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Flywheel";
	}

	public int getOppTorque(FlywheelBlockEntity reading) {
		if (reading.getTypeOrdinal().ordinal() < this.getTypeOrdinal().ordinal())
			return this.getMinTorque()*MINTORQUERATIO;
		return omega < omegain-omegain/20 ? torque+oppTorque : oppTorque;
	}

	public int getMinTorque() {
		return this.getTypeOrdinal().getMinTorque();
	}

	public void testFailure() {
		double factor = 0.25*Math.sqrt(omega);
		switch(this.getTypeOrdinal().ordinal()) {
			case 1:
				factor /= 2.5;
			case 3:
				factor *= 1.25;
		}
		factor *= ReikaMathLibrary.doubpow(omega/65536D, 1.5); //to reduce damage
		double energy = ReikaEngLibrary.rotenergy(this.getDensity(), 0.25, omega, 0.75);
		if (ReikaEngLibrary.mat_rotfailure(this.getDensity(), 0.75, omega, 100*this.getStrength()))
			this.fail(level, worldPosition, energy/factor);
	}

	private void fail(Level world, BlockPos pos, double e) {
		failed = true;
		float f = getExplosionFromEnergy(e);
		if (!world.isClientSide)
			world.explode(null, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, f, RotaryConfig.BLOCKDAMAGE.get() ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
	}

	/** Original: ReikaPhysicsHelper.TNTenergy and getExplosionFromEnergy/getEnergyFromExplosion. */
	private static final double TNTenergy = 12420000000D;

	private static float getExplosionFromEnergy(double energy) {
		return (float)(4*energy/TNTenergy);
	}

	private static float getEnergyFromExplosion(float ex) {
		return ex/4F*(float)TNTenergy;
	}

	private double getDensity() {
		return this.getTypeOrdinal().density;
	}

	public Flywheels getTypeOrdinal() {
		return type;
	}

	private double getStrength() {
		return this.getTypeOrdinal().tensileStrength;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.loadType();
		this.getIOSides(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		if (failed) {
			omega = 0;
			torque = 0;
			power = 0;
			return;
		}
		if (read != null && write != null)
			this.process(world, pos);
		power = (long)omega*(long)torque;
		this.testFailure();
		this.playSounds();

		this.basicPowerReceiver();
	}

	private void playSounds() {
		if (omega == 0) {
			soundtick = 20000;
			return;
		}
		float pitch = (float)Math.sqrt(omega/1024F);
		if (pitch == 0)
			pitch = 0.01F;
		soundtick++;
		if (soundtick < -3F/(pitch*pitch)+69/(pitch))
			return;
		soundtick = 0;
		RotarySounds.play(level, worldPosition, RotarySounds.FLYWHEEL.get(), MachineSounds.isMuffled(level, worldPosition) ? 0.3F : 1, pitch);
	}

	private void loadType() {
		maxtorque = this.getTypeOrdinal().maxTorque;
		decayTime = this.getTypeOrdinal().decayTime;
	}

	public void getIOSides(int metadata) {
		switch(metadata%4) {
			case 0 -> read = Direction.WEST;
			case 1 -> read = Direction.EAST;
			case 2 -> read = Direction.NORTH;
			case 3 -> read = Direction.SOUTH;
		}
		write = read.getOpposite();
	}

	public void process(Level world, BlockPos pos) {
		omegain = 0;
		tickcount++;
		BlockEntity te = this.getAdjacentTileEntity(read);
		if (this.isProvider(te)) {
			if (te instanceof ShaftBlockEntity devicein) {
				if (devicein.isCross()) {
					omegain = this.readFromCross(devicein, false);
					torquein = this.readFromCross(devicein, true);
				}
				else if (devicein.isWritingTo(this)) {
					omegain = devicein.omega;
					torquein = devicein.torque;
				}
			}
			if (te instanceof SimpleProvider) {
				this.copyStandardPower(te);
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
			}
			if (te instanceof SplitterBlockEntity devicein) {
				if (devicein.isSplitting()) {
					int om = omega;
					int tq = torque;
					this.readFromSplitter(world, pos, devicein);
					omegain = omega;
					torquein = torque;
					omega = om;
					torque = tq;
					return;
				}
				else if (devicein.isWritingTo(this)) {
					omegain = devicein.omega;
					torquein = devicein.torque;
				}
			}
		}
		double r = 0.75;
		double t = 0.25; //thickness is closer to 0.25m for the actual main disk
		double mass = (t*r*r*Math.PI)*this.getDensity();
		double iner = mass*r*r/2; //standard inertial moment formula for a cylinder with its rotor on the central axis
		updateticks = 0;
		if (torquein >= this.getMinTorque()) {
			oppTorque = TorqueUsage.getTorque(this);
			if (oppTorque <= torquein) {
				if (omega <= omegain) {
					if ((torquein-oppTorque)/iner < 1 && (torquein-oppTorque) > 0) { //making up for the fact that numbers are integers
						int i = 1;
						while ((((double)torquein-oppTorque)/iner*i) < 1) {
							i++;
						}
						updateticks = i;
						if (tickcount % updateticks == 0) {
							omega++;
							tickcount = 0;
						}
					}
					else {
						omega += (torquein-oppTorque)/iner; //torque=inertia*ang.acceleration
					}
					if (omega > omegain)
						omega = omegain; //to prevent oscillations once reached the input value
				}
				else {
					if ((torquein+oppTorque)/iner < 1) {
						int i = 1;
						while ((((double)torquein+oppTorque)/iner*i) < 1) {
							i++;
						}
						updateticks = i;
						if (tickcount % updateticks == 0) {
							omega--;
							tickcount = 0;
						}
					}
					else
						omega = (int)Math.max(0, omega-(torquein+oppTorque)/iner);
				}
				torque = Math.min(torquein, maxtorque);
			}
			else {
				if ((oppTorque-torquein)/iner < 1) {
					int i = 1;
					while (((oppTorque-torquein)/iner*i) < 1) {
						i++;
					}
					updateticks = i;
					if (tickcount%updateticks == 0) {
						omega--;
						tickcount = 0;
					}
				}
				else
					omega = (int)Math.max(0, omega-(oppTorque-torquein)/iner);
				if (omega < 0)
					omega = 0;
			}
		}
		else {
			if (omega == 0) {
				torque = 0;
				tickcount = 0;
			}
			else { //same as before, but without input
				oppTorque = TorqueUsage.getTorque(this);
				if (oppTorque/iner < 1 && oppTorque > 0) {
					int i = 1;
					while ((oppTorque/iner*i) < 1) {
						i++;
					}
					updateticks = i;
					if (tickcount%updateticks == 0) {
						omega--;
					}
				}
				else
					omega = (int)Math.max(0, omega-oppTorque/iner);
				if (omega < 0)
					omega = 0;
			}
		}
		if (omega <= 0)
			torque = 0;
	}

	public int readFromCross(ShaftBlockEntity cross, boolean isTorque) {
		if (cross.isWritingTo(this)) {
			if (isTorque)
				return cross.readtorque[0];
			return cross.readomega[0];
		}
		else if (cross.isWritingTo2(this)) {
			if (isTorque)
				return cross.readtorque[1];
			return cross.readomega[1];
		}
		else
			return 0; //not its output
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("failed", failed);
		NBT.putInt("typeIdx", type.ordinal());
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		failed = NBT.getBoolean("failed");
		type = Flywheels.list[Math.floorMod(NBT.getInt("typeIdx"), Flywheels.list.length)];
	}

	@Override
	protected void applyImplicitComponents(DataComponentInput input) {
		super.applyImplicitComponents(input);
		type = input.getOrDefault(RotaryComponents.FLYWHEEL_TYPE.get(), Flywheels.WOOD);
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		components.set(RotaryComponents.FLYWHEEL_TYPE.get(), type);
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.05);
	}

	@Override
	public int getRedstoneOverride() {
		return 15*omega/this.getTypeOrdinal().maxSpeed;
	}

	@Override
	public void onEMP() {}

	@Override
	public PowerSourceList getPowerSources(PowerSourceTracker io, ShaftMerger caller) {
		PowerSourceList pwr = new PowerSourceList();
		pwr.addAll(PowerSourceList.getAllFrom(level, read, worldPosition.relative(read), this, caller));
		if (pwr.isEmpty())
			pwr.addSource(this);
		return pwr;
	}

	@Override
	public long getMaxPower() {
		return (long)maxtorque*omega;
	}

	@Override
	public long getCurrentPower() {
		return power;
	}

	@Override
	public boolean canProvidePower() {
		return true;
	}

	@Override
	public BlockPos getEmittingPos() {
		return worldPosition.relative(write);
	}

	@Override
	public void onPowerLooped(PowerTracker pwr) {
		omega = torque = 0;
		power = 0;
	}

	@Override
	public void getAllOutputs(Collection<BlockEntity> c, Direction dir) {
		if (dir == read)
			c.add(this.getAdjacentTileEntity(write));
	}

	@Override
	public void fail() {
		this.fail(level, worldPosition, getEnergyFromExplosion(4));
	}
}
