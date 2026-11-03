/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.menu.WasteMenus;

/** Original: GuiWasteContainer (wastecontainer2.png, 175 high), GuiWasteStorage (wastestorage.png, 186) and GuiWasteDecayer (wastedecayer.png, 175) - ReactorGuiBase. */
public class WasteScreen<T extends WasteMenus.Base> extends AbstractContainerScreen<T> {

	private final ResourceLocation texture;

	public WasteScreen(T menu, Inventory inv, Component title, String tex, int height) {
		this(menu, inv, title, tex, 176, height);
	}

	public WasteScreen(T menu, Inventory inv, Component title, String tex, int width, int height) {
		super(menu, inv, title);
		texture = ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "textures/gui/"+tex+".png");
		imageWidth = width;
		imageHeight = height;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
		g.drawString(font, playerInventoryTitle, imageWidth-58, imageHeight-96+3, 4210752, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
