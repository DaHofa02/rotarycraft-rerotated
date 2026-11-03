/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.item;

import java.util.Comparator;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import reika.reactorcraft.content.entity.RadiationEntity;
import reika.reactorcraft.registry.ReactorComponents;
import reika.reactorcraft.registry.ReactorEffects;

/**
 * Original: ItemGeigerCounter (ChargeableTool; the charge in kJ was the item damage, here a component) - held and charged it clicks:
 * constantly while its holder has radiation sickness, otherwise for the nearest radiation cloud within 20 blocks more often the closer
 * it is; every tick it uses one kJ with 1 in 8. Charged in the RotaryCraft coil winder (not ported yet; creative has a charged one).
 */
public class GeigerCounterItem extends Item implements reika.rotarycraft.api.interfaces.ChargeableTool {

	public static final int CREATIVE_CHARGE = 32000;

	public GeigerCounterItem(Properties props) {
		super(props.stacksTo(1));
	}

	public static int getCharge(ItemStack is) {
		return is.getOrDefault(ReactorComponents.CHARGE.get(), 0);
	}

	public static ItemStack charged(Item item, int charge) {
		ItemStack is = new ItemStack(item);
		is.set(ReactorComponents.CHARGE.get(), charge);
		return is;
	}

	@Override
	public void inventoryTick(ItemStack is, Level world, Entity e, int slot, boolean currentHeld) {
		int charge = getCharge(is);
		if (!currentHeld || charge <= 0)
			return;
		int r = 20;
		if (e instanceof LivingEntity le && le.hasEffect(ReactorEffects.RADIATION))
			r = -1;
		if (r < 0) {
			e.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 2, 2F);
		}
		else {
			List<RadiationEntity> li = world.getEntitiesOfClass(RadiationEntity.class, new AABB(e.position(), e.position()).inflate(r));
			if (!li.isEmpty()) {
				RadiationEntity er = li.stream().min(Comparator.comparingDouble(x -> x.distanceToSqr(e))).get();
				double dist = e.position().distanceTo(er.position());
				if (world.random.nextDouble()*r*16 > dist*dist)
					e.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1, 2F);
			}
		}
		if (!world.isClientSide && world.random.nextInt(8) == 0)
			is.set(ReactorComponents.CHARGE.get(), charge-1);
	}

	@Override
	public void appendHoverText(ItemStack is, TooltipContext ctx, List<Component> li, TooltipFlag flag) {
		li.add(Component.literal("Charge: "+getCharge(is)+" kJ"));
	}

	/** Original setCharged (worktable with a wound coil): the tool takes the coil's charge, the coil keeps the tool's old one. */
	@Override
	public int setCharged(ItemStack is, int charge, boolean strongcoil) {
		int ret = is.getOrDefault(ReactorComponents.CHARGE.get(), 0);
		is.set(ReactorComponents.CHARGE.get(), charge);
		return ret;
	}
}
