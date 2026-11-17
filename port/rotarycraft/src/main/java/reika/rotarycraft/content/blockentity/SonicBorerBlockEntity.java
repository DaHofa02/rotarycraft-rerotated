/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.content.entity.SonicShotEntity;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryBlocks;

/**
 * Original: Reika.RotaryCraft.TileEntities.World.TileEntitySonicBorer - with at least 4096 Nm and 65536 W from behind it builds up
 * pressure (power / 65536 kPa a tick; it falls back towards 101 kPa, 2000 in an ultrawarm dimension) and at 400 kPa fires a sonic pulse
 * (-400 kPa) at the first non-empty 7x7 plane in front of it (up to 512 blocks), which the pulse breaks. It does not fire if a plane on
 * the way holds a block it cannot break (unbreakable, liquid, shield block), nor downwards below the world. Above 1000 kPa it blows up
 * (seven explosions of strength 4 around it).
 */
public class SonicBorerBlockEntity extends PowerReceiverBlockEntity {

	public static final int FIRE_PRESSURE = 400;
	public static final int MAXPRESSURE = 1000;
	public static final int FOV = 3;

	private int pressure;
	public int xstep, ystep, zstep;

	public SonicBorerBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.SONIC_BORER.get(), pos, state, 4096, 1, 65536);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSides(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		this.readPower();
		this.updatePressure(world);
		if (this.canFire(world, pos)) {
			this.fire(world, pos);
			pressure -= FIRE_PRESSURE;
		}
		if (pressure > MAXPRESSURE)
			this.overpressure(world, pos);
	}

	private void getIOSides(int meta) {
		xstep = ystep = zstep = 0;
		switch (meta) {
			case 0 -> {
				read = Direction.EAST;
				xstep = -1;
			}
			case 1 -> {
				read = Direction.WEST;
				xstep = 1;
			}
			case 2 -> {
				read = Direction.SOUTH;
				zstep = -1;
			}
			case 3 -> {
				read = Direction.NORTH;
				zstep = 1;
			}
			case 4 -> {
				read = Direction.DOWN;
				ystep = 1;
			}
			default -> {
				read = Direction.UP;
				ystep = -1;
			}
		}
	}

	private void fire(Level world, BlockPos pos) {
		if (this.getDistanceToSurface() >= 0) {
			world.addFreshEntity(new SonicShotEntity(world, this));
			world.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4, 1);
		}
	}

	/** Steps to the first plane with something in it (the maximum range if none); -1 if a plane on the way cannot be broken. */
	public int getDistanceToSurface() {
		if (level == null)
			return -1;
		this.getIOSides(this.getBlockMetadata());
		int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
		int k = FOV;
		for (int m = 1; m < this.getMaxRange(); m++) {
			int dx = x+m*xstep, dy = y+m*ystep, dz = z+m*zstep;
			boolean nonair = false;
			for (int a = -k; a <= k; a++) {
				for (int b = -k; b <= k; b++) {
					BlockPos p = xstep != 0 ? new BlockPos(dx, y+b, z+a) : zstep != 0 ? new BlockPos(x+a, y+b, dz) : new BlockPos(x+a, dy, z+b);
					if (!canDrop(level, p))
						return -1;
					if (!level.getBlockState(p).isAir())
						nonair = true;
				}
			}
			if (nonair)
				return m;
		}
		return this.getMaxRange();
	}

	/** Original config SONICBORERRANGE (512, at least 64). */
	public int getMaxRange() {
		return 512;
	}

	public static boolean canDrop(Level world, BlockPos p) {
		BlockState b = world.getBlockState(p);
		if (b.isAir())
			return true;
		if (b.getDestroySpeed(world, p) < 0)
			return false;
		if (b.getBlock() instanceof LiquidBlock) //BlockLiquid and BlockFluidBase
			return false;
		return !b.is(RotaryBlocks.SHIELD_BLOCK.get());
	}

	private boolean canFire(Level world, BlockPos pos) {
		if (pressure < FIRE_PRESSURE || power < MINPOWER || torque < MINTORQUE)
			return false;
		return pos.getY()-this.getDistanceToSurface() > world.getMinBuildHeight() || ystep != -1;
	}

	public BlockPos getTargetPos() {
		int r = Math.max(0, this.getDistanceToSurface());
		return worldPosition.offset(xstep*r, ystep*r, zstep*r);
	}

	private void updatePressure(Level world) {
		int pamb = world.dimensionType().ultraWarm() ? 2000 : 101;
		int dP = pressure-pamb;
		int pd = dP/384+1;
		if (dP > 0)
			pressure -= pd;
		else
			pressure++;
		if (power >= MINPOWER && torque >= MINTORQUE)
			pressure += (int)(power/65536L);
	}

	public int getPressure() {
		return pressure;
	}

	public void addPressure(int press) {
		pressure += press;
	}

	public void overpressure(Level world, BlockPos pos) {
		Level.ExplosionInteraction mode = RotaryConfig.BLOCKDAMAGE.get() ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE;
		double x = pos.getX()+0.5, y = pos.getY()+0.5, z = pos.getZ()+0.5;
		double[][] at = {{0, 0, 0}, {0, 1, 0}, {0, -1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};
		for (double[] d : at)
			world.explode(null, x+d[0], y+d[1], z+d[2], 4, mode);
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
		NBT.putInt("press", pressure);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		pressure = NBT.getInt("press");
	}
}
