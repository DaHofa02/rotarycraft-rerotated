/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import reika.rotarycraft.content.menu.LandmineMenu;

/** Own drawn layout: the charge row (red), the modifier row (grey) and the blast strength. */
public class LandmineScreen extends AbstractContainerScreen<LandmineMenu> {

	public LandmineScreen(LandmineMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
		inventoryLabelY = 72;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int x = leftPos, y = topPos;
		g.fill(x, y, x+imageWidth, y+imageHeight, 0xff2b2d33);
		g.fill(x+2, y+2, x+imageWidth-2, y+imageHeight-2, 0xff8c8a80);
		for (Slot s : menu.slots)
			g.fill(x+s.x-1, y+s.y-1, x+s.x+17, y+s.y+17, s.index < 4 ? 0xff8a4038 : s.index < 8 ? 0xff5a5a62 : 0xff6e727a);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		super.renderLabels(g, mx, my);
		g.drawString(font, "Blast "+(int)menu.tile.getExplosionPower(), 130, 6, 0xff202020, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
