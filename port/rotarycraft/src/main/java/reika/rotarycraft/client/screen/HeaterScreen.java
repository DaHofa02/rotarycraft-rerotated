/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.HeaterBlockEntity;
import reika.rotarycraft.content.menu.HeaterMenu;

/**
 * Original: GuiHeater (heatergui.png) - the 18 fuel slots, the four-digit "Temperature Control" field whose value goes to the server
 * as it is typed, and the three-bar power tab.
 */
public class HeaterScreen extends AbstractContainerScreen<HeaterMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/heatergui.png");
	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");

	private EditBox input;

	public HeaterScreen(HeaterMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 167;
	}

	@Override
	protected void init() {
		super.init();
		input = new EditBox(font, leftPos+8+imageWidth/2+40, topPos-12+67, 32, 16, Component.empty());
		input.setMaxLength(4);
		input.setFilter(s -> s.matches("[0-9]*"));
		input.setResponder(s -> {
			if (s.isEmpty())
				return;
			if (minecraft != null && minecraft.gameMode != null)
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, Integer.parseInt(s));
		});
		this.addRenderableWidget(input);
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (input.isFocused() && key != 256) { //not Escape: typing must not close the GUI with the inventory key
			input.keyPressed(key, scan, mods);
			return true;
		}
		return super.keyPressed(key, scan, mods);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		HeaterBlockEntity te = menu.tile;
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		g.blit(POWERTAB, j+imageWidth, k+4, 0, 4, 42, imageHeight-20);
		g.blit(POWERTAB, j+imageWidth, k+4+imageHeight-20, 0, 157, 42, 6);
		g.blit(POWERTAB, j+imageWidth+5, k+imageHeight-145, 0, 0, (int)Math.min(29, te.power*29L/te.MINPOWER), 4);
		g.blit(POWERTAB, j+imageWidth+5, k+imageHeight-85, 0, 0, (int)Math.min(29, te.omega*29L/te.MINSPEED), 4);
		g.blit(POWERTAB, j+imageWidth+5, k+imageHeight-25, 0, 0, (int)Math.min(29, te.torque*29L/te.MINTORQUE), 4);
		g.drawString(font, "Power:", j+imageWidth+20-font.width("Power:")/2, k+9, 0xff000000, false);
		g.drawString(font, "Speed:", j+imageWidth+20-font.width("Speed:")/2, k+69, 0xff000000, false);
		g.drawString(font, "Torque:", j+imageWidth+20-font.width("Torque:")/2, k+129, 0xff000000, false);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
		g.drawString(font, playerInventoryTitle, 8, imageHeight-96+3, 4210752, false);
		g.drawString(font, "Temperature Control:", 26, 59, 4210752, false);
		if (!input.isFocused())
			g.drawString(font, String.format("%d", menu.tile.getSetTemperature()), 140, 59, 0xffffffff, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
