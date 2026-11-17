/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Transmission.TileEntitySplitBelt (take-off belt) - the driving hub passes its power straight
 * through, less a take-off of 64 Nm (16 wet) which the belt carries to the receiving hub; that one passes straight through as well and
 * adds the take-off to what it gets from behind (speeds averaged). A wet belt also loses 0-25 % of the speed at random.
 */
public class SplitBeltBlockEntity extends BeltHubBlockEntity {

	public static final int TAKEOFF_TORQUE = 64;
	public static final int TAKEOFF_TORQUE_WET = 16;

	public SplitBeltBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.SPLIT_BELT.get(), pos, state);
	}

	@Override
	public boolean isSplitting() {
		return !isReceivingEnd;
	}

	@Override
	public int copyTorqueFromDriverSide(int input) {
		return Math.min(input, this.getTakeoff());
	}

	private int getTakeoff() {
		return this.isWet() ? TAKEOFF_TORQUE_WET : TAKEOFF_TORQUE;
	}

	@Override
	public int copyOmegaFromDriverSide(int input) {
		if (this.isWet())
			input = (int)(input*(0.75+level.random.nextDouble()*0.25));
		return super.copyOmegaFromDriverSide(input);
	}

	@Override
	protected int getPropagatedTorque(int input) {
		return this.hasValidConnection() ? input-this.getTakeoff() : input;
	}

	@Override
	protected void setReceiverIOSides() {
		write = read.getOpposite();
	}

	@Override
	protected boolean mergeReceivingPower() {
		int prevT = torque;
		int prevO = omega;
		this.readPower();
		if (power <= 0 && (prevT <= 0 || prevO <= 0))
			return false;
		torque += prevT;
		omega = (omega+prevO)/2;
		power = (long)torque*(long)omega;
		return true;
	}

	@Override
	public int getBeltColor() {
		return 0x306040; //RGB 48, 96, 64
	}
}
