/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import reika.rotarycraft.content.blockentity.BedrockBreakerBlockEntity;
import reika.rotarycraft.registry.RotaryBlockEntities;

/** Original: MachineRegistry.BEDROCKBREAKER in BlockDMMachine (6-sided, X/Z-flipped placement); right-click pops out the collected dust. */
public class BedrockBreakerBlock extends MachineBlock {

	private static final MapCodec<BedrockBreakerBlock> CODEC = simpleCodec(BedrockBreakerBlock::new);

	public BedrockBreakerBlock(Properties props) {
		super(props, RotaryBlockEntities.BEDROCK_BREAKER, Placement.SIX_SIDED_XZ_FLIPPED, false);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player ep, BlockHitResult hit) {
		if (ep.isShiftKeyDown() || !(world.getBlockEntity(pos) instanceof BedrockBreakerBlockEntity tb))
			return InteractionResult.PASS;
		if (!world.isClientSide && tb.isPlayerAccessible(ep)) {
			tb.dropInventory();
			tb.syncAllData();
		}
		return InteractionResult.sidedSuccess(world.isClientSide);
	}
}
