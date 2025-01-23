package xyz.dahofa.rotarycraft.common.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import xyz.dahofa.rotarycraft.api.lib.Names;
import xyz.dahofa.rotarycraft.common.registry.RCItems;

import java.util.HashSet;
import java.util.Set;

import static xyz.dahofa.rotarycraft.common.item.gear.bedrock.BedrockHoeItem.till3x3;

@EventBusSubscriber(modid = Names.MOD_ID)
public class RCEvents {
    private static final Set<BlockPos> TILLED_BLOCKS = new HashSet<>();


    // Done with the help of https://github.com/CoFH/CoFHCore/blob/1.19.x/src/main/java/cofh/core/event/AreaEffectEvents.java
    // Don't be a jerk License
    @SubscribeEvent
    public static void onHoeUsage(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        ItemStack itemStack = event.getItemStack();

        // Check if the player is holding a hoe (you can replace this with your custom hoe)
        if (itemStack.getItem() == RCItems.BEDROCK_HOE.get()) {
            if (!level.isClientSide) {
                till3x3(level, pos, event);

                // Cancel the default behavior
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }
}
