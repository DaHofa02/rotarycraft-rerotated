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
import reika.rotarycraft.content.blockentity.GrinderBlockEntity;
import reika.rotarycraft.content.menu.GrinderMenu;

/** Original: GuiGrinder (grindergui.png) - progress arrow, lubricant gauge with tooltip, and the power tab (power, speed, torque). */
public class GrinderScreen extends AbstractContainerScreen<GrinderMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/grindergui.png");
	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");

	public GrinderScreen(GrinderMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		GrinderBlockEntity grin = menu.tile;
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		int i1 = Math.min(48, grin.getCookProgressScaled(48));
		g.blit(TEX, j+99, k+34, 176, 14, i1+1, 16);
		int i2 = grin.getLubricantScaled(55);
		g.blit(TEX, j+24, imageHeight/2+k-7-i2, 176, 126-i2, 8, i2);
		this.drawPowerTab(g, j, k);
	}

	/** Original GuiGrinder.drawPowerTab. */
	private void drawPowerTab(GuiGraphics g, int j, int k) {
		GrinderBlockEntity grin = menu.tile;
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
		g.drawString(font, "Lubricant", 5, 11, 4210752, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
		int j = leftPos, k = topPos;
		if (mx >= j+23 && mx <= j+32 && my >= k+20 && my <= k+76)
			g.renderTooltip(font, Component.literal(String.format("%d/%d", menu.tile.getLubricant(), GrinderBlockEntity.MAXLUBE)), mx, my);
	}
}
