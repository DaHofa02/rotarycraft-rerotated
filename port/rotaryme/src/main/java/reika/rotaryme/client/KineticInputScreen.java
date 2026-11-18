/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme.client;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import reika.rotaryme.KineticInputBlockEntity;
import reika.rotaryme.KineticInputMenu;

/** Own drawn layout: accepted power, loss (with the cards in the right column), connection, and the player inventory. */
public class KineticInputScreen extends AbstractContainerScreen<KineticInputMenu> {

	public KineticInputScreen(KineticInputMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 196;
		imageHeight = 202;
		inventoryLabelY = 109;
	}

	/** Slot frames, card slots tinted. */
	static void slots(GuiGraphics g, int x, int y, List<Slot> slots) {
		for (Slot s : slots)
			g.fill(x+s.x-1, y+s.y-1, x+s.x+17, y+s.y+17, s instanceof net.neoforged.neoforge.items.SlotItemHandler ? 0xff5a5f8a : 0xff6e727a);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int x = leftPos, y = topPos;
		g.fill(x, y, x+imageWidth, y+imageHeight, 0xff23262e);
		g.fill(x+2, y+2, x+imageWidth-2, y+imageHeight-2, 0xff8a93a6);
		slots(g, x, y, menu.slots);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		KineticInputBlockEntity te = menu.tile;
		int c = 0xff202020;
		g.drawString(font, title, 8, 6, c, false);
		g.drawString(font, "Input: "+te.getPower()+" W", 8, 24, c, false);
		g.drawString(font, String.format("Loss: %d %% (%d cards)", Math.round(te.getLoss()*100), te.getCards()), 8, 36, c, false);
		g.drawString(font, te.isConnected() ? "Connected" : "No ME network or channel", 8, 48, te.isConnected() ? c : 0xff902020, false);
		g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, c, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
