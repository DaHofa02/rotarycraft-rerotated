/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;

import reika.rotaryme.BundledBusBlockEntity;
import reika.rotaryme.BundledBusMenu;

/** Own drawn layout: each filter slot framed in its dye colour (dimmed when its channel is not powered), the active channel count. */
public class BundledBusScreen extends AbstractContainerScreen<BundledBusMenu> {

	public BundledBusScreen(BundledBusMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
		inventoryLabelY = 72;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int x = leftPos, y = topPos;
		g.fill(x, y, x+imageWidth, y+imageHeight, 0xff23262e);
		g.fill(x+2, y+2, x+imageWidth-2, y+imageHeight-2, 0xff8a93a6);
		int active = menu.tile.getActiveChannels();
		for (Slot s : menu.slots) {
			int col = 0xff6e727a;
			if (s.index < BundledBusBlockEntity.CHANNELS) {
				int rgb = DyeColor.byId(s.index).getTextureDiffuseColor() & 0xffffff;
				col = (s.index < active ? 0xff000000 : 0x80000000) | rgb;
				g.fill(x+s.x-2, y+s.y-2, x+s.x+18, y+s.y+18, col);
			}
			g.fill(x+s.x-1, y+s.y-1, x+s.x+17, y+s.y+17, 0xff5c606a);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		super.renderLabels(g, mx, my);
		g.drawString(font, menu.tile.getActiveChannels()+"/16 channels", 100, 6, 0xff202020, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
