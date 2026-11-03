/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.content.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.electricraft.content.blockentity.WireBlockEntity;
import reika.electricraft.registry.ElectriBlockEntities;
import reika.electricraft.registry.WireType;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;

/**
 * Original: Reika.ElectriCraft.Blocks.BlockWire - one block per wire type and insulation here (the original kept the type in the
 * metadata and the insulation in the tile). Hardness 0.05, cloth sound, drops itself, box from 0.3 to 0.7 stretched to each connected side.
 */
public class WireBlock extends BaseEntityBlock {

	public final WireType type;
	public final boolean insulated;
	private final MapCodec<WireBlock> codec;

	public WireBlock(Properties props, WireType type, boolean insulated) {
		super(props);
		this.type = type;
		this.insulated = insulated;
		codec = simpleCodec(p -> new WireBlock(p, type, insulated));
		BlockState s = this.stateDefinition.any();
		for (BooleanProperty p : PipeBlock.PROPERTY_BY_DIRECTION.values())
			s = s.setValue(p, false);
		this.registerDefaultState(s);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return codec;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PipeBlock.NORTH, PipeBlock.EAST, PipeBlock.SOUTH, PipeBlock.WEST, PipeBlock.UP, PipeBlock.DOWN);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		double lo = 0.3*16, hi = 0.7*16;
		return Block.box(state.getValue(PipeBlock.WEST) ? 0 : lo, state.getValue(PipeBlock.DOWN) ? 0 : lo, state.getValue(PipeBlock.NORTH) ? 0 : lo,
				state.getValue(PipeBlock.EAST) ? 16 : hi, state.getValue(PipeBlock.UP) ? 16 : hi, state.getValue(PipeBlock.SOUTH) ? 16 : hi);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WireBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> given) {
		return createTickerHelper(given, ElectriBlockEntities.WIRE.get(), RotaryCraftBlockEntity::tick);
	}

	@Override
	protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos from, boolean moving) {
		super.neighborChanged(state, world, pos, block, from, moving);
		if (!world.isClientSide && world.getBlockEntity(pos) instanceof WireBlockEntity te)
			te.recomputeConnections(world, pos);
	}
}
