/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.blockentity.IsoCentrifugeBlockEntity;
import reika.reactorcraft.content.menu.IsoCentrifugeMenu;
import reika.rotarycraft.client.screen.FluidGui;

/** Original: GuiCentrifuge - the UF6 tank with its gauge overlay, the progress bar, no inventory label. */
public class IsoCentrifugeScreen extends AbstractContainerScreen<IsoCentrifugeMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "textures/gui/centrifuge.png");

	public IsoCentrifugeScreen(IsoCentrifugeMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		IsoCentrifugeBlockEntity tile = menu.tile;
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		if (!tile.getTank().isEmpty()) {
			int h = 60*tile.getTank().getFluidAmount()/tile.getTank().getCapacity();
			FluidGui.drawFluid(g, tile.getTank().getFluid().getFluid(), j+80, k+78-h, 16, h);
		}
		g.blit(TEX, j+80, k+18, 223, 83, 16, 60);
		g.blit(TEX, j+104, k+18, 216, 84, 4, tile.getProcessingScaled(48));
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
