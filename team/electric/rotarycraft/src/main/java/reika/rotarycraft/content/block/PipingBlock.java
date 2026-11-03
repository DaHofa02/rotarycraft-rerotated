/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.content.blockentity.RotaryCraftBlockEntity;
import reika.rotarycraft.content.piping.HoseBlockEntity;
import reika.rotarycraft.content.piping.PipingBlockEntity;

/**
 * Original: Reika.RotaryCraft.Blocks.BlockPiping. Anyone can take pipes down with any tool or by hand (hardness from the
 * "Pipe Block Hardness" option, default 0); the box is inset by 2 pixels on every unconnected side; the hose burns
 * (flammability 45) and the pipes glow with luminous fluids. The six connection properties are those of vanilla {@link PipeBlock}.
 */
public class PipingBlock extends MachineBlock {

	private final MapCodec<PipingBlock> codec;
	private final boolean flammable;

	public PipingBlock(Properties props, Supplier<? extends BlockEntityType<? extends RotaryCraftBlockEntity>> type, boolean flammable) {
		super(props, type, Placement.NONE, false);
		this.flammable = flammable;
		codec = simpleCodec(p -> new PipingBlock(p, type, flammable));
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
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		double d = 2;
		double[] dd = new double[6];
		for (Direction dir : Direction.values())
			dd[dir.ordinal()] = state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir)) ? 0 : d;
		return Block.box(dd[Direction.WEST.ordinal()], dd[Direction.DOWN.ordinal()], dd[Direction.NORTH.ordinal()],
				16-dd[Direction.EAST.ordinal()], 16-dd[Direction.UP.ordinal()], 16-dd[Direction.SOUTH.ordinal()]);
	}

	@Override
	protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return Shapes.empty();
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return true;
	}

	@Override
	protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos from, boolean moving) {
		super.neighborChanged(state, world, pos, block, from, moving);
		if (!world.isClientSide && world.getBlockEntity(pos) instanceof PipingBlockEntity te)
			te.recomputeConnections(world, pos);
	}

	/** The face clicked when placing, for the bypass (original ItemMachinePlacer -> onPlacedAgainst). */
	private static final ThreadLocal<Direction> placedAgainst = new ThreadLocal<>();

	@Override
	public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
		placedAgainst.set(ctx.getClickedFace().getOpposite());
		return super.getStateForPlacement(ctx);
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);
		Direction against = placedAgainst.get();
		placedAgainst.remove();
		if (!world.isClientSide && world.getBlockEntity(pos) instanceof PipingBlockEntity te) {
			if (against != null && te instanceof reika.rotarycraft.content.piping.FlowPipeBlockEntity f)
				f.onPlacedAgainst(against);
			te.recomputeConnections(world, pos);
		}
	}

	/** Original BlockBasicMultiTE.canHarvest: pipes can always be harvested (except in creative mode). */
	@Override
	protected boolean canHarvest(Player ep, BlockEntity te) {
		return !ep.isCreative();
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		float f = (float)Math.min(Math.max(RotaryConfig.PIPEHARDNESS.get(), 0), 1);
		if (f <= 0)
			return 1F;
		return player.getDigSpeed(state, pos)/f/30F;
	}

	@Override
	public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof PipingBlockEntity te ? te.getLightLevel() : 0;
	}

	@Override
	public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return flammable ? 45 : 0;
	}

	/** Original: RotaryEventManager.burnLubricantHose - fire consuming a hose sets the neighbouring hoses alight. */
	@Override
	public void onCaughtFire(BlockState state, Level world, BlockPos pos, @Nullable Direction direction, @Nullable LivingEntity igniter) {
		if (!world.isClientSide && world.getBlockEntity(pos) instanceof HoseBlockEntity h)
			h.burn();
	}
}
