package xyz.dahofa.rotarycraft.common.item.gear.bedrock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class BedrockHoeItem extends HoeItem {
    private final int range;

    public BedrockHoeItem(Tier tier, int range, Properties properties) {
        super(tier, properties);
        this.range = range;
    }

    public static void till3x3(Level level, BlockPos centerPos, PlayerInteractEvent.RightClickBlock event) {
        // Loop through the 3x3 area
        /*for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos pos = centerPos.offset(x, 0, z);
                BlockState state = level.getBlockState(pos);
                Block block = state.getBlock();

                // Check if the block is in the TILLABLES map
                if (TILLABLES.containsKey(block)) {
                    Pair<Predicate<UseOnContext>, Consumer<UseOnContext>> pair = TILLABLES.get(block);
                    Predicate<UseOnContext> predicate = pair.getFirst();
                    Consumer<UseOnContext> consumer = pair.getSecond();

                    // Create a UseOnContext for the block
                    UseOnContext context = new UseOnContext(event.getEntity(), event.getHand(), event.getHitVec());

                    // Check if the predicate allows tilling
                    if (predicate.test(context)) {
                        // Perform the tilling action
                        consumer.accept(context);
                    }
                }
            }*/
    }
}

