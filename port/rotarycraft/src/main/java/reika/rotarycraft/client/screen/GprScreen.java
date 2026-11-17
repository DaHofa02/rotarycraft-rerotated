/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.GprBlockEntity;
import reika.rotarycraft.content.menu.GprMenu;

/**
 * Original: Reika.RotaryCraft.GUIs.Machine.GuiGPR (176x215, gprgui.png) - the scanned slice as 2x2 pixel cells (96 deep, up to 81 wide)
 * and, at half size, the scan point and slice direction. '[' and ']' move the scan point, '\' resets it.
 */
public class GprScreen extends AbstractContainerScreen<GprMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/gprgui.png");

	public GprScreen(GprMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 215;
		inventoryLabelY = imageHeight+100;
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		int id = key == GLFW.GLFW_KEY_LEFT_BRACKET ? 0 : key == GLFW.GLFW_KEY_RIGHT_BRACKET ? 1 : key == GLFW.GLFW_KEY_BACKSLASH ? 2 : -1;
		if (id >= 0 && minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
			return true;
		}
		return super.keyPressed(key, scan, mods);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		g.blit(TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		GprBlockEntity te = menu.tile;
		if (te == null)
			return;
		int a = leftPos, b = topPos+1;
		int r = te.getRange();
		for (int x = -r; x <= r; x++)
			for (int dd = 1; dd <= GprBlockEntity.MAX_HEIGHT; dd++) {
				int x0 = a+7+2*(x+GprBlockEntity.MAX_WIDTH/2), y0 = b+16+2*dd-2;
				g.fill(x0, y0, x0+2, y0+2, 0xFF000000 | te.getColor(x, dd));
			}
		String s = te.getLookDescription();
		g.pose().pushPose();
		g.pose().scale(0.5F, 0.5F, 1);
		g.drawString(font, s, (a+font.width(s)/2)*2, (b+16)*2, 0xFFFFFF);
		g.pose().popPose();
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
	}
}
