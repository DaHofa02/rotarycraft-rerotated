/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Original: ReactorItems.MAGNET (ItemReactorMulti, 8 metadata values, one sprite) - permanent magnets of 4^level/1000 tesla, made in the
 * RotaryCraft compactor from lodestone and from the magnet one level weaker; the two strongest go into the solenoid magnets.
 */
public class MagnetItem extends Item {

	public final int level;

	public MagnetItem(Properties props, int level) {
		super(props);
		this.level = level;
	}

	/** Original getMultiValuedName: the field strength with an SI prefix. */
	@Override
	public Component getName(ItemStack is) {
		double num = Math.pow(4, level)/1000D;
		String prefix = "";
		if (num < 1) {
			num *= 1000;
			prefix = "m";
		}
		return Component.translatable(this.getDescriptionId(is)).append(String.format(" (%.3f %sT)", num, prefix));
	}
}
