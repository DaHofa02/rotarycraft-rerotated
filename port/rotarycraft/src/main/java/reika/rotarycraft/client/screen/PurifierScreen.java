/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import reika.rotarycraft.content.blockentity.PurifierBlockEntity;
import reika.rotarycraft.content.menu.PurifierMenu;

/** Own drawn layout: slots, a progress bar to the output and a temperature bar (red above 600 C). */
public class PurifierScreen extends AbstractContainerScreen<PurifierMenu> {

	public PurifierScreen(PurifierMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
		inventoryLabelY = 72;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int x = leftPos, y = topPos;
		PurifierBlockEntity te = menu.tile;
		g.fill(x, y, x+imageWidth, y+imageHeight, 0xff2b2d33);
		g.fill(x+2, y+2, x+imageWidth-2, y+imageHeight-2, 0xffa09a92);
		for (Slot s : menu.slots)
			g.fill(x+s.x-1, y+s.y-1, x+s.x+17, y+s.y+17, s.index < 8 ? 0xff6a6058 : 0xff6e727a);
		int w = te.getOperationTime() <= 0 ? 0 : 14*te.cookTime/te.getOperationTime();
		g.fill(x+135, y+40, x+135+w, y+46, 0xffe0e0e0);
		int h = Math.max(0, Math.min(60, te.temperature*60/PurifierBlockEntity.MAXTEMP));
		g.fill(x+8, y+16, x+16, y+76, 0xff30323a);
		g.fill(x+9, y+76-h, x+15, y+76, te.temperature >= PurifierBlockEntity.SMELTTEMP ? 0xffd04020 : 0xffd0a040);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		super.renderLabels(g, mx, my);
		g.drawString(font, menu.tile.temperature+"C", 110, 6, 0xff202020, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
