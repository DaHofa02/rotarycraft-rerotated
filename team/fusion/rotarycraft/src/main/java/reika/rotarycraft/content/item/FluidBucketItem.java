/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * Original: Reika.RotaryCraft.Items.Tools.ItemFuelLubeBucket (lubricant, jet fuel, ethanol bucket), registered as 1000 mB fluid
 * containers with the empty bucket as container item. The fluids have no world block, so the bucket cannot be emptied into the
 * world; it is filled and emptied by machines and tanks (vanilla BucketItem, so NeoForge's bucket fluid handler works with it).
 */
public class FluidBucketItem extends BucketItem {

	public FluidBucketItem(Fluid content, Properties props) {
		super(content, props);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player ep, InteractionHand hand) {
		return InteractionResultHolder.pass(ep.getItemInHand(hand));
	}
}
