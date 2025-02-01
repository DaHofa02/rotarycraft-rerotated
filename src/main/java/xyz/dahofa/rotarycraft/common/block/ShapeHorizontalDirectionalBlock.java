package xyz.dahofa.rotarycraft.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class ShapeHorizontalDirectionalBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Grinder> CODEC = simpleCodec(Grinder::new);
    private static final VoxelShape SHAPE = makeShape();
    private static final VoxelShape SHAPE_NORTH = rotateShape(0, SHAPE); //super
    private static final VoxelShape SHAPE_SOUTH = rotateShape(2, SHAPE);
    private static final VoxelShape SHAPE_WEST = rotateShape(3, SHAPE);
    private static final VoxelShape SHAPE_EAST = rotateShape(1, SHAPE); //right

    public ShapeHorizontalDirectionalBlock(Properties properties) {
        super(properties.noOcclusion());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> SHAPE_NORTH;
            case UP -> SHAPE_NORTH;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
        };
    }


    //to implement rotation for voxel shapes with absulute direction
    public static VoxelShape rotateShape(int rotation, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{shape, Shapes.empty()};

        for (int i = 0; i < rotation; i++) {
            shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[0] = Shapes.or(buffer[0], Shapes.create(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            shape = buffer[0];
            buffer[0] = Shapes.empty();
        }

        return buffer[0];
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }


    public static VoxelShape makeShape() {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, Shapes.box(0, 0, 0, 1, 0.0625, 1), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.9375, 0.5625, 0, 1, 0.75, 1), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0, 0.5625, 0, 0.0625, 0.75, 1), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.0625, 0, 0.9375, 0.6875, 0.0625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.0625, 0.9375, 0.9375, 0.8125, 1), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.9375, 0.0625, 0.875, 1, 0.5625, 1), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.9375, 0.0625, 0, 1, 0.5625, 0.125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0, 0.0625, 0, 0.0625, 0.5625, 0.125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0, 0.0625, 0.875, 0.0625, 0.5625, 1), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.125, 0.9375, 0.5625, 0.1875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.125, 0.9375, 0.5625, 0.1875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.1875, 0.9375, 0.5625, 0.25), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.1875, 0.9375, 0.5625, 0.25), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.25, 0.9375, 0.5625, 0.3125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.25, 0.9375, 0.5625, 0.3125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.3125, 0.9375, 0.5625, 0.375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.3125, 0.9375, 0.5625, 0.375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.5625, 0.9375, 0.5625, 0.625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.5625, 0.9375, 0.5625, 0.625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.375, 0.9375, 0.5625, 0.4375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.375, 0.9375, 0.5625, 0.4375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.4375, 0.9375, 0.5625, 0.5), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.4375, 0.9375, 0.5625, 0.5), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.5, 0.9375, 0.5625, 0.5625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.5, 0.9375, 0.5625, 0.5625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.8125, 0.9375, 0.5625, 0.875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.8125, 0.9375, 0.5625, 0.875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.625, 0.9375, 0.5625, 0.6875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.625, 0.9375, 0.5625, 0.6875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.6875, 0.9375, 0.5625, 0.75), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.6875, 0.9375, 0.5625, 0.75), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.75, 0.9375, 0.5625, 0.8125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.5625, 0.1875, 0.75, 0.9375, 0.5625, 0.8125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.125, 0.4375, 0.5625, 0.1875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.125, 0.4375, 0.5625, 0.1875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.1875, 0.4375, 0.5625, 0.25), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.1875, 0.4375, 0.5625, 0.25), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.25, 0.4375, 0.5625, 0.3125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.25, 0.4375, 0.5625, 0.3125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.3125, 0.4375, 0.5625, 0.375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.3125, 0.4375, 0.5625, 0.375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.375, 0.4375, 0.5625, 0.4375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.375, 0.4375, 0.5625, 0.4375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.4375, 0.4375, 0.5625, 0.5), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.4375, 0.4375, 0.5625, 0.5), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.5, 0.4375, 0.5625, 0.5625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.5, 0.4375, 0.5625, 0.5625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.5625, 0.4375, 0.5625, 0.625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.5625, 0.4375, 0.5625, 0.625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.625, 0.4375, 0.5625, 0.6875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.625, 0.4375, 0.5625, 0.6875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.6875, 0.4375, 0.5625, 0.75), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.6875, 0.4375, 0.5625, 0.75), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.75, 0.4375, 0.5625, 0.8125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.75, 0.4375, 0.5625, 0.8125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.8125, 0.4375, 0.5625, 0.875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.1875, 0.8125, 0.4375, 0.5625, 0.875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.3125, 0.3125, 0.875, 0.6875, 0.6875, 0.9375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.3125, 0.3125, 0.875, 0.6875, 0.6875, 0.9375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.3125, 0.3125, 0.875, 0.6875, 0.6875, 0.9375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.3125, 0.3125, 0.875, 0.6875, 0.6875, 0.9375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.6875, 0.3125, 0.03125, 0.8125, 0.4375, 0.96875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.6875, 0.3125, 0.03125, 0.8125, 0.4375, 0.96875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.1875, 0.3125, 0.03125, 0.3125, 0.4375, 0.96875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.1875, 0.3125, 0.03125, 0.3125, 0.4375, 0.96875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.4375, 0.4375, 0.90625, 0.5625, 0.5625, 1.03125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.4375, 0.4375, 0.90625, 0.5625, 0.5625, 1.03125), BooleanOp.OR);
        //TODO:  get that out!
        /*final VoxelShape[] buffer = {Shapes.empty()};
        for (int i = 0; i < rotation; i++) {
            shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[0] = Shapes.or(buffer[0], Shapes.create(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            shape = buffer[0];
            buffer[0] = Shapes.empty();
        }*/
        shape.optimize();
        return shape;
    }
}
