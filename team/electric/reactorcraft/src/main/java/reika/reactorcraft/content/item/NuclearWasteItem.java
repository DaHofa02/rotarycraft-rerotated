/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.content.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import reika.reactorcraft.content.entity.NuclearWasteEntity;
import reika.reactorcraft.radiation.Isotopes;
import reika.reactorcraft.radiation.RadiationEffects;
import reika.reactorcraft.radiation.RadiationIntensity;
import reika.reactorcraft.registry.ReactorComponents;

/**
 * Original: ItemNuclearWaste - irradiates whoever carries it (high level), drops as a never-despawning radioactive item entity, and
 * names its isotope and half-life (or the element group of mixed waste) in the tooltip.
 */
public class NuclearWasteItem extends Item {

	public NuclearWasteItem(Properties props) {
		super(props);
	}

	@Override
	public void inventoryTick(ItemStack is, Level world, Entity e, int slot, boolean selected) {
		if (!world.isClientSide && e instanceof LivingEntity le)
			RadiationEffects.applyEffects(le, RadiationIntensity.HIGHLEVEL);
	}

	@Override
	public boolean hasCustomEntity(ItemStack stack) {
		return true;
	}

	@Nullable
	@Override
	public Entity createEntity(Level world, Entity location, ItemStack is) {
		NuclearWasteEntity ei = new NuclearWasteEntity(world, location.getX(), location.getY(), location.getZ(), is);
		ei.setDeltaMovement(location.getDeltaMovement());
		ei.setPickUpDelay(10);
		return ei;
	}

	@Override
	public void appendHoverText(ItemStack is, Item.TooltipContext ctx, List<Component> li, TooltipFlag flag) {
		Integer id = is.get(ReactorComponents.ISOTOPE.get());
		if (id == null)
			return;
		if (id >= 1000) {
			li.add(Component.literal("Mixed Waste: "+Isotopes.ElementGroup.values()[Math.min(id-1000, 3)].displayName));
		}
		else if (id < Isotopes.isoList.length) {
			Isotopes atom = Isotopes.isoList[id];
			li.add(atom.extraDanger ? Component.literal(atom.getName()).withStyle(ChatFormatting.RED) : Component.literal(atom.getName()));
			li.add(Component.literal("Half Life: "+atom.getHalfLifeAsDisplay()));
		}
	}

	public static ItemStack of(Isotopes atom) {
		ItemStack is = new ItemStack(reika.reactorcraft.registry.ReactorItems.WASTE.get());
		is.set(ReactorComponents.ISOTOPE.get(), atom.ordinal());
		return is;
	}
}
