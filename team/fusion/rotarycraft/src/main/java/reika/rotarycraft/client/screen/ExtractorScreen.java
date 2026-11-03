/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import reika.rotarycraft.RotaryConfig;
import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.ExtractorBlockEntity;
import reika.rotarycraft.content.menu.ExtractorMenu;

/**
 * Original: GuiExtractor (extractorgui.png, extractorgui2.png with drill wear) - progress of the four stages, drill type, and the power
 * tab with one bar per stage for power, speed and torque against that stage's minimum.
 */
public class ExtractorScreen extends AbstractContainerScreen<ExtractorMenu> {

	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");

	public ExtractorScreen(ExtractorMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
	}

	private static ResourceLocation tex() {
		return ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/"+(RotaryConfig.EXTRACTORMAINTAIN.get() ? "extractorgui2" : "extractorgui")+".png");
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		ExtractorBlockEntity ext = menu.tile;
		int j = leftPos, k = topPos;
		ResourceLocation t = tex();
		g.blit(t, j, k, 0, 0, imageWidth, imageHeight);
		int i2 = Math.min(28, ext.getCookProgressScaled(28, 1));
		int i3 = Math.min(28, ext.getCookProgressScaled(28, 2));
		int i4 = Math.min(32, ext.getCookProgressScaled(32, 3));
		int i1 = Math.min(32, ext.getCookProgressScaled(32, 0));
		if (!RotaryConfig.EXTRACTORMAINTAIN.get())
			g.blit(t, j+29, k+34, 176, 48, 10, i1);
		g.blit(t, j+63, k+35, 186, 48, 14, i2);
		g.blit(t, j+99, k+35, 200, 48, 14, i3);
		g.blit(t, j+133, k+49-i4, 176, 79-i4, 17, i4);
		if (RotaryConfig.EXTRACTORMAINTAIN.get()) {
			int i5 = ext.getDrillLifeScaled(16);
			g.blit(t, j+20, k+50-i5, 178, 159-i5, 2, i5);
			g.blit(t, j+29, k+34, 176, 48, 10, i1);
		}
		//drill type marker (original foreground layer)
		g.blit(t, j+10, k+33, ext.isBedrock() ? 190 : 182, 142, 7, 18);
		this.drawPowerTab(g, j, k);
	}

	/** Original GuiExtractor.drawPowerTab. */
	private void drawPowerTab(GuiGraphics g, int j, int k) {
		ExtractorBlockEntity ext = menu.tile;
		g.blit(POWERTAB, j+imageWidth, k+4, 42, 4, 42, imageHeight-4);
		for (int i = 0; i < 4; i++) {
			int frac = (int)Math.min(31, (ext.power*31L)/ExtractorBlockEntity.MIN_POWER[i]);
			g.blit(POWERTAB, j+imageWidth+7+7*i, k+imageHeight-144+31-frac, 0, 200-frac, 5, frac);
		}
		for (int i = 0; i < 4; i++) {
			int frac = Math.min(31, ext.omega*31/ExtractorBlockEntity.MIN_SPEED[i]);
			g.blit(POWERTAB, j+imageWidth+7+7*i, k+imageHeight-93+31-frac, 0, 200-frac, 5, frac);
		}
		for (int i = 0; i < 4; i++) {
			int frac = Math.min(31, ext.torque*31/ExtractorBlockEntity.MIN_TORQUE[i]);
			g.blit(POWERTAB, j+imageWidth+7+7*i, k+imageHeight-42+31-frac, 0, 200-frac, 5, frac);
		}
		g.drawString(font, "Power:", j+imageWidth+20-font.width("Power:")/2, k+9, 0xff000000, false);
		g.drawString(font, "Speed:", j+imageWidth+20-font.width("Speed:")/2, k+60, 0xff000000, false);
		g.drawString(font, "Torque:", j+imageWidth+20-font.width("Torque:")/2, k+111, 0xff000000, false);
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
		if (mx >= j+10 && mx <= j+17 && my >= k+33 && my <= k+51)
			g.renderTooltip(font, Component.literal(menu.tile.isBedrock() ? "Bedrock Drill" : "Steel Drill"), mx, my);
		if (RotaryConfig.EXTRACTORMAINTAIN.get() && mx >= j+19 && mx <= j+23 && my >= k+33 && my <= k+51)
			g.renderTooltip(font, Component.literal(String.format("Drill Status: %d%%", menu.tile.getDrillLifeScaled(100))), mx, my);
	}
}
