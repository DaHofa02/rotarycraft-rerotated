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
import reika.rotarycraft.content.blockentity.CrystallizerBlockEntity;
import reika.rotarycraft.content.menu.CrystallizerMenu;

/**
 * Original: GuiCrystallizer (crystalgui.png) - the freezing progress bar (44 wide), the liquid column (72 high) with its freezing point
 * and a tooltip, the temperature, and the power tab (power, speed, torque).
 */
public class CrystallizerScreen extends AbstractContainerScreen<CrystallizerMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/crystalgui.png");
	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");

	public CrystallizerScreen(CrystallizerMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		CrystallizerBlockEntity te = menu.tile;
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		int i1 = Math.min(44, te.getProgressScaled(44));
		g.blit(TEX, j+29, k+41, 178, 1, i1, 4);
		if (!te.getTank().isEmpty()) {
			int i2 = te.getLiquidScaled(72);
			FluidGui.drawFluid(g, te.getTank().getFluid().getFluid(), j+8, k+78-i2+1, 16, i2);
		}
		this.drawPowerTab(g, j, k);
	}

	/** Original GuiCrystallizer.drawPowerTab. */
	private void drawPowerTab(GuiGraphics g, int j, int k) {
		CrystallizerBlockEntity te = menu.tile;
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
		CrystallizerBlockEntity te = menu.tile;
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
		g.drawString(font, playerInventoryTitle, imageWidth-58, imageHeight-96+3, 4210752, false);
		if (!te.getTank().isEmpty()) {
			String s = String.format("%d C", te.getFreezingPoint());
			g.drawString(font, s, imageWidth/2-font.width(s)/2, 56, 0, false);
		}
		String s = String.format("%d C", te.getTemperature());
		g.drawString(font, s, 50-font.width(s)/2, 30, 0, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
		int j = leftPos, k = topPos;
		if (mx >= j+7 && mx <= j+24 && my >= k+6 && my <= k+79)
			g.renderTooltip(font, Component.literal(String.format("%d/%d", menu.tile.getTank().getFluidAmount(), CrystallizerBlockEntity.CAPACITY)), mx, my);
	}
}
