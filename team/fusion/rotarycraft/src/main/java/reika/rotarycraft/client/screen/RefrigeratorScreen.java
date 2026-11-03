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
import reika.rotarycraft.content.blockentity.RefrigeratorBlockEntity;
import reika.rotarycraft.content.menu.RefrigeratorMenu;

/**
 * Original: GuiFridge (fridgegui.png, 176x188) - the frost progress overlay (145x67), the LN2 column (16x72) with level/capacity and
 * the fluid name on hover, and its own taller power tab.
 */
public class RefrigeratorScreen extends AbstractContainerScreen<RefrigeratorMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/fridgegui.png");
	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");

	public RefrigeratorScreen(RefrigeratorMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 188;
		inventoryLabelY = imageHeight-94;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		RefrigeratorBlockEntity te = menu.tile;
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		g.blit(TEX, j+7, k+17, 0, 189, Math.min(145, te.getProgressScaled(145)), 67);
		if (!te.getTank().isEmpty()) {
			int i2 = te.getLiquidScaled(72);
			FluidGui.drawFluid(g, te.getTank().getFluid().getFluid(), j+152, k+89-i2+1, 16, i2);
		}
		g.blit(POWERTAB, j+imageWidth, k+4, 0, 4, 42, imageHeight-24);
		long frac = Math.min(29, (te.power*29L)/te.MINPOWER);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-166, 0, 0, (int)frac, 4);
		frac = Math.min(29, (te.omega*29L)/te.MINSPEED);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-106, 0, 0, (int)frac, 4);
		frac = Math.min(29, (te.torque*29L)/te.MINTORQUE);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-46, 0, 0, (int)frac, 4);
		g.drawString(font, "Power:", j+imageWidth+20-font.width("Power:")/2, k+9, 0xff000000, false);
		g.drawString(font, "Speed:", j+imageWidth+20-font.width("Speed:")/2, k+69, 0xff000000, false);
		g.drawString(font, "Torque:", j+imageWidth+20-font.width("Torque:")/2, k+129, 0xff000000, false);
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
		if (mx >= j+152 && mx <= j+167 && my >= k+18 && my <= k+89) {
			RefrigeratorBlockEntity te = menu.tile;
			java.util.List<Component> li = new java.util.ArrayList<>();
			li.add(Component.literal(String.format("%d/%d", te.getTank().getFluidAmount(), RefrigeratorBlockEntity.CAPACITY)));
			if (!te.getTank().isEmpty())
				li.add(te.getTank().getFluid().getHoverName());
			g.renderComponentTooltip(font, li, mx, my);
		}
	}
}
