/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.registry;

import com.mojang.serialization.Codec;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

import reika.rotarycraft.RotaryCraft;

/**
 * Item values that the original kept in code tables, as NeoForge data maps (data/rotarycraft/data_maps/item/...).
 * <ul>
 * <li>{@link #MULCH_VALUE}: original MulchMaterials / PlantMaterials - how much sludge the Fermenter makes from one plant item.</li>
 * </ul>
 */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class RotaryDataMaps {

	public static final DataMapType<Item, Integer> MULCH_VALUE = DataMapType.builder(
			ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "mulch_value"), Registries.ITEM, Codec.intRange(1, 64)).synced(Codec.intRange(1, 64), false).build();

	private RotaryDataMaps() {}

	@SubscribeEvent
	public static void register(RegisterDataMapTypesEvent e) {
		e.register(MULCH_VALUE);
	}

	/** Original MulchMaterials.getPlantValue: 0 for anything that is no plant material. */
	public static int getPlantValue(ItemStack is) {
		if (is.isEmpty())
			return 0;
		Integer v = is.getItemHolder().getData(MULCH_VALUE);
		return v == null ? 0 : v;
	}
}
