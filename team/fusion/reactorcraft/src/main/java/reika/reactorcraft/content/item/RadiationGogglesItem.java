/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Original: ItemRadiationGoggles (ItemReactorTool, worn in the helmet slot) - with them on, radiation clouds become visible. */
public class RadiationGogglesItem extends Item implements Equipable {

	public RadiationGogglesItem(Properties props) {
		super(props.stacksTo(1));
	}

	@Override
	public EquipmentSlot getEquipmentSlot() {
		return EquipmentSlot.HEAD;
	}

	public static boolean isWearingGoggles(Player ep) {
		ItemStack helmet = ep.getItemBySlot(EquipmentSlot.HEAD);
		return helmet.getItem() instanceof RadiationGogglesItem;
	}
}
