/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.blockentity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.lib.math.ReikaMathLibrary;

/**
 * Original TileEntityAreaFiller: fills the connected air below the machine with a block, one block per operation. Power comes from any
 * side (summative). The area is everything connected to the block right below the machine through air, at most "spillerRange" (16)
 * blocks away horizontally and down to one above the world's bottom; for a liquid also the flowing (non-source) blocks of that liquid.
 * It is filled from the bottom up and, within a layer, from the machine outwards. The list is only rebuilt once it is used up, and a
 * listed position is overwritten whatever stands there by then (as in the original).
 * <p>
 * The original searched recursively and stopped at a stack overflow; here the search is a queue without that accidental limit.
 */
public abstract class AreaFillerBlockEntity extends PowerReceiverBlockEntity {

	private final List<BlockPos> blocks = new ArrayList<>();
	private int next;

	protected AreaFillerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, long minPower) {
		super(type, pos, state, 0, 0, minPower);
	}

	@Override
	protected void updateEntity(Level world, BlockPos pos, BlockState state) {
		this.updateTileEntity();
		if (world.isClientSide)
			return;
		this.readPowerSummative();
		tickcount++;
		if (power < MINPOWER || !this.hasRemainingBlocks())
			return;
		BlockState bk = this.getNextPlacedBlock();
		if (bk == null || power < this.getRequiredPower())
			return;
		if (next >= blocks.size())
			this.findBlocks(world, pos, bk);
		if (tickcount >= this.getOperationTime() && next < blocks.size()) {
			tickcount = 0;
			BlockPos c = blocks.get(next++);
			world.setBlockAndUpdate(c, bk);
			boolean liquid = bk.getBlock() instanceof LiquidBlock;
			SoundEvent snd = liquid ? SoundEvents.GENERIC_SWIM : bk.getSoundType(world, c, null).getPlaceSound();
			world.playSound(null, c, snd, SoundSource.BLOCKS, 1, liquid ? 0.5F+world.random.nextFloat() : 1);
			this.onBlockPlaced();
			this.setChanged();
		}
	}

	private void findBlocks(Level world, BlockPos pos, BlockState bk) {
		blocks.clear();
		next = 0;
		int r = RotaryConfig.SPILLERRANGE.get();
		if (r <= 0)
			return;
		Set<BlockPos> found = new HashSet<>();
		int bottom = world.getMinBuildHeight();
		this.spread(world, pos, r, bottom+1, found, BlockState::isAir);
		if (this.allowFluidOverwrite())
			this.spread(world, pos, r, bottom, found, st -> st.getBlock() instanceof LiquidBlock);
		if (bk.getBlock() instanceof LiquidBlock)
			this.spread(world, pos, r, bottom, found, s -> s.is(bk.getBlock()) && !s.getFluidState().isSource());
		blocks.addAll(found);
		blocks.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY).thenComparingDouble(c -> c.distSqr(pos)));
	}

	/** Original BlockArray.recursiveAddWithBounds from the block below the machine, over the six neighbours. */
	private void spread(Level world, BlockPos pos, int r, int minY, Set<BlockPos> found, Predicate<BlockState> match) {
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		queue.add(pos.below());
		seen.add(pos.below());
		while (!queue.isEmpty()) {
			BlockPos c = queue.poll();
			if (Math.abs(c.getX()-pos.getX()) > r || Math.abs(c.getZ()-pos.getZ()) > r || c.getY() < minY || c.getY() >= pos.getY())
				continue;
			if (!world.isLoaded(c) || !match.test(world.getBlockState(c)))
				continue;
			found.add(c);
			for (Direction dir : Direction.values()) {
				BlockPos n = c.relative(dir);
				if (seen.add(n))
					queue.add(n);
			}
		}
	}

	/** The positions still to be filled (for tests). */
	public int getRemainingArea() {
		return blocks.size()-next;
	}

	protected abstract boolean allowFluidOverwrite();

	public abstract long getRequiredPower();

	protected abstract void onBlockPlaced();

	protected abstract boolean hasRemainingBlocks();

	@Nullable
	protected abstract BlockState getNextPlacedBlock();

	/** Original DurationRegistry.FILLER / SPILLER: DurationFormula(22, 2) = max(1, 22 - 2*log2(omega+1)) ticks. */
	public int getOperationTime() {
		return (int)Math.max(1, 22-2*ReikaMathLibrary.logbase(Math.max(0, omega)+1, 2));
	}

	@Override
	public void onEMP() {}
}
