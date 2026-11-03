/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import reika.reactorcraft.radiation.RadiationEffects;
import reika.reactorcraft.radiation.RadiationIntensity;

/** Original: ItemPlutonium - plutonium fuel (100 steps like uranium); carried by a player who is not in creative it gives moderate radiation sickness. */
public class PlutoniumItem extends FuelItem {

	public PlutoniumItem(Properties props, int steps) {
		super(props, steps);
	}

	@Override
	public void inventoryTick(ItemStack is, Level world, Entity e, int slot, boolean selected) {
		if (!world.isClientSide && e instanceof Player ep && !ep.isCreative())
			RadiationEffects.applyEffects(ep, RadiationIntensity.MODERATE);
	}
}
