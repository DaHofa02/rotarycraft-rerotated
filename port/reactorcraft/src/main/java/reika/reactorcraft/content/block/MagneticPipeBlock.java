/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.fusion.MagneticPipeBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/** Original: Reika.ReactorCraft.Blocks.BlockDuct - the magnetic pipe: drawn like the RotaryCraft pipes (gold frame, glowing gold while charged; blast glass windows). */
public class MagneticPipeBlock extends ReactorPipeBlock {

	public static final MapCodec<MagneticPipeBlock> CODEC = simpleCodec(MagneticPipeBlock::new);

	public MagneticPipeBlock(Properties props) {
		super(props);
		this.registerDefaultState(this.defaultBlockState().setValue(MagneticPipeBlockEntity.CHARGED, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(MagneticPipeBlockEntity.CHARGED);
	}

	@Override
	protected boolean connectsTo(Level world, BlockPos pos, Direction dir) {
		return MagneticPipeBlockEntity.connectsTo(world.getBlockEntity(pos.relative(dir)));
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MagneticPipeBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> given) {
		return createTickerHelper(given, ReactorBlockEntities.MAGNETIC_PIPE.get(), RotaryCraftBlockEntity::tick);
	}
}
