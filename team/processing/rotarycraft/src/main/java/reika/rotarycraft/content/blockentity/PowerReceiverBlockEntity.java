/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.api.interfaces.ComplexIO;
import reika.rotarycraft.api.power.ShaftMerger;
import reika.rotarycraft.api.power.ShaftPowerEmitter;
import reika.rotarycraft.content.power.PowerSourceList;
import reika.rotarycraft.content.power.PowerSourceTracker;
import reika.rotarycraft.content.power.SimpleProvider;

/**
 * Original: Reika.RotaryCraft.Base.TileEntity.TileEntityPowerReceiver (single-sided part): a machine that consumes shaft power from its
 * read side and needs minimum torque, speed and power (original PowerReceivers values) to work. Double-sided reading and machines with
 * several power levels come with the machines that use them.
 */
public abstract class PowerReceiverBlockEntity extends IOMachineBlockEntity {

	public final long MINPOWER;
	public final int MINTORQUE;
	public final int MINSPEED;

	protected PowerReceiverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int minTorque, int minSpeed, long minPower) {
		super(type, pos, state);
		MINTORQUE = minTorque;
		MINSPEED = minSpeed;
		MINPOWER = minPower;
	}

	public final long getScaledOmega(int a) {
		return MINSPEED > 0 ? Math.min(a, a*omega/MINSPEED) : (omega > 0 ? a : 0);
	}

	public final long getScaledTorque(int a) {
		return MINTORQUE > 0 ? Math.min(a, a*torque/MINTORQUE) : (torque > 0 ? a : 0);
	}

	public final long getScaledPower(int a) {
		return MINPOWER > 0 ? Math.min(a, a*power/MINPOWER) : (power > 0 ? a : 0);
	}

	/** Original getIOSidesDefault. */
	public void getIOSidesDefault(int metadata) {
		switch(metadata) {
			case 0 -> read = Direction.EAST;
			case 1 -> read = Direction.WEST;
			case 2 -> read = Direction.SOUTH;
			case 3 -> read = Direction.NORTH;
			case 4 -> read = Direction.DOWN;	//moving up
			case 5 -> read = Direction.UP;		//moving down
		}
	}

	/** Original readFromSplitter for receivers. */
	private void readFromSplitter(SplitterBlockEntity spl) {
		int ratio = spl.getRatioFromMode();
		if (ratio == 0)
			return;
		boolean favorbent = false;
		if (ratio < 0) {
			favorbent = true;
			ratio = -ratio;
		}
		if (spl.isInlineTarget(worldPosition)) { //We are the inline
			omegain = spl.omega; //omega always constant
			if (ratio == 1) { //Even split, favorbent irrelevant
				torquein = spl.torque/2;
				return;
			}
			if (favorbent)
				torquein = spl.torque/ratio;
			else
				torquein = (int)(spl.torque*((ratio-1D)/(ratio)));
		}
		else if (spl.isBendTarget(worldPosition)) { //We are the bend
			omegain = spl.omega; //omega always constant
			if (ratio == 1) { //Even split, favorbent irrelevant
				torquein = spl.torque/2;
				return;
			}
			if (favorbent)
				torquein = (int)(spl.torque*((ratio-1D)/(ratio)));
			else
				torquein = spl.torque/ratio;
		}
		else { //We are not one of its write-to blocks
			torquein = 0;
			omegain = 0;
		}
	}

	/** Original getPower(false): torque and speed from the block on the read side. */
	protected final void readPower() {
		if (level == null || level.isClientSide || read == null)
			return;
		this.readSide(read);
		torque = torquein;
		omega = omegain;
		power = (long)omega*(long)torque;
	}

	/** Original getPower(true): reads both read and read2 and takes the side with the higher power (returnHighest). */
	protected final void readPowerDoubleSided() {
		if (level == null || level.isClientSide)
			return;
		long[][] powerin = new long[2][3];
		Direction[] sides = {read, read2};
		for (int i = 0; i < 2; i++) {
			if (sides[i] == null)
				continue;
			this.readSide(sides[i]);
			powerin[i][0] = (long)torquein*(long)omegain;
			powerin[i][1] = torquein;
			powerin[i][2] = omegain;
		}
		long[] val = new long[3];
		for (long[] p : powerin) {
			if (p[0] > val[0]) //decide based on max power
				val = p;
		}
		torque = (int)val[1];
		omega = (int)val[2];
		power = (long)torque*(long)omega;
	}

	/**
	 * Original getSummativeSidedPower: power from every side (below, above, east, west, south, north). All sides that deliver must turn
	 * at the same speed - then the torques add up - otherwise there is no power and (one time in five) a blaze-hit sound. The original's
	 * crit particles were spawned on the server, where 1.7.10 does not show them. Afterwards read is unset (the machine sets its own).
	 */
	protected final void readPowerSummative() {
		isOmniSided = true;
		if (level == null || level.isClientSide)
			return;
		Direction[] order = {Direction.DOWN, Direction.UP, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};
		long[] omegas = new long[6];
		long[] torques = new long[6];
		for (int i = 0; i < 6; i++) {
			this.readSide(order[i]);
			omegas[i] = omegain;
			torques[i] = torquein;
		}
		read = null;
		torque = 0;
		omega = 0;
		power = 0;
		long first = 0;
		for (long o : omegas) {
			if (o != 0) {
				if (first != 0 && o != first) {
					if (level.random.nextInt(5) == 0)
						level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.BLAZE_HURT, net.minecraft.sounds.SoundSource.BLOCKS, 1, 1);
					return;
				}
				first = o;
			}
		}
		int i = 0;
		while (omegas[i] == 0 && i < 5)
			i++;
		omega = (int)omegas[i];
		long t = 0;
		for (long tq : torques)
			t += tq;
		torque = (int)t;
		power = (long)omega*(long)torque;
	}

	/** Torque and speed delivered from the given side into torquein / omegain. */
	private void readSide(Direction side) {
		torquein = omegain = 0;
		BlockEntity te = this.getAdjacentTileEntity(side);
		if (this.isProvider(te)) {
			if (te instanceof ShaftBlockEntity devicein) {
				if (devicein.isCross()) {
					if (devicein.isWritingTo(this)) {
						omegain = devicein.readomega[0];
						torquein = devicein.readtorque[0];
					}
					else if (devicein.isWritingTo2(this)) {
						omegain = devicein.readomega[1];
						torquein = devicein.readtorque[1];
					}
				}
				else if (devicein.isWritingTo(this)) {
					torquein = devicein.torque;
					omegain = devicein.omega;
				}
			}
			if (te instanceof ComplexIO pwr) {
				Direction dir = side.getOpposite();
				omegain = pwr.getSpeedToSide(dir);
				torquein = pwr.getTorqueToSide(dir);
			}
			if (te instanceof SimpleProvider) {
				this.copyStandardPower(te);
			}
			if (te instanceof ShaftPowerEmitter sp) {
				if (sp.isEmitting() && sp.canWriteTo(side.getOpposite())) {
					torquein = sp.getTorque();
					omegain = sp.getOmega();
				}
			}
			if (te instanceof SplitterBlockEntity devicein) {
				if (devicein.isSplitting()) {
					this.readFromSplitter(devicein);
				}
				else if (devicein.isWritingTo(this)) {
					torquein = devicein.torque;
					omegain = devicein.omega;
				}
				else {
					torquein = omegain = 0;
				}
			}
		}
	}

	@Override
	public boolean canProvidePower() {
		return false;
	}

	@Override
	public PowerSourceList getPowerSources(PowerSourceTracker io, ShaftMerger caller) {
		PowerSourceList pwr = new PowerSourceList();
		for (Direction d : isOmniSided ? Direction.values() : new Direction[] {read, read2, read3, read4}) {
			if (d != null)
				pwr.addAll(PowerSourceList.getAllFrom(level, d, worldPosition.relative(d), this, caller));
		}
		return pwr;
	}

	@Override
	public void getAllOutputs(Collection<BlockEntity> c, Direction dir) {

	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}
}
