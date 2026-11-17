/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import reika.rotarycraft.content.blockentity.ItemFilterBlockEntity;
import reika.rotarycraft.content.menu.ItemFilterMenu;

/** Own layout (drawn, no texture): template and buffer at the top, blacklist below, the match settings on the right. */
public class ItemFilterScreen extends AbstractContainerScreen<ItemFilterMenu> {

	private static final int SHOWN_TAGS = 4;
	private final List<Button> settingButtons = new ArrayList<>();
	private int shownTagCount = -1;

	public ItemFilterScreen(ItemFilterMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 256;
		imageHeight = 184;
		inventoryLabelY = ItemFilterMenu.INV_Y-11;
	}

	private void click(int id) {
		if (minecraft != null && minecraft.gameMode != null)
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
	}

	@Override
	protected void init() {
		super.init();
		this.rebuild();
	}

	private void rebuild() {
		settingButtons.forEach(this::removeWidget);
		settingButtons.clear();
		ItemFilterBlockEntity te = menu.tile;
		int x = leftPos+156, y = topPos+16;
		for (ItemFilterBlockEntity.Criterion c : ItemFilterBlockEntity.Criterion.values()) {
			int id = c.ordinal();
			settingButtons.add(this.addRenderableWidget(Button.builder(Component.empty(), b -> this.click(id)).bounds(x, y+id*18, 94, 16).build()));
		}
		shownTagCount = Math.min(SHOWN_TAGS, te.getTags().size());
		for (int i = 0; i < shownTagCount; i++) {
			int id = 10+i;
			settingButtons.add(this.addRenderableWidget(Button.builder(Component.empty(), b -> this.click(id)).bounds(x, y+76+i*16, 94, 14).build()));
		}
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		if (Math.min(SHOWN_TAGS, menu.tile.getTags().size()) != shownTagCount)
			this.rebuild();
		ItemFilterBlockEntity te = menu.tile;
		for (int i = 0; i < settingButtons.size(); i++) {
			if (i < 4) {
				ItemFilterBlockEntity.Criterion c = ItemFilterBlockEntity.Criterion.values()[i];
				settingButtons.get(i).setMessage(Component.literal(c.name().charAt(0)+c.name().substring(1).toLowerCase()+": "+te.getSetting(c).name().toLowerCase()));
			}
			else if (i-4 < te.getTags().size()) {
				String path = te.getTags().get(i-4).getPath();
				settingButtons.get(i).setMessage(Component.literal((path.length() > 9 ? path.substring(0, 9) : path)+": "+te.getTagSetting(i-4).name().toLowerCase()));
			}
		}
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int x = leftPos, y = topPos;
		g.fill(x, y, x+imageWidth, y+imageHeight, 0xff2b2d33);
		g.fill(x+2, y+2, x+imageWidth-2, y+imageHeight-2, 0xff9ea6b0);
		for (Slot s : menu.slots)
			g.fill(x+s.x-1, y+s.y-1, x+s.x+17, y+s.y+17, s.index == 0 ? 0xff4a6a9a : s.index == 1 ? 0xff4a8a4a : s.index < 18 ? 0xff8a4a4a : 0xff6e727a);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, 8, 5, 0xff202020, false);
		g.drawString(font, "Template", 8, 37, 0xff202020, false);
		g.drawString(font, "Buffer", 44, 37, 0xff202020, false);
		g.drawString(font, "Blacklist", 70, 44, 0xff202020, false);
		g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xff404040, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
