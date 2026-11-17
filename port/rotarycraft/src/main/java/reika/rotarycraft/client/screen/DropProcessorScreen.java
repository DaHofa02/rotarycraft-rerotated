/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import reika.rotarycraft.content.menu.DropProcessorMenu;

/** Own layout (drawn): input, a progress bar, output and the overflow count. */
public class DropProcessorScreen extends AbstractContainerScreen<DropProcessorMenu> {

	public DropProcessorScreen(DropProcessorMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
		inventoryLabelY = 72;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int x = leftPos, y = topPos;
		g.fill(x, y, x+imageWidth, y+imageHeight, 0xff2b2d33);
		g.fill(x+2, y+2, x+imageWidth-2, y+imageHeight-2, 0xffaaa69c);
		for (Slot s : menu.slots)
			g.fill(x+s.x-1, y+s.y-1, x+s.x+17, y+s.y+17, s.index < 2 ? 0xff7a6a5a : 0xff6e727a);
		g.fill(x+66, y+40, x+110, y+46, 0xff505050);
		g.fill(x+66, y+40, x+66+menu.tile.getProgressScaled(44), y+46, 0xffe08030);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		super.renderLabels(g, mx, my);
		if (menu.tile.getOverflowCount() > 0)
			g.drawString(font, "+"+menu.tile.getOverflowCount()+" waiting", 100, 56, 0xff303030, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
