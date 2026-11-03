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
import reika.rotarycraft.content.blockentity.DryingBedBlockEntity;
import reika.rotarycraft.content.menu.DryingBedMenu;

/** Original: GuiDryer (drygui.png) - the liquid column (16x72), level/capacity on hover, and the drying progress bar (91 wide). */
public class DryingBedScreen extends AbstractContainerScreen<DryingBedMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/drygui.png");

	public DryingBedScreen(DryingBedMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		DryingBedBlockEntity te = menu.tile;
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		g.blit(TEX, j+29, k+41, 1, 169, te.getProgressScaled(91), 4);
		if (!te.getTank().isEmpty()) {
			int i2 = te.getLiquidScaled(72);
			FluidGui.drawFluid(g, te.getTank().getFluid().getFluid(), j+8, k+78-i2+1, 16, i2);
		}
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
		if (mx >= j+7 && mx <= j+24 && my >= k+6 && my <= k+79)
			g.renderTooltip(font, Component.literal(String.format("%d/%d", menu.tile.getTank().getFluidAmount(), DryingBedBlockEntity.CAPACITY)), mx, my);
	}
}
