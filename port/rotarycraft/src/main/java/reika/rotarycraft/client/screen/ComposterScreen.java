/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.ComposterBlockEntity;
import reika.rotarycraft.content.menu.ComposterMenu;

/** Original: GuiComposter (compostergui.png, a GuiNonPoweredMachine) - the progress arrow (48 wide) and the temperature column (54 high). */
public class ComposterScreen extends AbstractContainerScreen<ComposterMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/compostergui.png");

	public ComposterScreen(ComposterMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		ComposterBlockEntity te = menu.tile;
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		int i1 = te.getScaledTimer(48);
		g.blit(TEX, j+79, k+34, 176, 14, i1+1, 16);
		int i2 = Math.min(54, te.getScaledTemperature(54));
		if (i2 > 0)
			g.blit(TEX, j+24, k+70-i2, 177, 86-i2, 9, i2);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
		g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 4210752, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
