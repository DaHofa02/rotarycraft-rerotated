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
import reika.reactorcraft.content.blockentity.SynthesizerBlockEntity;
import reika.reactorcraft.content.menu.SynthesizerMenu;
import reika.rotarycraft.client.screen.FluidGui;

/** Original: GuiSynthesizer - input and output tanks (16x60, fluid name on hover) and the step timer. */
public class SynthesizerScreen extends AbstractContainerScreen<SynthesizerMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "textures/gui/synthesizer.png");

	public SynthesizerScreen(SynthesizerMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 175;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		SynthesizerBlockEntity tile = menu.tile;
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		g.blit(TEX, j+103, k+26, 176, 92, tile.getTimerScaled(24), 34);
		tank(g, tile.getInputTank(), j+17, k+18);
		tank(g, tile.getOutputTank(), j+134, k+18);
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

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
		int j = leftPos, k = topPos;
		if (mx >= j+16 && mx <= j+33 && my >= k+17 && my <= k+78 && !menu.tile.getInputTank().isEmpty())
			g.renderTooltip(font, menu.tile.getInputTank().getFluid().getHoverName(), mx, my);
		if (mx >= j+133 && mx <= j+150 && my >= k+17 && my <= k+78 && !menu.tile.getOutputTank().isEmpty())
			g.renderTooltip(font, menu.tile.getOutputTank().getFluid().getHoverName(), mx, my);
	}
}
