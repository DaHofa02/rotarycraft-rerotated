/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import reika.rotarycraft.lib.math.ReikaMathLibrary;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Surveying.TileEntityGPR (ground-penetrating radar) - with 32768 W from any side it scans,
 * once a second, a vertical slice 96 blocks deep below itself and 2 * log2(P - 32768) blocks (at most 40) to each side of its scan point;
 * the slice runs north-south or east-west (set when placed, screwdriver flips it). In the GUI the slice is drawn block by block in the
 * blocks' colours; '[' and ']' move the scan point along the GUI direction, '\' resets it.
 * Changes: the colours are the blocks' map colours (the original's BlockColorMapper table is not ported); other mods' GPRReactive
 * blocks and the achievements are left out.
 */
public class GprBlockEntity extends PowerReceiverBlockEntity implements reika.rotarycraft.api.interfaces.Screwdriverable {

	public static final int MAX_HEIGHT = 96;
	public static final int MAX_WIDTH = 81;

	private final BlockState[][] blocks = new BlockState[MAX_HEIGHT][MAX_WIDTH];
	private boolean xdir;
	private int offsetX, offsetY, offsetZ;
	private int scanTick;

	public GprBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.GPR.get(), pos, state, 1, 1, 32768);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	/** The GUI direction (the scan point moves along it): south if set down facing north/south, else east. */
	public Direction getGuiDirection() {
		return xdir ? Direction.SOUTH : Direction.EAST;
	}

	/** The way the slice runs (ReikaDirectionHelper.getRightBy90 of the GUI direction). */
	public Direction getSliceDirection() {
		return this.getGuiDirection().getClockWise();
	}

	public BlockPos getScanPoint() {
		return worldPosition.offset(offsetX, offsetY, offsetZ);
	}

	/** Original BlockVector toString of getLookDirection: the scan point and the slice direction. */
	public String getLookDescription() {
		BlockPos p = this.getScanPoint();
		return p.getX()+", "+p.getY()+", "+p.getZ()+" "+this.getSliceDirection().getName().toUpperCase(java.util.Locale.ROOT);
	}

	public void shift(Direction dir, int amt) {
		if (amt == 0)
			offsetX = offsetY = offsetZ = 0;
		else {
			offsetX += dir.getStepX()*amt;
			offsetY += dir.getStepY()*amt;
			offsetZ += dir.getStepZ()*amt;
		}
		scanTick = 0;
		this.syncAllData();
	}

	public void setDirection(boolean x) {
		xdir = x;
		this.syncAllData();
	}

	public void flipDirection() {
		this.setDirection(!xdir);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (!world.isClientSide)
			this.readPowerSummative();
		power = (long)omega*(long)torque;
		if (power >= MINPOWER) {
			if (scanTick == 0) {
				this.eval2(world);
				scanTick = 20;
			}
			scanTick--;
		}
	}

	private void eval2(Level world) {
		Direction dir = this.getSliceDirection();
		int r = this.getRange();
		BlockPos c = this.getScanPoint();
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
		for (int j = -r; j <= r; j++)
			for (int dd = 1; dd <= MAX_HEIGHT; dd++) //always from its own height down
				blocks[dd-1][j+r] = world.getBlockState(m.set(c.getX()+j*Math.abs(dir.getStepX()), worldPosition.getY()-dd, c.getZ()+j*Math.abs(dir.getStepZ())));
	}

	/** Column x (-range .. range), depth dd (1 .. 96); null before the first scan. */
	public BlockState getBlock(int x, int dd) {
		int i = x+this.getRange();
		return dd < 1 || dd > MAX_HEIGHT || i < 0 || i >= MAX_WIDTH ? null : blocks[dd-1][i];
	}

	/** RGB for the GUI: the block's map colour (unknown: black). */
	public int getColor(int x, int dd) {
		BlockState s = this.getBlock(x, dd);
		if (s == null || level == null)
			return 0;
		if (s.isAir())
			return 0;
		MapColor mc = s.getMapColor(level, worldPosition);
		return mc == MapColor.NONE ? (s.is(Blocks.WATER) ? MapColor.WATER.col : 0) : mc.col;
	}

	public int getRange() {
		return Math.min(this.getMaxRange(), 2*ReikaMathLibrary.logbase2(power-MINPOWER));
	}

	public int getMaxRange() {
		return 40;
	}

	/** Original ItemScrewdriver: flips the slice direction (sneaking or not). */
	@Override
	public boolean onRightClick(Level world, BlockPos pos, Direction side) {
		this.flipDirection();
		return true;
	}

	@Override
	public boolean onShiftRightClick(Level world, BlockPos pos, Direction side) {
		this.flipDirection();
		return true;
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
		NBT.putInt("xoff", offsetX);
		NBT.putInt("yoff", offsetY);
		NBT.putInt("zoff", offsetZ);
		NBT.putBoolean("xd", xdir);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		offsetX = NBT.getInt("xoff");
		offsetY = NBT.getInt("yoff");
		offsetZ = NBT.getInt("zoff");
		xdir = NBT.getBoolean("xd");
	}
}
