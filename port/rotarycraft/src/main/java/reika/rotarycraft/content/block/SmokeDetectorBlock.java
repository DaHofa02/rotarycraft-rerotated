/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.rotarycraft.content.blockentity.SmokeDetectorBlockEntity;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: MachineRegistry.SMOKEDETECTOR in BlockMIMachine - a flat box under the ceiling (MachineRegistry.getMinX/Y/Z: 0.25 to 0.75
 * wide, from 0.875 up), weak redstone power 15 while alarming (BlockBasicMultiTE.isProvidingWeakPower).
 */
public class SmokeDetectorBlock extends MachineBlock {

	private static final MapCodec<SmokeDetectorBlock> CODEC = simpleCodec(SmokeDetectorBlock::new);
	private static final VoxelShape SHAPE = Block.box(4, 14, 4, 12, 16, 12);

	public SmokeDetectorBlock(Properties props) {
		super(props, RotaryBlockEntities.SMOKE_DETECTOR, Placement.NONE, false);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
		return world.getBlockEntity(pos) instanceof SmokeDetectorBlockEntity te && te.isAlarming() ? 15 : 0;
	}
}
