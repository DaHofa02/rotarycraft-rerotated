/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.HEATRAY in BlockDMMachine - 4 orientations with north/south swapped (isZFlipped), 0.6875 high. */
public class HeatRayBlock extends MachineBlock {

	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 11, 16);

	public HeatRayBlock(Properties props) {
		super(props, RotaryBlockEntities.HEAT_RAY, Placement.FOUR_SIDED_Z_FLIPPED, false);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}
}
