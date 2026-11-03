/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.content.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import reika.rotarycraft.api.interfaces.TensionStorage;
import reika.rotarycraft.registry.RotaryComponents;

/**
 * Original: Reika.RotaryCraft.Items.ItemCoil - the wind spring (stiffness 1, power scale 1, can break while winding) and the
 * high-strength spring (stiffness 16, power scale 4, unbreakable). The charge in kJ (original item damage, up to 32000) is a component
 * and shows in the name.
 */
public class CoilItem extends Item implements TensionStorage {

	public static final int MAX_CHARGE = 32000;

	private final boolean strong;

	public CoilItem(Properties props, boolean strong) {
		super(props.stacksTo(1));
		this.strong = strong;
	}

	public static int getCharge(ItemStack is) {
		return is.getOrDefault(RotaryComponents.CHARGE.get(), 0);
	}

	public static ItemStack withCharge(ItemStack is, int charge) {
		is.set(RotaryComponents.CHARGE.get(), Math.max(0, Math.min(MAX_CHARGE, charge)));
		return is;
	}

	public boolean isStrong() {
		return strong;
	}

	@Override
	public int getStiffness(ItemStack is) {
		return strong ? 16 : 1;
	}

	@Override
	public int getPowerScale(ItemStack is) {
		return strong ? 4 : 1;
	}

	@Override
	public boolean isBreakable(ItemStack is) {
		return !strong;
	}

	/** Original getMultiValuedName: "<name> (N kJ)". */
	@Override
	public Component getName(ItemStack is) {
		return Component.translatable(this.getDescriptionId(is)).append(" ("+getCharge(is)+" kJ)");
	}
}
