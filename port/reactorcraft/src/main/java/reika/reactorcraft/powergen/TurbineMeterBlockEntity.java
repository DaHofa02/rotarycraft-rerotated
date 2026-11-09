/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.powergen;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.reactorcraft.reactor.ReactorBlockEntity;
import reika.reactorcraft.registry.ReactorBlockEntities;

/**
 * Original: Reika.ReactorCraft.TileEntities.TileEntityTurbineMeter (ReactorTiles.TURBINEMETER, "Turbine Dynamometer", ReactorBlocks.MACHINE).
 * On its first tick and every 32 world ticks it looks straight up for a turbine (TileEntityTurbineCore, also the high pressure turbine);
 * its comparator output is 15*omega/maxSpeed of that turbine. The original scan does not stop at the first turbine: it runs up to the
 * build height, and every opaque block it passes clears the found turbine again - so the meter sees the topmost turbine above it, and only
 * if no opaque block lies anywhere above that turbine (turbine blocks themselves have opacity 0 in the original). Kept as in the original.
 */
public class TurbineMeterBlockEntity extends ReactorBlockEntity {

	private int turbineY = Integer.MIN_VALUE;
	private int oldlvl;
	private int lvl;

	public TurbineMeterBlockEntity(BlockPos pos, BlockState state) {
		super(ReactorBlockEntities.TURBINE_METER.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Turbine Dynamometer";
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (world.isClientSide)
			return;
		if (this.getTicksExisted() == 0 || world.getGameTime()%32 == 0)
			this.remapTurbine(world, pos);
		TurbineCoreBlockEntity te = this.getTurbine(world, pos);
		lvl = te != null ? getRedstoneFrom(te) : 0;
		if (this.getTicksExisted() == 0 || oldlvl != lvl)
			world.updateNeighbourForOutputSignal(pos, state.getBlock());
		oldlvl = lvl;
	}

	/** Original remapTurbine; "no turbine" is Integer.MIN_VALUE instead of -1, since 1.21 worlds go below y = 0. */
	private void remapTurbine(Level world, BlockPos pos) {
		turbineY = Integer.MIN_VALUE;
		for (int y = pos.getY()+1; y < world.getMaxBuildHeight(); y++) {
			BlockPos p = new BlockPos(pos.getX(), y, pos.getZ());
			if (world.getBlockEntity(p) instanceof TurbineCoreBlockEntity) {
				turbineY = y;
				continue;
			}
			BlockState s = world.getBlockState(p);
			if (!s.isAir() && s.getLightBlock(world, p) > 0)
				turbineY = Integer.MIN_VALUE;
		}
	}

	private static int getRedstoneFrom(TurbineCoreBlockEntity te) {
		return 15*te.getOmega()/te.getMaxSpeed();
	}

	@Nullable
	private TurbineCoreBlockEntity getTurbine(Level world, BlockPos pos) {
		if (turbineY == Integer.MIN_VALUE)
			return null;
		return world.getBlockEntity(new BlockPos(pos.getX(), turbineY, pos.getZ())) instanceof TurbineCoreBlockEntity te ? te : null;
	}

	/** The turbine the last scan found, or null. */
	@Nullable
	public BlockPos getTurbinePos() {
		return turbineY == Integer.MIN_VALUE ? null : new BlockPos(worldPosition.getX(), turbineY, worldPosition.getZ());
	}

	/** Comparator output; like the original, nothing is saved - the level is recomputed on the first tick. */
	@Override
	public int getRedstoneOverride() {
		return lvl;
	}
}
