/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.rotarycraft.content.piping.PipeConnector;
import reika.rotarycraft.content.piping.PipeType;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: Reika.RotaryCraft.TileEntities.Production.TileEntitySpillway - takes water from the side it faces (metadata 0 east, 1 west,
 * 2 north, 3 south) into an 8000 mB tank that empties downwards only. Falling water there (a column of three flowing blocks) gives
 * 50 mB a tick (the free water factor is 1); still water instead is drained as a pool: the connected source blocks (up to 64 blocks
 * aside, 24 up, 240 steps) are taken one per tick, highest and farthest first, for 1000 mB each, and kept empty. Water right above it is
 * removed. Not ported: Streams mod rivers, ChromatiCraft crystal tanks, the free water config.
 */
public class SpillwayBlockEntity extends RotaryCraftBlockEntity implements PipeConnector {

	public static final int CAPACITY = 8000;
	public static final int WATERFALL_RATE = 50;

	private final FluidTank tank = new FluidTank(CAPACITY, fs -> fs.is(Fluids.WATER));
	@Nullable
	private List<BlockPos> liquidPool;
	private int poolIndex;
	private final List<BlockPos> forcedEmpty = new ArrayList<>();
	private int activeTick;

	public SpillwayBlockEntity(BlockPos pos, BlockState state) {
		super(RotaryBlockEntities.SPILLWAY.get(), pos, state);
	}

	@Override
	public String getName() {
		return "Spillway";
	}

	public static Direction getDrainSide(int meta) {
		return switch (meta) {
			case 1 -> Direction.WEST;
			case 2 -> Direction.NORTH;
			case 3 -> Direction.SOUTH;
			default -> Direction.EAST;
		};
	}

	public Direction getDrainSide() {
		return getDrainSide(this.getBlockMetadata());
	}

	public int getWaterLevel() {
		return tank.getFluidAmount();
	}

	public boolean isActive() {
		return activeTick > 0;
	}

	private static boolean isWater(FluidState fs) {
		return fs.is(FluidTags.WATER);
	}

	/** Original ReikaWorldHelper.isLiquidAColumn: flowing water here, above and below. */
	private static boolean isWaterColumn(Level world, BlockPos p) {
		for (int dy = -1; dy <= 1; dy++) {
			FluidState fs = world.getFluidState(p.above(dy));
			if (!isWater(fs) || fs.isSource())
				return false;
		}
		return true;
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		if (world.isClientSide)
			return;
		BlockPos d = pos.relative(this.getDrainSide());
		if (isWater(world.getFluidState(d)) && world.getBlockState(d).is(Blocks.WATER)) {
			if (isWaterColumn(world, d.above())) {
				liquidPool = null;
				this.addWater(WATERFALL_RATE);
			}
			else
				this.formAndDrainPool(world, pos, d);
		}
		else
			liquidPool = null;
		if (activeTick > 0)
			activeTick--;
		if (isWater(world.getFluidState(pos.above())) && world.getBlockState(pos.above()).is(Blocks.WATER))
			world.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
	}

	private void addWater(int amt) {
		if (tank.fill(new FluidStack(Fluids.WATER, amt), IFluidHandler.FluidAction.EXECUTE) > 0) {
			this.setActive();
			this.setChanged();
		}
	}

	private void formAndDrainPool(Level world, BlockPos pos, BlockPos start) {
		if (liquidPool == null || poolIndex >= liquidPool.size()) {
			liquidPool = findPool(world, pos, start);
			poolIndex = 0;
			forcedEmpty.clear();
		}
		if (liquidPool.isEmpty()) {
			liquidPool = null;
			return;
		}
		if (CAPACITY-tank.getFluidAmount() >= 1000 && poolIndex < liquidPool.size()) {
			forcedEmpty.add(liquidPool.get(poolIndex++));
			this.addWater(1000);
		}
		for (BlockPos c : forcedEmpty)
			world.setBlock(c, Blocks.AIR.defaultBlockState(), 2);
	}

	/** Original BlockArray.recursiveAddWithBoundsMetadata (water source blocks, maxDepth 240) sorted by distance (farthest first), then by height (top first). */
	private static List<BlockPos> findPool(Level world, BlockPos pos, BlockPos start) {
		Map<BlockPos, Integer> depth = new HashMap<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		List<BlockPos> found = new ArrayList<>();
		if (isPoolBlock(world, start)) {
			depth.put(start, 0);
			queue.add(start);
		}
		while (!queue.isEmpty()) {
			BlockPos c = queue.poll();
			found.add(c);
			int dd = depth.get(c);
			if (dd >= 240)
				continue;
			for (Direction dir : Direction.values()) {
				BlockPos n = c.relative(dir);
				if (depth.containsKey(n) || Math.abs(n.getX()-pos.getX()) > 64 || Math.abs(n.getZ()-pos.getZ()) > 64 || n.getY() < pos.getY() || n.getY() > pos.getY()+24)
					continue;
				if (!world.isLoaded(n) || !isPoolBlock(world, n))
					continue;
				depth.put(n, dd+1);
				queue.add(n);
			}
		}
		found.sort(Comparator.comparingDouble((BlockPos c) -> c.distSqr(pos)).reversed()); //InwardsComparator: farthest first
		found.sort(Comparator.comparingInt((BlockPos c) -> c.getY()).reversed());
		return found;
	}

	private static boolean isPoolBlock(Level world, BlockPos c) {
		BlockState s = world.getBlockState(c);
		return s.is(Blocks.WATER) && s.getFluidState().isSource();
	}

	private void setActive() {
		boolean last = activeTick > 0;
		activeTick = 4;
		if (!last)
			this.syncAllData();
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	public boolean canConnectToPipe(PipeType p) {
		return p.isStandardPipe();
	}

	@Override
	public boolean canConnectToPipeOnSide(PipeType p, Direction side) {
		return this.canConnectToPipe(p);
	}

	@Override
	public Flow getFlowForSide(Direction side) {
		return side == Direction.DOWN ? Flow.OUTPUT : Flow.NONE;
	}

	@Override
	public int fill(@Nullable Direction from, FluidStack resource, IFluidHandler.FluidAction action) {
		return 0;
	}

	@Override
	public FluidStack drain(@Nullable Direction from, int maxDrain, IFluidHandler.FluidAction action) {
		if (from != Direction.DOWN)
			return FluidStack.EMPTY;
		FluidStack out = tank.drain(maxDrain, action);
		if (!out.isEmpty() && action.execute())
			this.setChanged();
		return out;
	}

	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return 1;
			}

			@Override
			public FluidStack getFluidInTank(int t) {
				return tank.getFluid();
			}

			@Override
			public int getTankCapacity(int t) {
				return CAPACITY;
			}

			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return false;
			}

			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return 0;
			}

			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				return resource.is(Fluids.WATER) ? SpillwayBlockEntity.this.drain(side, resource.getAmount(), action) : FluidStack.EMPTY;
			}

			@Override
			public FluidStack drain(int maxDrain, FluidAction action) {
				return SpillwayBlockEntity.this.drain(side, maxDrain, action);
			}
		};
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.writeSyncTag(NBT, registries);
		NBT.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		NBT.putInt("active", activeTick);
	}

	@Override
	protected void readSyncTag(CompoundTag NBT, HolderLookup.Provider registries) {
		super.readSyncTag(NBT, registries);
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, NBT.getCompound("tank"));
		activeTick = NBT.getInt("active");
	}
}
