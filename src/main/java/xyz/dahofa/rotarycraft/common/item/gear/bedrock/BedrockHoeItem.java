package xyz.dahofa.rotarycraft.common.item.gear.bedrock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class BedrockHoeItem extends HoeItem {
    private final int range;

    public BedrockHoeItem(Tier tier, int range, Properties properties) {
        super(tier, properties);
        this.range = range;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var player = context.getPlayer();

        if (player == null)
            return InteractionResult.FAIL;

        var level = context.getLevel();
        var pos = context.getClickedPos();

        var playedSound = false;

        if (till(context, pos)) {
            level.playSound(player, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);

            playedSound = true;

            if (!player.isCrouching())
                return InteractionResult.FAIL;
        }

        if (player.isCrouching()) {
            var positions = BlockPos.betweenClosedStream(pos.offset(-this.range, 0, -this.range), pos.offset(this.range, 0, this.range)).iterator();

            while (positions.hasNext()) {
                var aoePos = positions.next();

                if (till(context, aoePos) && !playedSound) {
                    level.playSound(player, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);

                    playedSound = true;
                }
            }
        }

        return InteractionResult.PASS;
    }

    private static boolean till(UseOnContext context, BlockPos pos) {
        var level = context.getLevel();
        var direction = context.getClickedFace();

        if (direction != Direction.DOWN && level.isEmptyBlock(pos.above())) {
            var modifiedState = level.getBlockState(pos).getToolModifiedState(context, ItemAbilities.HOE_TILL, false);
            if (modifiedState != null) {
                if (!level.isClientSide()) {
                    var stack = context.getItemInHand();
                    var player = context.getPlayer();

                    level.setBlock(pos, modifiedState, 11);
                    level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, modifiedState));

                    if (player != null) {
                        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                    }
                }

                return true;
            }
        }

        return false;
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

