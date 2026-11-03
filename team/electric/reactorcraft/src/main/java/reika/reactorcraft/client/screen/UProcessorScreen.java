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
import reika.reactorcraft.content.blockentity.UProcessorBlockEntity;
import reika.reactorcraft.content.menu.UProcessorMenu;
import reika.rotarycraft.client.screen.FluidGui;

/** Original: GuiProcessor (ReactorGuiBase) - the two step timers and the three tanks (16x60) with the fluid name on hover. */
public class UProcessorScreen extends AbstractContainerScreen<UProcessorMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "textures/gui/processor.png");

	public UProcessorScreen(UProcessorMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 175;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		UProcessorBlockEntity tile = menu.tile;
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		g.blit(TEX, j+67, k+21, 176, 92, tile.getIntermediateTimerScaled(24), 17);
		g.blit(TEX, j+67, k+58, 176, 92, tile.getOutputTimerScaled(24), 17);
		tank(g, tile.getInputTank(), j+98, k+18);
		tank(g, tile.getIntermediateTank(), j+116, k+18);
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
		FluidTank[] tanks = {menu.tile.getInputTank(), menu.tile.getIntermediateTank(), menu.tile.getOutputTank()};
		for (int i = 0; i < 3; i++) {
			int x = j+97+18*i;
			if (mx >= x && mx <= x+17 && my >= k+17 && my <= k+78)
				g.renderTooltip(font, tanks[i].isEmpty() ? Component.literal("Empty") : tanks[i].getFluid().getHoverName(), mx, my);
		}
	}
}
