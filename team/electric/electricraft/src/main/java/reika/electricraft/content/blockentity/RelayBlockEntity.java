/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.electricraft.network.ToggledConnection;
import reika.electricraft.registry.ElectriBlockEntities;

/** Original: Reika.ElectriCraft.TileEntities.TileEntityRelay - conducts only while it has a redstone signal. */
public class RelayBlockEntity extends WireComponentBlockEntity implements ToggledConnection {

	private boolean lastPower;

	public RelayBlockEntity(BlockPos pos, BlockState state) {
		super(ElectriBlockEntities.RELAY.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Relay";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		super.updateEntity(world, pos, state);
		boolean pwr = world.hasNeighborSignal(pos);
		if (!world.isClientSide && pwr != lastPower && network != null)
			network.updateWires();
		lastPower = pwr;
	}

	public boolean isEnabled() {
		return level != null && level.hasNeighborSignal(worldPosition);
	}

	@Override
	public int getResistance() {
		return this.isEnabled() ? 0 : Integer.MAX_VALUE;
	}

	@Override
	public boolean canConnect() {
		return this.isEnabled();
	}

	@Override
	public float getHeight() {
		return 0.75F;
	}

	@Override
	public float getWidth() {
		return 0.75F;
	}
}
