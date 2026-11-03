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
import reika.rotarycraft.content.blockentity.PulseJetFurnaceBlockEntity;
import reika.rotarycraft.content.menu.PulseJetFurnaceMenu;

/** Original: GuiPulseFurnace (pulsejetgui.png) - temperature, water, fuel and (empty, not ported) oxygen gauges, flame and smelt progress, power tab. */
public class PulseJetFurnaceScreen extends AbstractContainerScreen<PulseJetFurnaceMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/pulsejetgui.png");
	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");

	public PulseJetFurnaceScreen(PulseJetFurnaceMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		PulseJetFurnaceBlockEntity puls = menu.tile;
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		int i1 = puls.getCookProgressScaled(10);
		int i2 = puls.getFuelScaled(52);
		int i3 = puls.getWaterScaled(52);
		int i4 = Math.max(9, puls.getTempScaled(54));
		int i5 = puls.getFireScaled(38);
		g.blit(TEX, j + 131, k + 36, 215, 55, 4, i1);
		g.blit(TEX, j + 91, k + 68-i2, 248, 53-i2, 5, i2);
		g.blit(TEX, j + 59, k + 68-i3, 199, 53-i3, 5, i3);
		g.blit(TEX, j + 20, k + 70-i4, 176, 55-i4, 11, i4);
		g.blit(TEX, j + 115, k + 61-i5, 177, 95-i5, 9, i5);
		g.blit(TEX, j + 142, k + 61-i5, 204, 95-i5, 9, i5);
		this.drawPowerTab(g, j, k);
	}

	/** Original GuiPulseFurnace.drawPowerTab. */
	private void drawPowerTab(GuiGraphics g, int j, int k) {
		PulseJetFurnaceBlockEntity puls = menu.tile;
		g.blit(POWERTAB, j+imageWidth, k+4, 0, 4, 42, imageHeight-4);
		long frac = Math.min(29, (puls.power*29L)/puls.MINPOWER);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-144, 0, 0, (int)frac, 4);
		frac = Math.min(29, (puls.omega*29L)/puls.MINSPEED);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-84, 0, 0, (int)frac, 4);
		frac = Math.min(29, (puls.torque*29L)/puls.MINTORQUE);
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
		PulseJetFurnaceBlockEntity puls = menu.tile;
		if (mx >= j+90 && mx <= j+96 && my >= k+15 && my <= k+68)
			g.renderTooltip(font, Component.literal(String.format("%d/%d", puls.getFuel(), PulseJetFurnaceBlockEntity.MAXFUEL)), mx, my);
		if (mx >= j+20 && mx <= j+30 && my >= k+15 && my <= k+70)
			g.renderTooltip(font, Component.literal(String.format("%dC", puls.temperature)), mx, my);
	}
}
