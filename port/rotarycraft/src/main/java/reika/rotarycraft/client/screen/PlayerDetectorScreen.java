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
import reika.rotarycraft.content.menu.PlayerDetectorMenu;

/**
 * Original: Reika.RotaryCraft.GUIs.Machine.GuiPlayerDetector - a three-digit text field for the detection range; behind it the range
 * in effect in brackets, red when the typed one is more than the power allows.
 */
public class PlayerDetectorScreen extends AbstractContainerScreen<PlayerDetectorMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/rangegui.png");

	private EditBox input;
	private int range;

	public PlayerDetectorScreen(PlayerDetectorMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 46;
		if (menu.tile != null)
			range = menu.tile.selectedrange;
	}

	@Override
	protected void init() {
		super.init();
		input = new EditBox(font, leftPos+8+imageWidth/2-6, topPos+21, 26, 16, Component.empty());
		input.setMaxLength(3);
		input.setFilter(s -> s.matches("[0-9]*"));
		input.setResponder(s -> {
			if (s.isEmpty())
				return;
			range = Integer.parseInt(s);
			if (minecraft != null && minecraft.gameMode != null)
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, range);
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
		g.blit(TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		if (menu.tile == null)
			return;
		int color = range > menu.tile.getMaxRange() ? 0xffff0000 : 0xff404040;
		String s = String.format("(%d)", menu.tile.getRange());
		g.drawString(font, s, leftPos+imageWidth/2+58-font.width(s)/2, topPos+25, color, false);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
		g.drawString(font, "Detection Range:", imageWidth/2-82, 25, 4210752, false);
		if (!input.isFocused() && menu.tile != null)
			g.drawString(font, String.format("%d", menu.tile.selectedrange), imageWidth/2+6, 25, 0xffffffff, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
