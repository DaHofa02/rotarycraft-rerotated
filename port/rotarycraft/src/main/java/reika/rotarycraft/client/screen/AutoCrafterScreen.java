/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import reika.rotarycraft.content.blockentity.AutoCrafterBlockEntity;
import reika.rotarycraft.content.menu.AutoCrafterMenu;

/**
 * Own layout (drawn): patterns, outputs, leftovers. The mode button sits top right; in request mode a right click on a pattern asks for one
 * cycle, in sustain mode mouse wheel over a pattern changes its threshold (shown in its corner).
 */
public class AutoCrafterScreen extends AbstractContainerScreen<AutoCrafterMenu> {

	private Button modeButton;

	public AutoCrafterScreen(AutoCrafterMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 232;
		inventoryLabelY = AutoCrafterMenu.INV_Y-11;
	}

	private void click(int id) {
		if (minecraft != null && minecraft.gameMode != null)
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
	}

	@Override
	protected void init() {
		super.init();
		modeButton = this.addRenderableWidget(Button.builder(Component.empty(), b -> this.click(0)).bounds(leftPos+104, topPos+2, 66, 14).build());
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		modeButton.setMessage(Component.literal(menu.tile.getMode().name().toLowerCase()));
	}

	private int patternAt(double mx, double my) {
		for (int i = 0; i < AutoCrafterBlockEntity.SIZE; i++) {
			Slot s = menu.slots.get(i);
			if (mx >= leftPos+s.x && mx < leftPos+s.x+16 && my >= topPos+s.y && my < topPos+s.y+16)
				return i;
		}
		return -1;
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		int i = this.patternAt(mx, my);
		if (button == 1 && i >= 0 && menu.tile.getMode() == AutoCrafterBlockEntity.Mode.REQUEST && !menu.slots.get(i).getItem().isEmpty()) {
			this.click(100+i);
			return true;
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double dx, double dy) {
		int i = this.patternAt(mx, my);
		if (i >= 0 && menu.tile.getMode() == AutoCrafterBlockEntity.Mode.SUSTAIN) {
			int n = Math.max(0, Math.min(64, menu.tile.getThreshold(i)+(dy > 0 ? 1 : -1)));
			this.click(1000+100*i+n);
			return true;
		}
		return super.mouseScrolled(mx, my, dx, dy);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int x = leftPos, y = topPos;
		g.fill(x, y, x+imageWidth, y+imageHeight, 0xff2b2d33);
		g.fill(x+2, y+2, x+imageWidth-2, y+imageHeight-2, 0xffa4aab4);
		for (Slot s : menu.slots)
			g.fill(x+s.x-1, y+s.y-1, x+s.x+17, y+s.y+17, s.index < 18 ? 0xff5a6a9a : s.index < 36 ? 0xff5a8a5a : s.index < 54 ? 0xff8a7a5a : 0xff6e727a);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, 8, 5, 0xff202020, false);
		g.drawString(font, "Outputs", 8, 53, 0xff202020, false);
		g.drawString(font, "Leftovers", 8, 97, 0xff202020, false);
		g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xff404040, false);
		if (menu.tile.getMode() == AutoCrafterBlockEntity.Mode.SUSTAIN)
			for (int i = 0; i < AutoCrafterBlockEntity.SIZE; i++) {
				Slot s = menu.slots.get(i);
				g.drawString(font, String.valueOf(menu.tile.getThreshold(i)), s.x, s.y, 0xffffff60, true);
			}
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
