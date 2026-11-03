/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.item;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import reika.reactorcraft.registry.ReactorComponents;

/**
 * Original: ItemReactorFuel (uranium fuel pellet; 100 depletion steps, "(Fresh)" / "(x% Depleted)"). The step is a component instead of
 * the item damage so that fresh pellets stack as in 1.7.10; a bar shows the remaining fuel.
 */
public class FuelItem extends Item {

	public final int steps;

	public FuelItem(Properties props, int steps) {
		super(props);
		this.steps = steps;
	}

	public static int getDepletion(ItemStack is) {
		return is.getOrDefault(ReactorComponents.DEPLETION.get(), 0);
	}

	@Override
	public Component getName(ItemStack is) {
		int d = getDepletion(is);
		Component base = super.getName(is);
		return d == 0 ? Component.empty().append(base).append(" (Fresh)") : Component.empty().append(base).append(" ("+(d*100/steps)+"% Depleted)");
	}

	@Override
	public boolean isBarVisible(ItemStack is) {
		return getDepletion(is) > 0;
	}

	@Override
	public int getBarWidth(ItemStack is) {
		return Math.round(13F-13F*getDepletion(is)/steps);
	}

	@Override
	public int getBarColor(ItemStack is) {
		return Mth.hsvToRgb(Math.max(0, 1F-(float)getDepletion(is)/steps)/3F, 1, 1);
	}
}
