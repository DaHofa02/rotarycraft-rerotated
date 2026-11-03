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

import reika.rotarycraft.api.power.ShaftMerger;
import reika.rotarycraft.content.power.PowerSourceList;
import reika.rotarycraft.content.power.PowerSourceTracker;
import reika.rotarycraft.content.power.SimpleProvider;

/** Original: Reika.RotaryCraft.Base.TileEntity.TileEntity1DTransmitter */
public abstract class OneDTransmitterBlockEntity extends TransmissionMachineBlockEntity implements SimpleProvider {

	protected int ratio;
	protected boolean performRatio = true;

	protected OneDTransmitterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public final int getRatio() {
		return ratio;
	}

	public void getIOSides(Level world, BlockPos pos, int meta, boolean hasVertical) {
		switch(meta) {
			case 0:
				read = Direction.EAST;
				break;
			case 1:
				read = Direction.WEST;
				break;
			case 2:
				read = Direction.SOUTH;
				break;
			case 3:
				read = Direction.NORTH;
				break;
			case 4:
				if (hasVertical) {
					read = Direction.DOWN;
				}
				break;
			case 5:
				if (hasVertical) {
					read = Direction.UP;
				}
				break;
		}
		write = read != null ? read.getOpposite() : null;
	}

	protected abstract void transferPower(Level world, BlockPos pos, int meta);

	@Override
	public boolean canProvidePower() {
		return true;
	}

	@Override
	public PowerSourceList getPowerSources(PowerSourceTracker io, ShaftMerger caller) {
		if (read == null)
			return new PowerSourceList();
		return PowerSourceList.getAllFrom(level, read, worldPosition.relative(read), this, caller);
	}

	@Override
	public void getAllOutputs(Collection<BlockEntity> c, Direction dir) {
		if (dir == read)
			c.add(this.getAdjacentTileEntity(write));
	}
}
