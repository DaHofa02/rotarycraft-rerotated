/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import reika.rotarycraft.content.block.CanolaBlock;
import reika.rotarycraft.registry.RotaryBlocks;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: Reika.RotaryCraft.Items.ItemCanolaSeed (seeds and dense seeds). Seeds plant one canola crop on moist farmland; dense
 * seeds plant a 3x3 field around the target and drop a plain seed wherever a crop cannot go. Both use up one item.
 */
public class CanolaSeedItem extends Item {

	private final boolean spread;

	public CanolaSeedItem(boolean spread, Properties props) {
		super(props);
		this.spread = spread;
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Level world = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		Player ep = ctx.getPlayer();
		ItemStack is = ctx.getItemInHand();
		if (!world.getBlockState(pos).canBeReplaced())
			pos = pos.relative(ctx.getClickedFace());
		boolean flag = false;
		if (world.getBlockState(pos).canBeReplaced() && CanolaBlock.isValidFarmBlock(world, pos.below())) {
			if (world.isClientSide)
				return InteractionResult.SUCCESS;
			int r = spread ? 1 : 0;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					BlockPos p = pos.offset(dx, 0, dz);
					if (!world.getBlockState(p).canBeReplaced() || !CanolaBlock.isValidFarmBlock(world, p.below())
							|| (ep != null && !ep.mayUseItemAt(p, ctx.getClickedFace(), is))) {
						net.minecraft.world.Containers.dropItemStack(world, p.getX()+0.5, p.getY()+0.5, p.getZ()+0.5, new ItemStack(RotaryItems.CANOLA_SEEDS.get()));
					}
					else {
						world.setBlockAndUpdate(p, RotaryBlocks.CANOLA.get().defaultBlockState());
						flag = true;
					}
				}
			}
			if (ep == null || !ep.isCreative())
				is.shrink(1);
		}
		return flag ? InteractionResult.CONSUME : InteractionResult.PASS;
	}
}
