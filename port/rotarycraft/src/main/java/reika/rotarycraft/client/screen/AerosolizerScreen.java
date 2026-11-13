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
import reika.rotarycraft.content.blockentity.AerosolizerBlockEntity;
import reika.rotarycraft.content.menu.AerosolizerMenu;

/** Original: GuiAerosolizer (aerosolizergui.png) - each store as a bar in its potion colour with its level, and the power tab. */
public class AerosolizerScreen extends AbstractContainerScreen<AerosolizerMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/aerosolizergui.png");
	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");

	public AerosolizerScreen(AerosolizerMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		AerosolizerBlockEntity te = menu.tile;
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		for (int r = 0; r < 3; r++)
			for (int c = 0; c < 3; c++) {
				int lvl = te.getPotionLevel(3*r+c), h = lvl/4;
				if (h > 0)
					g.fill(j+62+18*c, k+17+18*r+16-h, j+78+18*c, k+17+18*r+16, te.getPotionColor(3*r+c));
			}
		this.drawPowerTab(g, j, k);
	}

	/** Original GuiPowerOnlyMachine power tab with three bars (as the other screens). */
	private void drawPowerTab(GuiGraphics g, int j, int k) {
		AerosolizerBlockEntity grin = menu.tile;
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
		super.renderLabels(g, mx, my);
		for (int r = 0; r < 3; r++)
			for (int c = 0; c < 3; c++) {
				String n = String.valueOf(menu.tile.getPotionLevel(3*r+c));
				g.drawString(font, n, 70+18*c-font.width(n)/2, 22+18*r, 0, false);
			}
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
		int j = leftPos, k = topPos;
	}
}
