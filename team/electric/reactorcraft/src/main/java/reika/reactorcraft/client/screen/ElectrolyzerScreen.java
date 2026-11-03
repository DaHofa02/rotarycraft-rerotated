/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.blockentity.ElectrolyzerBlockEntity;
import reika.reactorcraft.content.menu.ElectrolyzerMenu;
import reika.rotarycraft.client.screen.FluidGui;

/**
 * Original: GuiElectrolyzer - input tank, heavy (bottom) and light (top) output tanks (16x60, liquid name on hover), and the step timer
 * (66 wide; the arrow with the item when there is one).
 */
public class ElectrolyzerScreen extends AbstractContainerScreen<ElectrolyzerMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "textures/gui/electrolyzer.png");

	public ElectrolyzerScreen(ElectrolyzerMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 175;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		ElectrolyzerBlockEntity tile = menu.tile;
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		int dy = tile.getInventory().getStackInSlot(0).isEmpty() ? 124 : 61;
		g.blit(TEX, j+65, k+17, 177, dy, tile.getTimerScaled(66), 62);
		tank(g, tile.getHeavyTank(), j+98, k+18);
		tank(g, tile.getLightTank(), j+134, k+18);
		tank(g, tile.getInputTank(), j+17, k+18);
	}

	private static void tank(GuiGraphics g, FluidTank t, int x, int y) {
		if (t.isEmpty())
			return;
		int h = 60*t.getFluidAmount()/t.getCapacity();
		FluidGui.drawFluid(g, t.getFluid().getFluid(), x, y+60-h, 16, h);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
		g.drawString(font, playerInventoryTitle, imageWidth-58, imageHeight-96+3, 4210752, false); //original ReactorGuiBase
	}

	private void hover(GuiGraphics g, FluidTank t, int x0, int y0, int mx, int my) {
		if (mx >= x0 && mx <= x0+17 && my >= y0 && my <= y0+61)
			g.renderTooltip(font, t.isEmpty() ? Component.literal("Empty") : t.getFluid().getHoverName(), mx, my);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
		int j = leftPos, k = topPos;
		this.hover(g, menu.tile.getHeavyTank(), j+97, k+17, mx, my);
		this.hover(g, menu.tile.getLightTank(), j+133, k+17, mx, my);
		this.hover(g, menu.tile.getInputTank(), j+16, k+17, mx, my);
	}
}
