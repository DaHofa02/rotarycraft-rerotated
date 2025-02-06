package xyz.dahofa.rotarycraft.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import xyz.dahofa.rotarycraft.common.registry.RCItems;

public class CanolaSeedBlock extends CropBlock {
    public static final int MAX_AGE = 9;
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, MAX_AGE);

    public CanolaSeedBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_AGE[this.getAge(state)];
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return RCItems.CANOLA_SEEDS.get();
    }

    @Override
    public IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return MAX_AGE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    private static final VoxelShape[] SHAPE_BY_AGE;

    static {
        SHAPE_BY_AGE = new VoxelShape[]{
                Block.box((double) 0.0F, (double) 0.0F, (double) 0.0F, (double) 16.0F, (double) 4.0F, (double) 16.0F),
                Block.box((double) 0.0F, (double) 0.0F, (double) 0.0F, (double) 16.0F, (double) 7.0F, (double) 16.0F),
                Block.box((double) 0.0F, (double) 0.0F, (double) 0.0F, (double) 16.0F, (double) 9.0F, (double) 16.0F),
                Block.box((double) 0.0F, (double) 0.0F, (double) 0.0F, (double) 16.0F, (double) 9.0F, (double) 16.0F),
                Block.box((double) 0.0F, (double) 0.0F, (double) 0.0F, (double) 16.0F, (double) 9.0F, (double) 16.0F),
                Block.box((double) 0.0F, (double) 0.0F, (double) 0.0F, (double) 16.0F, (double) 12.0F, (double) 16.0F),
                Block.box((double) 0.0F, (double) 0.0F, (double) 0.0F, (double) 16.0F, (double) 14.0F, (double) 16.0F),
                Block.box((double) 0.0F, (double) 0.0F, (double) 0.0F, (double) 16.0F, (double) 16.0F, (double) 16.0F),
                Block.box((double) 0.0F, (double) 0.0F, (double) 0.0F, (double) 16.0F, (double) 16.0F, (double) 16.0F),
                Block.box((double) 0.0F, (double) 0.0F, (double) 0.0F, (double) 16.0F, (double) 16.0F, (double) 16.0F)
        };
    }
}
