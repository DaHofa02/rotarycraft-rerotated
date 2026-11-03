/*******************************************************************************
 * Test tool of the private NeoForge 1.21.1 ports (not part of the original mods): with the system property "reactorcraft.showcase.items"
 * (the itemGallery run) shows every item of RotaryCraft, ReactorCraft and ElectriCraft page by page, drawn by the game's item renderer
 * at twice the inventory size with its id below, takes a screenshot of each page (run/screenshots/items_<n>.png) and quits - to look at
 * every item as it appears in the inventory.
 ******************************************************************************/
package reika.reactorcraft.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import reika.reactorcraft.ReactorCraft;

@EventBusSubscriber(modid = ReactorCraft.MOD_ID, value = Dist.CLIENT)
public final class ItemGallery {

	private static final boolean ACTIVE = System.getProperty("reactorcraft.showcase.items") != null;
	private static final int COLS = 12, ROWS = 6;

	private static List<Item> items;
	private static int ticks;

	private ItemGallery() {}

	private static final class Page extends Screen {
		private final int page;

		Page(int page) {
			super(Component.literal("items"));
			this.page = page;
		}

		@Override
		public void render(GuiGraphics g, int mx, int my, float pt) {
			g.fill(0, 0, width, height, 0xFF8B8B8B);
			int cw = width/COLS, ch = height/ROWS;
			for (int i = 0; i < COLS*ROWS; i++) {
				int k = page*COLS*ROWS+i;
				if (k >= items.size())
					break;
				int x = (i%COLS)*cw, y = (i/COLS)*ch;
				g.pose().pushPose();
				g.pose().translate(x+cw/2F-16, y+4, 0);
				g.pose().scale(2, 2, 1);
				g.renderItem(new ItemStack(items.get(k)), 0, 0);
				g.pose().popPose();
				String id = BuiltInRegistries.ITEM.getKey(items.get(k)).getPath();
				g.pose().pushPose();
				g.pose().translate(x+2, y+40, 0);
				g.pose().scale(0.5F, 0.5F, 1);
				g.drawString(font, id.length() > 28 ? id.substring(0, 28) : id, 0, 0, 0xFFFFFFFF, true);
				g.pose().popPose();
			}
		}

		@Override
		public boolean isPauseScreen() {
			return false;
		}
	}

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post e) {
		if (!ACTIVE)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null)
			return;
		if (items == null) {
			items = new ArrayList<>();
			Set<String> mods = Set.of("rotarycraft", "reactorcraft", "electricraft");
			for (Item i : BuiltInRegistries.ITEM)
				if (mods.contains(BuiltInRegistries.ITEM.getKey(i).getNamespace()))
					items.add(i);
		}
		ticks++;
		if (ticks < 60)
			return;
		int pages = (items.size()+COLS*ROWS-1)/(COLS*ROWS);
		int page = (ticks-60)/20, phase = (ticks-60)%20;
		if (page >= pages) {
			mc.stop();
			return;
		}
		if (phase == 0)
			mc.setScreen(new Page(page));
		if (phase == 15)
			Screenshot.grab(mc.gameDirectory, "items_"+page+".png", mc.getMainRenderTarget(), msg -> ReactorCraft.LOGGER.info("item gallery: {}", msg.getString()));
	}
}
