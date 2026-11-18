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
 * A mod's handbook (the port's own text, not the original's): a right click opens it, at the entry of the machine you are looking at if it
 * is one of that mod's. RotaryCraft, ReactorCraft and ElectriCraft each register one with their mod id.
 */
public class HandbookItem extends Item {

	private final String mod;

	public HandbookItem(Properties props) {
		this(props, reika.rotarycraft.RotaryCraft.MOD_ID);
	}

	public HandbookItem(Properties props, String mod) {
		super(props.stacksTo(1));
		this.mod = mod;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player ep, InteractionHand hand) {
		if (world.isClientSide)
			reika.rotarycraft.client.HandbookClient.open(mod);
		return InteractionResultHolder.sidedSuccess(ep.getItemInHand(hand), world.isClientSide);
	}
}
