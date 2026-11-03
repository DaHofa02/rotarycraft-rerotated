/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Original: Reika.RotaryCraft.Base.TileEntity.TileEntityTransmissionMachine */
public abstract class TransmissionMachineBlockEntity extends IOMachineBlockEntity {

	protected TransmissionMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	protected void readFromCross(ShaftBlockEntity cross) {
		if (cross.isWritingTo(this)) {
			omega = cross.readomega[0];
			torque = cross.readtorque[0];
		}
		else if (cross.isWritingTo2(this)) {
			omega = cross.readomega[1];
			torque = cross.readtorque[1];
		}
		else {
			omega = torque = 0;
		}
	}

	/** Complex enough to deserve its own function. */
	protected void readFromSplitter(Level world, BlockPos pos, SplitterBlockEntity spl) {
		int sratio = spl.getRatioFromMode();
		if (sratio == 0)
			return;
		boolean favorbent = false;
		if (sratio < 0) {
			favorbent = true;
			sratio = -sratio;
		}
		if (spl.isInlineTarget(pos)) { //We are the inline
			omega = spl.omega; //omega always constant
			if (sratio == 1) { //Even split, favorbent irrelevant
				torque = spl.torque/2;
				return;
			}
			if (favorbent) {
				torque = spl.torque/sratio;
			}
			else {
				torque = (int)(spl.torque*((sratio-1D))/sratio);
			}
		}
		else if (spl.isBendTarget(pos)) { //We are the bend
			omega = spl.omega; //omega always constant
			if (sratio == 1) { //Even split, favorbent irrelevant
				torque = spl.torque/2;
				return;
			}
			if (favorbent) {
				torque = (int)(spl.torque*((sratio-1D)/(sratio)));
			}
			else {
				torque = spl.torque/sratio;
			}
		}
		else { //We are not one of its write-to blocks
			torque = 0;
			omega = 0;
			power = 0;
		}
	}
}
