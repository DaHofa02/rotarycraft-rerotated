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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.reactorcraft.waste.WastePipeBlockEntity;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/** Original: Reika.ReactorCraft.Blocks.BlockDuct - the waste pipe: drawn like the RotaryCraft pipes (concrete frame, leaf windows). */
public class WastePipeBlock extends ReactorPipeBlock {

	public static final MapCodec<WastePipeBlock> CODEC = simpleCodec(WastePipeBlock::new);

	public WastePipeBlock(Properties props) {
		super(props);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected boolean connectsTo(Level world, BlockPos pos, Direction dir) {
		return WastePipeBlockEntity.connectsTo(world.getBlockEntity(pos.relative(dir)));
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WastePipeBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> given) {
		return createTickerHelper(given, ReactorBlockEntities.WASTE_PIPE.get(), RotaryCraftBlockEntity::tick);
	}
}
