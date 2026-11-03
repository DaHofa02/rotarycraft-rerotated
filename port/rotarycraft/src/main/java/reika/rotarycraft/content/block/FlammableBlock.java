/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Original: Reika.RotaryCraft.Blocks.BlockDeco getFlammability / getFireSpreadSpeed - the anthracite block (30 / 4) and the coal coke
 * block (20 / 3) burn; the other decorative storage blocks do not. Same meaning in 1.7.10 Forge and NeoForge: flammability is the
 * chance to burn up, the spread speed the chance to catch fire from a neighbour.
 */
public class FlammableBlock extends Block {

	private final int flammability;
	private final int spreadSpeed;
	private final MapCodec<FlammableBlock> codec;

	public FlammableBlock(Properties props, int flammability, int spreadSpeed) {
		super(props);
		this.flammability = flammability;
		this.spreadSpeed = spreadSpeed;
		codec = simpleCodec(p -> new FlammableBlock(p, flammability, spreadSpeed));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return codec;
	}

	@Override
	public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return flammability;
	}

	@Override
	public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return spreadSpeed;
	}
}
