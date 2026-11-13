/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.api.interfaces.Screwdriverable;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.registry.RotaryBlockEntities;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.TileEntities.World.TileEntityFloodlight - 1024 W from behind lights the air in front of it with
 * invisible light blocks (level 15), up to 64 blocks until the first block that is not clear; with a fresnel lens (right-click) a
 * widening cone instead (side 1 + 2 * ((d - 1) / 3 + 1) at distance d > 1), at most 24 deep. The beam is laid every 16 ticks for 8
 * ticks ((time & 8) == 8, as in the original) and rebuilt when the range changes (a block put into the beam, the power gone). Sneaking
 * with a screwdriver on an upright floodlight switches the beam look.
 * Changes: both of the original's light blocks (BlockLightBlock, and BlockBeam in beam mode) are vanilla's {@code minecraft:light}; the
 * beam mode only changes the model, as the visible beam block is not ported. Breaking it removes only its light blocks.
 */
public class FloodlightBlockEntity extends PowerReceiverBlockEntity implements UpgradeableMachine, Screwdriverable {

	public static final int MAX_RANGE = 64;
	public static final int FRESNEL_RANGE = 24;

	public boolean beammode;
	public boolean fresnel;
	private final List<BlockPos> beam = new ArrayList<>();
	private int lastRange;
	private Direction facing = Direction.WEST;

	public FloodlightBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.FLOODLIGHT.get(), pos, state, 1, 1, 1024);
	}

	@Override
	public String getName() {
		return this.getBlockState().getBlock().getName().getString();
	}

	/** Original TileEntityBeamMachine.getIOSides: input behind, the light goes the other way. */
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

	/** The spots it lights (collected when the range last changed). */
	public List<BlockPos> getBeam() {
		return beam;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		this.getIOSides(this.getBlockMetadata());
		if (fresnel)
			beammode = false;
		if (world.isClientSide)
			return;
		this.readPower();
		power = (long)omega*(long)torque;
		if ((world.getGameTime() & 8L) == 8L)
			this.makeBeam(world, pos);
	}

	private void makeBeam(Level world, BlockPos pos) {
		int r = this.getRange();
		if (lastRange != r) {
			this.clearLights(world);
			if (r > 0) {
				if (!fresnel) {
					for (int i = 1; i <= r; i++)
						if (!this.addIfClear(world, pos.relative(facing, i)))
							break;
				}
				else {
					Direction[] ds = perpendicular(facing);
					for (int d = 1; d <= r; d++) {
						int w = (d-1)/3;
						if (d > 1)
							w++;
						for (int a = -w; a <= w; a++)
							for (int b = -w; b <= w; b++)
								this.addIfClear(world, pos.relative(facing, d).relative(ds[0], a).relative(ds[1], b));
					}
				}
			}
			lastRange = r;
		}
		for (BlockPos c : beam)
			if (world.getBlockState(c).isAir())
				world.setBlockAndUpdate(c, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15));
	}

	private static Direction[] perpendicular(Direction d) {
		return switch (d.getAxis()) {
			case X -> new Direction[] {Direction.UP, Direction.NORTH};
			case Y -> new Direction[] {Direction.EAST, Direction.NORTH};
			case Z -> new Direction[] {Direction.UP, Direction.EAST};
		};
	}

	/** DragonAPI BlockArray.addIfClear: air (or a light block left from before) is taken; anything else stops a line. */
	private boolean addIfClear(Level world, BlockPos c) {
		BlockState s = world.getBlockState(c);
		if (s.isAir() || s.is(Blocks.LIGHT)) {
			beam.add(c);
			return true;
		}
		return false;
	}

	private void clearLights(Level world) {
		for (BlockPos c : beam)
			if (world.getBlockState(c).is(Blocks.LIGHT))
				world.setBlockAndUpdate(c, Blocks.AIR.defaultBlockState());
		beam.clear();
	}

	/** Original lightsOut: all its lights go out (they come back on the next beam tick). */
	public void lightsOut(Level world) {
		this.clearLights(world);
		lastRange = 0;
	}

	/** 0 without power, else the distance to the first opaque block (counting it), at most the maximum range. */
	public int getRange() {
		if (power < MINPOWER)
			return 0;
		int ir = this.getMaxRange();
		for (int i = 1; i <= ir; i++) {
			BlockPos p = worldPosition.relative(facing, i);
			BlockState s = level.getBlockState(p);
			if (!s.isAir() && s.isSolidRender(level, p))
				return i;
		}
		return ir;
	}

	public int getMaxRange() {
		return fresnel ? FRESNEL_RANGE : MAX_RANGE;
	}

	public static boolean isLens(ItemStack is) {
		return is.is(RotaryItems.part(CraftingPart.LENS));
	}

	@Override
	public void upgrade(ItemStack is) {
		if (!fresnel && isLens(is)) {
			fresnel = true;
			lastRange = -1; //the cone replaces the line on the next beam tick
		}
	}

	@Override
	public boolean canUpgradeWith(ItemStack item) {
		return !fresnel && isLens(item);
	}

	/** Original ItemScrewdriver: sneaking on an upright floodlight switches the beam look and puts the lights out. */
	@Override
	public boolean onShiftRightClick(Level world, BlockPos pos, Direction side) {
		if (this.getBlockMetadata() >= 4) {
			beammode = !beammode;
			this.lightsOut(world);
			this.syncAllData();
			return true;
		}
		beammode = false;
		return false;
	}

	/** Original: the beam look is reset, then the generic case cycles through the 6 directions. */
	@Override
	public boolean onRightClick(Level world, BlockPos pos, Direction side) {
		beammode = false;
		this.lightsOut(world);
		int m = this.getBlockMetadata();
		this.setBlockMetadata(m < 5 ? m+1 : 0);
		this.syncAllData();
		return true;
	}

	/** Original BreakAction.breakBlock: the lights go out and an installed lens drops. */
	@Override
	public void onBlockBroken(Level world, BlockPos pos) {
		this.clearLights(world);
		if (fresnel)
			Block.popResource(world, pos, new ItemStack(RotaryItems.part(CraftingPart.LENS)));
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public void onEMP() {}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.putBoolean("beam", beammode);
		NBT.putBoolean("lens", fresnel);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		beammode = NBT.getBoolean("beam");
		fresnel = NBT.getBoolean("lens");
	}
}
