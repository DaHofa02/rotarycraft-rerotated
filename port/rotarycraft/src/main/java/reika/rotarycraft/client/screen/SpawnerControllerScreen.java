/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.menu.SpawnerControllerMenu;

/**
 * Original: GuiSpawnerController (spawnercontrollergui.png, 176x75) - a Disable/Enable button and the spawn delay field; without a spawner
 * below, the lower half of the texture and a red warning.
 */
public class SpawnerControllerScreen extends AbstractContainerScreen<SpawnerControllerMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/spawnercontrollergui.png");

	private EditBox input;

	public SpawnerControllerScreen(SpawnerControllerMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 75;
	}

	@Override
	protected void init() {
		super.init();
		this.addRenderableWidget(Button.builder(Component.literal("Disable/Enable"), b -> this.toggle()).bounds(leftPos+imageWidth/2-48, topPos+20, 80, 20).build());
		input = new EditBox(font, leftPos+imageWidth/2-7, topPos+47, 26, 16, Component.empty());
		input.setMaxLength(5);
		input.setFilter(s -> s.matches("[0-9]*"));
		input.setResponder(s -> {
			if (!s.isEmpty() && !menu.tile.disable)
				this.send(Math.max(Integer.parseInt(s), menu.tile.getOperationTime()));
		});
		this.addRenderableWidget(input);
	}

	private void toggle() {
		if (menu.tile.disable)
			this.send(menu.tile.getDelay());
		else if (minecraft != null && minecraft.gameMode != null)
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
	}

	private void send(int delay) {
		if (minecraft != null && minecraft.gameMode != null)
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1+Math.min(delay, SpawnerControllerMenu.MAX_DELAY));
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (input.isFocused() && key != 256) //not Escape: typing must not close the GUI with the inventory key
			return input.keyPressed(key, scan, mods) || input.canConsumeInput() || super.keyPressed(key, scan, mods);
		return super.keyPressed(key, scan, mods);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		boolean valid = menu.tile.getLevel() != null && menu.tile.isValidLocation(menu.tile.getLevel());
		g.blit(TEX, leftPos, topPos, 0, valid ? 0 : imageHeight, imageWidth, imageHeight);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, (imageWidth-font.width(title))/2, 6, 0x404040, false);
		boolean valid = menu.tile.getLevel() != null && menu.tile.isValidLocation(menu.tile.getLevel());
		if (!valid) {
			g.drawString(font, "No Spawner", (imageWidth-font.width("No Spawner"))/2, 51, 0xff0000, false);
			return;
		}
		int color = menu.tile.disable ? 0xcccccc : 0x404040;
		g.drawString(font, "Spawn Delay:", imageWidth/2-64, 51, color, false);
		if (!input.isFocused())
			g.drawString(font, String.valueOf(menu.tile.getDelay()), imageWidth/2+24, 51, 0xffffff, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
