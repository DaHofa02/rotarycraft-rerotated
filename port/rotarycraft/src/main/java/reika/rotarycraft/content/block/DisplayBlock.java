/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import reika.rotarycraft.content.blockentity.DisplayBlockEntity;
import reika.rotarycraft.registry.RotaryBlockEntities;

/**
 * Original: MachineRegistry.DISPLAY in BlockMMachine - 4 orientations; right-clicked with a dye it takes that colour, with glowstone dust
 * argon blue, with a written book its pages as the message (joined with a space where a page does not end in one; BlockBasicMultiTE).
 * Otherwise a right click opens the coil slot.
 */
public class DisplayBlock extends MachineBlock {

	public DisplayBlock(Properties props) {
		super(props, RotaryBlockEntities.DISPLAY, Placement.FOUR_SIDED, false);
	}

	public static String readBook(WrittenBookContent book) {
		StringBuilder sb = new StringBuilder();
		var pages = book.getPages(false);
		for (int i = 0; i < pages.size(); i++) {
			String s = pages.get(i).getString();
			sb.append(s);
			if (i < pages.size()-1 && !s.endsWith(" "))
				sb.append(" ");
		}
		return sb.toString();
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level world, BlockPos pos, Player ep, InteractionHand hand, BlockHitResult hit) {
		if (world.getBlockEntity(pos) instanceof DisplayBlockEntity td) {
			if (held.getItem() instanceof DyeItem dye) {
				if (!world.isClientSide)
					td.setDyeColor(dye.getDyeColor());
				return ItemInteractionResult.sidedSuccess(world.isClientSide);
			}
			if (held.is(Items.GLOWSTONE_DUST)) {
				if (!world.isClientSide)
					td.setColorToArgon();
				return ItemInteractionResult.sidedSuccess(world.isClientSide);
			}
			if (held.is(Items.WRITTEN_BOOK) && held.has(DataComponents.WRITTEN_BOOK_CONTENT)) {
				if (!world.isClientSide)
					td.setMessage(readBook(held.get(DataComponents.WRITTEN_BOOK_CONTENT)));
				return ItemInteractionResult.sidedSuccess(world.isClientSide);
			}
		}
		return super.useItemOn(held, state, world, pos, ep, hand, hit);
	}
}
