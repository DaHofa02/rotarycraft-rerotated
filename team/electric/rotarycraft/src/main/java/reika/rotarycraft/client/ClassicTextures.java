/*******************************************************************************
 * Private NeoForge 1.21.1 ports of RotaryCraft, ReactorCraft and ElectriCraft (not part of the original mods): the mods' textures are
 * restyled in a modern look (tools/modernize_textures.py); the original 1.7.10 textures stay in each mod as the built-in resource pack
 * "classic" (resourcepacks/classic in the mod), which can be switched on in the resource pack screen.
 ******************************************************************************/
package reika.rotarycraft.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import reika.rotarycraft.RotaryCraft;

@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClassicTextures {

	private ClassicTextures() {}

	@SubscribeEvent
	public static void packs(AddPackFindersEvent e) {
		if (e.getPackType() != PackType.CLIENT_RESOURCES)
			return;
		String[][] mods = {{"rotarycraft", "RotaryCraft"}, {"reactorcraft", "ReactorCraft"}, {"electricraft", "ElectriCraft"}};
		for (String[] m : mods)
			if (ModList.get().isLoaded(m[0]))
				e.addPackFinders(ResourceLocation.fromNamespaceAndPath(m[0], "resourcepacks/classic"), PackType.CLIENT_RESOURCES,
						Component.literal(m[1]+" Classic Textures"), PackSource.BUILT_IN, false, Pack.Position.TOP);
	}
}
