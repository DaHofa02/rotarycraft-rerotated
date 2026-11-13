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
import reika.rotarycraft.content.blockentity.ObsidianMakerBlockEntity;
import reika.rotarycraft.content.menu.ObsidianMakerMenu;

/** Original: GuiObsidian (obsidiangui.png) - the water and lava gauges with tooltips and the power tab (power, speed, torque). */
public class ObsidianMakerScreen extends AbstractContainerScreen<ObsidianMakerMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/obsidiangui.png");
	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");

	public ObsidianMakerScreen(ObsidianMakerMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		ObsidianMakerBlockEntity te = menu.tile;
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		int i1 = te.getWaterScaled(54), i2 = te.getLavaScaled(54);
		g.blit(TEX, j+48, k+71-i1, 193, 55-i1, 7, i1);
		g.blit(TEX, j+120, k+71-i2, 202, 55-i2, 7, i2);
		this.drawPowerTab(g, j, k);
	}

	/** Original GuiObsidian.drawPowerTab. */
	private void drawPowerTab(GuiGraphics g, int j, int k) {
		ObsidianMakerBlockEntity grin = menu.tile;
		g.blit(POWERTAB, j+imageWidth, k+4, 0, 4, 42, imageHeight-4);
		long frac = Math.min(29, (grin.power*29L)/grin.MINPOWER);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-144, 0, 0, (int)frac, 4);
		frac = Math.min(29, (grin.omega*29L)/grin.MINSPEED);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-84, 0, 0, (int)frac, 4);
		frac = Math.min(29, (grin.torque*29L)/grin.MINTORQUE);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-24, 0, 0, (int)frac, 4);
		g.drawString(font, "Power:", j+imageWidth+20-font.width("Power:")/2, k+9, 0xff000000, false);
		g.drawString(font, "Speed:", j+imageWidth+20-font.width("Speed:")/2, k+69, 0xff000000, false);
		g.drawString(font, "Torque:", j+imageWidth+20-font.width("Torque:")/2, k+129, 0xff000000, false);
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
		int j = leftPos, k = topPos;
		if (mx >= j+47 && mx <= j+55 && my >= k+16 && my <= k+71)
			g.renderTooltip(font, Component.literal(String.format("Water: %dmB", menu.tile.getWater())), mx, my);
		if (mx >= j+119 && mx <= j+127 && my >= k+16 && my <= k+71)
			g.renderTooltip(font, Component.literal(String.format("Lava: %dmB", menu.tile.getLava())), mx, my);
	}
}
