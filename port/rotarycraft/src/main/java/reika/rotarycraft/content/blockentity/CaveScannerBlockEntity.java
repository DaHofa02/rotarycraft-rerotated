/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Surveying.TileEntityCaveFinder - with 131072 W from any side it maps the cave surfaces in a
 * cube of 16 blocks around its source point (at first itself): every block corner whose eight surrounding blocks are some but not all
 * air (DragonAPI cornerHasAirAdjacent) becomes a coloured point, drawn through walls (colour by height), with the cube's outline. The map
 * is redone every 4 ticks. Right-clicking moves the source 4 blocks the way the player looks (sneaking: the other way).
 * Changes: the map is computed in the tick (the original started a thread each time); the original's unused follow-the-player mode is
 * left out; the range is the original's default (config "Cave Scanner FOV" 16).
 */
public class CaveScannerBlockEntity extends PowerReceiverBlockEntity {

	public static final int RANGE = 16;

	private BlockPos src;
	public boolean on;
	private final boolean[][][] points = new boolean[RANGE*2+1][RANGE*2+1][RANGE*2+1];
	private boolean needsCalc = true;
	/** Bumped whenever the map changes (the renderer rebuilds its cached point list). */
	public int version;

	public CaveScannerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.CAVE_SCANNER.get(), pos, state, 1, 1, 131072);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	public int getRange() {
		return RANGE;
	}

	public int getMaxRange() {
		return 128;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (src == null)
			src = pos;
		if (!world.isClientSide)
			this.readPowerSummative();
		on = power >= MINPOWER;
		if (on && (needsCalc || (world.getGameTime() & this.getUpdateFrequency()) == 0))
			this.calculatePoints(world);
	}

	private int getUpdateFrequency() {
		int r = this.getRange();
		return r < 16 ? 1 : r < 32 ? 3 : r < 64 ? 7 : r < 128 ? 15 : 31;
	}

	private void calculatePoints(Level world) {
		int r = this.getRange();
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
		for (int i = -r; i <= r; i++)
			for (int j = -r; j <= r; j++)
				for (int k = -r; k <= r; k++)
					points[i+r][j+r][k+r] = cornerHasAirAdjacent(world, m, src.getX()+i, src.getY()+j, src.getZ()+k);
		needsCalc = false;
		version++;
	}

	/** DragonAPI ReikaWorldHelper.cornerHasAirAdjacent: of the 8 blocks around the corner at (x, y, z), some but not all are air. */
	public static boolean cornerHasAirAdjacent(Level world, BlockPos.MutableBlockPos m, int x, int y, int z) {
		if (y <= world.getMinBuildHeight())
			return false;
		int airs = 0;
		for (int dy = 0; dy <= 1; dy++)
			for (int dx = 0; dx <= 1; dx++)
				for (int dz = 0; dz <= 1; dz++)
					if (world.getBlockState(m.set(x-dx, y-dy, z-dz)).isAir())
						airs++;
		return airs > 0 && airs != 8;
	}

	/** Indices 0 .. 2 * range (the map's corner is the source minus the range). */
	public boolean hasPointAt(int dx, int dy, int dz) {
		int n = this.getRange()*2;
		return dx >= 0 && dy >= 0 && dz >= 0 && dx <= n && dy <= n && dz <= n && points[dx][dy][dz];
	}

	public BlockPos getSource() {
		return src == null ? worldPosition : src;
	}

	public void setSrc(BlockPos p) {
		src = p.immutable();
		needsCalc = true;
		this.syncAllData();
	}

	public void moveSrc(int num, Direction dir) {
		this.setSrc(this.getSource().relative(dir, num));
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		BlockPos s = this.getSource();
		NBT.putIntArray("Source", new int[] {s.getX(), s.getY(), s.getZ()});
		NBT.putBoolean("calc", needsCalc);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		int[] s = NBT.getIntArray("Source");
		if (s.length == 3 && (src == null || src.getX() != s[0] || src.getY() != s[1] || src.getZ() != s[2])) {
			src = new BlockPos(s[0], s[1], s[2]);
			needsCalc = true;
		}
		needsCalc |= NBT.getBoolean("calc");
	}
}
