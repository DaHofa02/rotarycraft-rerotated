/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BigFurnaceBlockEntity;
import reika.rotarycraft.content.menu.BigFurnaceMenu;

/** Original: GuiBigFurnace (bigfurngui.png, 190x207) - the progress bar between inputs and outputs, the temperature and the lava column. */
public class BigFurnaceScreen extends AbstractContainerScreen<BigFurnaceMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/bigfurngui.png");

	public BigFurnaceScreen(BigFurnaceMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 190;
		imageHeight = 207;
		inventoryLabelY = imageHeight-94;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		BigFurnaceBlockEntity te = menu.tile;
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		g.blit(TEX, j+7, k+55, 0, 208, 162, te.getCookScaled(17));
		if (!te.getTank().isEmpty()) {
			int i2 = te.getLavaScaled(91);
			FluidGui.drawFluid(g, Fluids.LAVA, j+173, k+108-i2+1, 10, i2);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		BigFurnaceBlockEntity te = menu.tile;
		g.drawString(font, title, 8, 6, 4210752, false);
		g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 4210752, false);
		int c = te.getTemperature() >= 1000 ? 4 : te.getTemperature() >= 100 ? 2 : 0;
		String s = te.getTemperature()+"C";
		g.drawString(font, s, imageWidth-13-c-font.width(s)/2, 6, 4210752, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
		int j = leftPos, k = topPos;
		if (mx >= j+172 && mx <= j+183 && my >= k+17 && my <= k+109)
			g.renderTooltip(font, Component.literal(String.format("%d/%d", menu.tile.getTank().getFluidAmount(), BigFurnaceBlockEntity.CAPACITY)), mx, my);
	}
}
