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
import reika.rotarycraft.content.blockentity.FillingStationBlockEntity;
import reika.rotarycraft.content.menu.FillingStationMenu;

/** Original: GuiFillingStation (fillingstationgui.png, 176x187) - the tank column (12x66) with its level on hover, the power tab. */
public class FillingStationScreen extends AbstractContainerScreen<FillingStationMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/fillingstationgui.png");
	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");

	public FillingStationScreen(FillingStationMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 187;
		inventoryLabelY = imageHeight-94;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		FillingStationBlockEntity te = menu.tile;
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		if (te.getFluid() != null) {
			int i2 = te.getLiquidScaled(66);
			FluidGui.drawFluid(g, te.getFluid(), j+82, k+87-i2, 12, i2);
		}
		g.blit(POWERTAB, j+imageWidth, k+4, 0, 4, 42, 24); //original GuiPowerOnlyMachine: the short power tab
		g.blit(POWERTAB, j+imageWidth, k+4+23, 0, 157, 42, 6);
		long frac = Math.min(29, (te.power*29L)/te.MINPOWER);
		g.blit(POWERTAB, j+imageWidth+5, k+22, 0, 0, (int)frac, 4);
		g.drawString(font, "Power:", j+imageWidth+20-font.width("Power:")/2, k+9, 0xff000000, false);
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
		int j = leftPos, k = topPos;
		if (mx >= j+81 && mx <= j+94 && my >= k+20 && my <= k+87)
			g.renderTooltip(font, Component.literal(String.format("%d/%d mB", menu.tile.getTank().getFluidAmount(), FillingStationBlockEntity.CAPACITY)), mx, my);
	}
}
