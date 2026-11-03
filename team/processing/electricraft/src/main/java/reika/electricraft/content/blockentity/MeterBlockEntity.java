/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.electricraft.registry.ElectriBlockEntities;

/**
 * Original: Reika.ElectriCraft.TileEntities.TileEntityMeter - a wire piece (no resistance, connects on all sides but the top) that shows
 * the voltage and current at its point and gives a comparator signal of power/1048576 W (1..15).
 */
public class MeterBlockEntity extends WiringBlockEntity {

	public MeterBlockEntity(BlockPos pos, BlockState state) {
		super(ElectriBlockEntities.METER.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Electric Meter";
	}

	@Override
	public int getResistance() {
		return 0;
	}

	@Override
	public void onNetworkChanged() {
		super.onNetworkChanged();
		this.syncAllData();
		if (level != null)
			level.updateNeighbourForOutputSignal(worldPosition, this.getBlockState().getBlock());
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public boolean canNetworkOnSide(Direction dir) {
		return dir != Direction.UP;
	}

	@Override
	public int getRedstoneOverride() {
		long p = this.getWirePower();
		return p > 0 ? Math.max(1, Math.min(15, (int)(p/1048576))) : 0;
	}
}
