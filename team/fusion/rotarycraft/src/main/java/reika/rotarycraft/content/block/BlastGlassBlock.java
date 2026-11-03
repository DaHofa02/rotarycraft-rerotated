/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Original: Reika.RotaryCraft.Blocks.BlockBlastGlass - hardness 10, blast resistance 6000, mined with a diamond pickaxe (anything that
 * mines obsidian), cannot be destroyed by explosions or by the wither and the dragon. Faces between two blast glass blocks are hidden.
 * Not ported: the connected-texture edge frames (glass_0..9 overlays of ConnectedGlassRenderer); the block shows the obsidiglass texture.
 */
public class BlastGlassBlock extends TransparentBlock {

	public static final MapCodec<BlastGlassBlock> CODEC = simpleCodec(BlastGlassBlock::new);

	public BlastGlassBlock(Properties props) {
		super(props);
	}

	@Override
	protected MapCodec<? extends TransparentBlock> codec() {
		return CODEC;
	}

	@Override
	public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
		return false;
	}

	@Override
	public boolean canDropFromExplosion(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
		return false;
	}

	@Override
	protected boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
		return adjacent.is(this) || super.skipRendering(state, adjacent, side);
	}
}
