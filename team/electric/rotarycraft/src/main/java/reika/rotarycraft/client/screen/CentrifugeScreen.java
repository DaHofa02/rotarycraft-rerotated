/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.CentrifugeBlockEntity;
import reika.rotarycraft.content.menu.CentrifugeMenu;

/** Original: GuiCentrifuge (centrifugegui.png) - spin progress, the fluid tank with tooltip, the power tab; inventory label on the left. */
public class CentrifugeScreen extends AbstractContainerScreen<CentrifugeMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/centrifugegui.png");
	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");

	public CentrifugeScreen(CentrifugeMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		CentrifugeBlockEntity cent = menu.tile;
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		int i3 = Math.min(37, cent.getProgressScaled(37));
		g.blit(TEX, j+45, k+27, 178, 1, i3, 37);
		if (cent.getFluidLevel() > 0) {
			int i2 = cent.getLiquidScaled(70);
			FluidGui.drawFluid(g, cent.getFluid().getFluid(), j+152, k+77-i2+1, 16, i2);
		}
		this.drawPowerTab(g, j, k);
	}

	/** Original GuiCentrifuge.drawPowerTab. */
	private void drawPowerTab(GuiGraphics g, int j, int k) {
		CentrifugeBlockEntity cent = menu.tile;
		g.blit(POWERTAB, j+imageWidth, k+4, 0, 4, 42, imageHeight-4);
		long frac = Math.min(29, (cent.power*29L)/cent.MINPOWER);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-144, 0, 0, (int)frac, 4);
		frac = Math.min(29, (cent.omega*29L)/cent.MINSPEED);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-84, 0, 0, (int)frac, 4);
		frac = Math.min(29, (cent.torque*29L)/cent.MINTORQUE);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-24, 0, 0, (int)frac, 4);
		g.drawString(font, "Power:", j+imageWidth+20-font.width("Power:")/2, k+9, 0xff000000, false);
		g.drawString(font, "Speed:", j+imageWidth+20-font.width("Speed:")/2, k+69, 0xff000000, false);
		g.drawString(font, "Torque:", j+imageWidth+20-font.width("Torque:")/2, k+129, 0xff000000, false);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
		g.drawString(font, playerInventoryTitle, 8, imageHeight-96+3, 4210752, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
		int j = leftPos, k = topPos;
		if (mx >= j+151 && mx <= j+168 && my >= k+7 && my <= k+78) {
			CentrifugeBlockEntity cent = menu.tile;
			FluidStack fs = cent.getFluid();
			String s = fs.isEmpty() ? String.format("0/%d mB", CentrifugeBlockEntity.CAPACITY)
					: String.format("%s: %d/%d", fs.getHoverName().getString(), cent.getFluidLevel(), CentrifugeBlockEntity.CAPACITY);
			g.renderTooltip(font, Component.literal(s), mx, my);
		}
	}
}
