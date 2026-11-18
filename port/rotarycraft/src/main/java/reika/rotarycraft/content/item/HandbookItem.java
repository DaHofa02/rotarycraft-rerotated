/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The RotaryCraft handbook (the port's own text, not the original's): a right click opens it, at the entry of the machine you are looking at
 * if there is one.
 */
public class HandbookItem extends Item {

	public HandbookItem(Properties props) {
		super(props.stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player ep, InteractionHand hand) {
		if (world.isClientSide)
			reika.rotarycraft.client.HandbookClient.open();
		return InteractionResultHolder.sidedSuccess(ep.getItemInHand(hand), world.isClientSide);
	}
}
