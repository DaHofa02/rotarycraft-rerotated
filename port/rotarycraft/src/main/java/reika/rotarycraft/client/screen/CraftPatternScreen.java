/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import reika.rotarycraft.content.menu.CraftPatternMenu;

/** Own layout (drawn): the 3x3 ghost grid, an arrow and the result. */
public class CraftPatternScreen extends AbstractContainerScreen<CraftPatternMenu> {

	public CraftPatternScreen(CraftPatternMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
		inventoryLabelY = 72;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int x = leftPos, y = topPos;
		g.fill(x, y, x+imageWidth, y+imageHeight, 0xff2b2d33);
		g.fill(x+2, y+2, x+imageWidth-2, y+imageHeight-2, 0xffb4b0a4);
		for (Slot s : menu.slots)
			g.fill(x+s.x-1, y+s.y-1, x+s.x+17, y+s.y+17, s.index < 9 ? 0xff7a8a6a : s.index == 9 ? 0xff8a7a4a : 0xff6e727a);
		g.fill(x+92, y+42, x+116, y+46, 0xff404040);
		g.fill(x+112, y+38, x+116, y+50, 0xff404040);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
