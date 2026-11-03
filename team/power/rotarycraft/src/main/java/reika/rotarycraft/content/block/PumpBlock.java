/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.PUMP in BlockDMMachine - 2-sided, 12/16 of a block high (MachineRegistry.getMaxY), no GUI. */
public class PumpBlock extends MachineBlock {

	private static final MapCodec<PumpBlock> CODEC = simpleCodec(PumpBlock::new);
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

	public PumpBlock(Properties props) {
		super(props, RotaryBlockEntities.PUMP, Placement.TWO_SIDED, false);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}
}
