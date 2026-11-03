/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Original: ReactorItems.BREEDERFUEL (ItemReactorFuel, 20 steps) - "(x% Converted)" in 5 % steps; after the last step a breeder core makes plutonium of it. */
public class BreederFuelItem extends FuelItem {

	public BreederFuelItem(Properties props, int steps) {
		super(props, steps);
	}

	@Override
	public Component getName(ItemStack is) {
		return Component.empty().append(Component.translatable(this.getDescriptionId(is))).append(" ("+(getDepletion(is)*5)+"% Converted)");
	}
}
