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
import reika.rotarycraft.content.blockentity.CompactorBlockEntity;
import reika.rotarycraft.content.menu.CompactorMenu;

/** Original: GuiCompactor (compactorgui2.png) - the press progress, the temperature and pressure bars with their values on hover, the power tab. */
public class CompactorScreen extends AbstractContainerScreen<CompactorMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/compactorgui2.png");
	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");

	public CompactorScreen(CompactorMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		CompactorBlockEntity te = menu.tile;
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		int i1 = te.getCookProgressScaled(30);
		int i2 = te.getTemperatureScaled(54);
		int i3 = te.getPressureScaled(54);
		if (i1 != 0)
			i1++;
		if (i2 < 9)
			i2 = 9;
		g.blit(TEX, j+46, k+14, 193, 32, i1, 58);
		g.blit(TEX, j+147, k+70-i3, 176, 82-i3, 4, i3);
		g.blit(TEX, j+118, k+70-i2, 182, 86-i2, 9, i2);
		g.blit(POWERTAB, j+imageWidth, k+4, 0, 4, 42, imageHeight-4);
		long frac = Math.min(29, (te.power*29L)/te.MINPOWER);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-144, 0, 0, (int)frac, 4);
		frac = Math.min(29, (te.omega*29L)/te.MINSPEED);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-84, 0, 0, (int)frac, 4);
		frac = Math.min(29, (te.torque*29L)/te.MINTORQUE);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-24, 0, 0, (int)frac, 4);
		g.drawString(font, "Power:", j+imageWidth+20-font.width("Power:")/2, k+9, 0xff000000, false);
		g.drawString(font, "Speed:", j+imageWidth+20-font.width("Speed:")/2, k+69, 0xff000000, false);
		g.drawString(font, "Torque:", j+imageWidth+20-font.width("Torque:")/2, k+129, 0xff000000, false);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
		g.drawString(font, playerInventoryTitle, 8, imageHeight-96+2, 4210752, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
		int j = leftPos, k = topPos;
		if (mx >= j+146 && mx <= j+151 && my >= k+15 && my <= k+70)
			g.renderTooltip(font, Component.literal(String.format("%d kPa", menu.tile.getPressure())), mx, my);
		if (mx >= j+117 && mx <= j+127 && my >= k+15 && my <= k+70)
			g.renderTooltip(font, Component.literal(String.format("%dC", menu.tile.temperature)), mx, my);
	}
}
