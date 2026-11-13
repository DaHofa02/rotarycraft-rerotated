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

import reika.rotarycraft.content.block.BridgeBlock;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryBlocks;

/**
 * Original: Reika.RotaryCraft.TileEntities.World.TileEntityLightBridge - power from behind lays a walkable bridge of hard-light plates
 * forward, one block more per tick, up to min(64, 64 * power / 33554432) blocks (so from 524288 W on), through air, replaceable blocks
 * and light blocks, until something solid. Without enough range, or when the light above it is below 13, the bridge goes out.
 * Changes: the light check uses the raw brightness above (sky light darkened by the time of day, or block light), as 1.7.10's
 * getBlockLightValue; the original's soft blocks are the blocks a player can build into ({@code canBeReplaced}).
 */
public class LightBridgeBlockEntity extends PowerReceiverBlockEntity {

	public static final int DISTANCE_LIMIT = 64;
	public static final long MIN_POWER = 33554432L;

	private int animtick;
	private Direction facing = Direction.WEST;

	public LightBridgeBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.LIGHT_BRIDGE.get(), pos, state, 1, 1, MIN_POWER);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	/** Original TileEntityBeamMachine.getIOSides. */
	private void getIOSides(int meta) {
		read = switch (meta) {
			case 0 -> Direction.EAST;
			case 1 -> Direction.WEST;
			case 2 -> Direction.NORTH;
			case 3 -> Direction.SOUTH;
			case 4 -> Direction.DOWN;
			default -> Direction.UP;
		};
		facing = read.getOpposite();
	}

	public Direction getFacing() {
		return facing;
	}

	/** The plates' direction (texture) for this metadata: 0 -> 3, 1 -> 1, 2 -> 2, 3 -> 0. */
	private int bridgeDir() {
		return switch (this.getBlockMetadata()) {
			case 0 -> 3;
			case 1 -> 1;
			case 2 -> 2;
			default -> 0;
		};
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		animtick++;
		this.getIOSides(this.getBlockMetadata());
		if (world.isClientSide)
			return;
		this.readPower();
		power = (long)omega*(long)torque;
		this.makeBeam(world, pos);
	}

	private boolean isPassable(Level world, BlockPos p) {
		BlockState s = world.getBlockState(p);
		return s.isAir() || s.canBeReplaced() || s.is(Blocks.LIGHT) || s.is(RotaryBlocks.BRIDGE.get());
	}

	private void makeBeam(Level world, BlockPos pos) {
		int range = this.getRange();
		if (range <= 0 || world.getMaxLocalRawBrightness(pos.above()) < 13) {
			this.lightsOut(world, pos);
			return;
		}
		BlockState plate = RotaryBlocks.BRIDGE.get().defaultBlockState().setValue(BridgeBlock.DIR, this.bridgeDir());
		boolean blocked = false;
		boolean front = this.isPassable(world, pos.relative(facing)); //the original only checks the first block in front here
		for (int i = 1; i <= range && i <= animtick && !blocked && front; i++) {
			BlockPos p = pos.relative(facing, i);
			boolean pass = this.isPassable(world, p);
			if (pass && world.getBlockState(p) != plate)
				world.setBlockAndUpdate(p, plate);
			if (!pass || animtick > range+1) {
				animtick--;
				blocked = true;
			}
		}
	}

	private void lightsOut(Level world, BlockPos pos) {
		animtick = 0;
		int dir = this.bridgeDir();
		for (int i = 1; i < this.getMaxRange(); i++) {
			BlockPos p = pos.relative(facing, i);
			BlockState s = world.getBlockState(p);
			if (s.is(RotaryBlocks.BRIDGE.get()) && s.getValue(BridgeBlock.DIR) == dir)
				world.removeBlock(p, false);
		}
	}

	public int getRange() {
		return (int)Math.min(DISTANCE_LIMIT, power*DISTANCE_LIMIT/MIN_POWER);
	}

	public int getMaxRange() {
		return DISTANCE_LIMIT;
	}

	/** Original BreakAction.breakBlock. */
	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		this.lightsOut(world, pos);
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
		NBT.putInt("tick", animtick);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		animtick = NBT.getInt("tick");
	}
}
