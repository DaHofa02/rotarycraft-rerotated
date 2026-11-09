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

import reika.reactorcraft.content.piping.GasDuctBlockEntity;
import reika.reactorcraft.registry.ReactorBlockEntities;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/** Original: Reika.ReactorCraft.Blocks.BlockDuct - the gas duct: drawn like the RotaryCraft pipes (hardened clay frame, glass windows). */
public class GasDuctBlock extends ReactorPipeBlock {

	public static final MapCodec<GasDuctBlock> CODEC = simpleCodec(GasDuctBlock::new);

	public GasDuctBlock(Properties props) {
		super(props);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected boolean connectsTo(Level world, BlockPos pos, Direction dir) {
		return GasDuctBlockEntity.connectsTo(world, pos, dir);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new GasDuctBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> given) {
		return createTickerHelper(given, ReactorBlockEntities.GAS_DUCT.get(), RotaryCraftBlockEntity::tick);
	}
}
