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
import reika.rotarycraft.content.blockentity.FractionatorBlockEntity;
import reika.rotarycraft.content.menu.FractionatorMenu;

/**
 * Original: GuiFractionator (fractiongui.png) - jet fuel, ethanol and pressure gauges with tooltips, the five-part progress bar
 * (DragonAPI SubdividedProgressBar: 18, 37, 38 at 0.4x speed, 8, 9 at 0.1x speed) and the power tab.
 */
public class FractionatorScreen extends AbstractContainerScreen<FractionatorMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/fractiongui.png");
	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");

	/** Section lengths (pixels) and speed factors of the original progress bar. */
	private static final int[] LEN = {18, 37, 38, 8, 9};
	private static final float[] SPEED = {1, 1, 0.4F, 1, 0.1F};

	public FractionatorScreen(FractionatorMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
	}

	/** Original SubdividedProgressBar: each section takes len/speed of the total time, in order. */
	private static int[] barScaled(int tick, int duration) {
		float total = 0;
		for (int i = 0; i < LEN.length; i++)
			total += LEN[i]/SPEED[i];
		float f = duration > 0 ? Math.min(tick, duration-1)/(float)duration : 0;
		int[] ret = new int[LEN.length];
		float at = 0;
		for (int i = 0; i < LEN.length; i++) {
			float start = at/total;
			at += LEN[i]/SPEED[i];
			float end = at/total;
			float frac = start >= f ? 0 : end <= f ? 1 : (f-start)/(end-start);
			ret[i] = (int)(frac*LEN[i]);
		}
		return ret;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		FractionatorBlockEntity fct = menu.tile;
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		int i2 = fct.getFuelScaled(54);
		g.blit(TEX, j+151, k+70-i2, 179, 55-i2, 8, i2);
		int i1 = fct.getEthanolScaled(54);
		g.blit(TEX, j+124, k+70-i1, 189, 55-i1, 8, i1);
		int i0 = fct.getPressureScaled(54);
		g.blit(TEX, j+134, k+70-i0, 224, 55-i0, 2, i0);
		int[] v = barScaled(fct.mixTime, fct.getOperationTime());
		g.blit(TEX, j+34, k+17, 178, 59, v[0], 47);
		g.blit(TEX, j+70, k+17, 197, 59, v[1], 47);
		g.blit(TEX, j+107, k+53-v[2], 212, 39-v[2], 8, v[2]);
		g.blit(TEX, j+115, k+17, 235, 59, v[3], 34);
		g.blit(TEX, j+139, k+18, 200, 1, v[4], 52);
		this.drawPowerTab(g, j, k);
	}

	/** Original GuiFractionator.drawPowerTab. */
	private void drawPowerTab(GuiGraphics g, int j, int k) {
		FractionatorBlockEntity fct = menu.tile;
		g.blit(POWERTAB, j+imageWidth, k+4, 0, 4, 42, imageHeight-4);
		long frac = Math.min(29, (fct.power*29L)/fct.MINPOWER);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-144, 0, 0, (int)frac, 4);
		frac = Math.min(29, (fct.omega*29L)/fct.MINSPEED);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-84, 0, 0, (int)frac, 4);
		frac = Math.min(29, (fct.torque*29L)/fct.MINTORQUE);
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
		FractionatorBlockEntity fct = menu.tile;
		if (mx >= j+150 && mx <= j+159 && my >= k+15 && my <= k+70)
			g.renderTooltip(font, Component.literal(String.format("%d/%d", fct.getFuelLevel(), FractionatorBlockEntity.CAPACITY)), mx, my);
		if (mx >= j+123 && mx <= j+132 && my >= k+15 && my <= k+70)
			g.renderTooltip(font, Component.literal(String.format("%d/%d", fct.getInputLevel(), FractionatorBlockEntity.INPUT_CAPACITY)), mx, my);
		if (mx >= j+133 && mx <= j+136 && my >= k+15 && my <= k+70)
			g.renderTooltip(font, Component.literal(String.format("%dkPa", fct.getPressure())), mx, my);
	}
}
