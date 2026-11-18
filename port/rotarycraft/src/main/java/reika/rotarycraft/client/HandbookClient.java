/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.client.screen.HandbookScreen;

/**
 * Loads a mod's handbook (assets/&lt;mod&gt;/handbook/&lt;language&gt;.json, falling back to en_us) and opens its screen. RotaryCraft,
 * ReactorCraft and ElectriCraft each have their own book. The file holds categories, each with entries {id, icon, text}; an entry's icon
 * defaults to the mod's item of its id, and its title is the icon item's name unless it has its own "title".
 */
public final class HandbookClient {

	public record Entry(String id, String category, ResourceLocation icon, String title, String text) {}

	private HandbookClient() {}

	public static void open(String mod) {
		Minecraft mc = Minecraft.getInstance();
		List<Entry> entries = load(mc, mod);
		String at = null;
		if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK && mc.level != null) {
			ResourceLocation id = BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(((BlockHitResult)mc.hitResult).getBlockPos()).getBlock());
			if (id.getNamespace().equals(mod))
				at = id.getPath();
		}
		mc.setScreen(new HandbookScreen(Component.translatable("item."+mod+".handbook"), entries, at));
	}

	public static List<Entry> load(Minecraft mc, String mod) {
		String lang = mc.getLanguageManager().getSelected();
		Optional<Resource> r = mc.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(mod, "handbook/"+lang+".json"));
		if (r.isEmpty())
			r = mc.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(mod, "handbook/en_us.json"));
		List<Entry> li = new ArrayList<>();
		if (r.isEmpty())
			return li;
		try (Reader rd = new InputStreamReader(r.get().open(), StandardCharsets.UTF_8)) {
			JsonArray cats = JsonParser.parseReader(rd).getAsJsonObject().getAsJsonArray("categories");
			for (JsonElement ce : cats) {
				JsonObject c = ce.getAsJsonObject();
				String cat = c.get("title").getAsString();
				for (JsonElement ee : c.getAsJsonArray("entries")) {
					JsonObject e = ee.getAsJsonObject();
					ResourceLocation icon = ResourceLocation.parse(e.has("icon") ? e.get("icon").getAsString() : mod+":"+e.get("id").getAsString());
					String title = e.has("title") ? e.get("title").getAsString() : BuiltInRegistries.ITEM.get(icon).getDescription().getString();
					li.add(new Entry(e.get("id").getAsString(), cat, icon, title, e.get("text").getAsString()));
				}
			}
		}
		catch (Exception ex) {
			RotaryCraft.LOGGER.error("Could not read the "+mod+" handbook", ex);
		}
		return li;
	}
}
