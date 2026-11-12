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
import reika.rotarycraft.content.blockentity.LineBuilderBlockEntity;
import reika.rotarycraft.content.menu.LineBuilderMenu;

/** Original: GuiBasicStorage (basicstorage.png) for one row of 9 slots, with the power tab (power, speed, torque). */
public class LineBuilderScreen extends AbstractContainerScreen<LineBuilderMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/basicstorage.png");
	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");

	public LineBuilderScreen(LineBuilderMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 114+18;
		inventoryLabelY = imageHeight-94;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, 18+17);
		g.blit(TEX, j, k+18+17, 0, 126, imageWidth, 96);
		LineBuilderBlockEntity te = menu.tile;
		g.blit(POWERTAB, j+imageWidth, k+4, 0, 4, 42, imageHeight-4);
		long frac = Math.min(29, (te.power*29L)/te.MINPOWER);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-144, 0, 0, (int)frac, 4);
		frac = Math.min(29, (te.omega*29L)/te.MINSPEED);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-84, 0, 0, (int)frac, 4);
		frac = Math.min(29, (te.torque*29L)/te.MINTORQUE);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-24, 0, 0, (int)frac, 4);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
