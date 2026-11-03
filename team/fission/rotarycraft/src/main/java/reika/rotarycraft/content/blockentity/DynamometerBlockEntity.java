/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.api.interfaces.ComplexIO;
import reika.rotarycraft.api.power.ShaftPowerEmitter;
import reika.rotarycraft.content.power.SimpleProvider;
import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: Reika.RotaryCraft.TileEntities.Transmission.TileEntityMonitor (the Dynamometer). Passes power through and shows it. */
public class DynamometerBlockEntity extends OneDTransmitterBlockEntity {

	public DynamometerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.DYNAMOMETER.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Dynamometer";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		power = (long)omega*(long)torque;
		this.getIOSides(world, pos, this.getBlockMetadata(), false);
		this.transferPower(world, pos, this.getBlockMetadata());

		this.basicPowerReceiver();
	}

	@Override
	protected void transferPower(Level world, BlockPos pos, int meta) {
		if (world.isClientSide)
			return;
		omegain = torquein = 0;
		BlockEntity te = this.getAdjacentTileEntity(read);
		if (this.isProvider(te)) {
			if (te instanceof ShaftBlockEntity devicein) {
				if (devicein.isCross()) {
					this.readFromCross(devicein);
					return;
				}
				else if (devicein.isWritingToCoordinate(pos)) {
					torquein = devicein.torque;
					omegain = devicein.omega;
				}
			}
			if (te instanceof ComplexIO pwr) {
				Direction dir = this.getInputForgeDirection().getOpposite();
				omegain = pwr.getSpeedToSide(dir);
				torquein = pwr.getTorqueToSide(dir);
			}
			if (te instanceof SimpleProvider) {
				this.copyStandardPower(te);
			}
			if (te instanceof ShaftPowerEmitter sp) {
				if (sp.isEmitting() && sp.canWriteTo(read.getOpposite())) {
					torquein = sp.getTorque();
					omegain = sp.getOmega();
				}
			}
			if (te instanceof SplitterBlockEntity devicein) {
				if (devicein.isSplitting()) {
					this.readFromSplitter(world, pos, devicein);
					torquein = torque;
					omegain = omega;
					return;
				}
				else if (devicein.isWritingToCoordinate(pos)) {
					torquein = devicein.torque;
					omegain = devicein.omega;
				}
			}
			omega = omegain;
			torque = torquein;
		}
		else {
			omega = torque = 0;
		}
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {
		phi += (float)ReikaMathLibrary.doubpow(ReikaMathLibrary.logbase(omega+1, 2), 1.25);
	}

	@Override
	public int getRedstoneOverride() {
		return power > 0 ? Math.max(1, Math.min(15, (int)(15L*power/1048576))) : 0;
	}
}
