/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.menu.ScaleableChestMenu;

/**
 * Original: GuiScaleChest (basicstorage.png with six rows) - "Page n", the closed slots of the last page greyed out, Next and Back
 * buttons and the power-only tab.
 */
public class ScaleableChestScreen extends AbstractContainerScreen<ScaleableChestMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/basicstorage.png");
	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");

	public ScaleableChestScreen(ScaleableChestMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 114+18*6;
		inventoryLabelY = imageHeight-94;
	}

	@Override
	protected void init() {
		super.init();
		int j = leftPos+8, k = topPos-12;
		this.addRenderableWidget(Button.builder(Component.literal("Next"), b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0)).bounds(j+imageWidth-9, k+45, 40, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1)).bounds(j+imageWidth-9, k+65, 40, 20).build());
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, 6*18+17);
		g.blit(TEX, j, k+6*18+17, 0, 126, imageWidth, 96);
		int open = Math.max(0, menu.tile.getSlotsOnPage(menu.page));
		for (int i = open; i < 54; i++)
			g.fill(j+7+18*(i%9), k+17+18*(i/9), j+25+18*(i%9), k+35+18*(i/9), 0xFFC6C6C6);
		g.blit(POWERTAB, j+imageWidth, k+4, 0, 4, 42, 24);
		g.blit(POWERTAB, j+imageWidth, k+4+23, 0, 157, 42, 6);
		g.blit(POWERTAB, j+imageWidth+5, k+22, 0, 0, (int)menu.tile.getScaledPower(29), 4);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		super.renderLabels(g, mx, my);
		g.drawString(font, "Page "+menu.page, imageWidth-48, 6, 4210752, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
