/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.menu.DistributionClutchMenu;

/**
 * Original: Reika.RotaryCraft.GUIs.Machine.GuiDistributionClutch - per side (north, south, west, east) a 9x9 switch (buttons.png: grey
 * for the input and the always-on output side, green when on) and a text field for the requested torque.
 */
public class DistributionClutchScreen extends AbstractContainerScreen<DistributionClutchMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/distribclutchgui.png");
	private static final ResourceLocation BUTTONS = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/buttons.png");

	private final EditBox[] amounts = new EditBox[4];

	public DistributionClutchScreen(DistributionClutchMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 160;
		imageHeight = 110;
	}

	private int buttonY(int idx) {
		return DistributionClutchMenu.SIDES[idx].ordinal()*20-12;
	}

	@Override
	protected void init() {
		super.init();
		for (int idx = 0; idx < 4; idx++) {
			Direction dir = DistributionClutchMenu.SIDES[idx];
			boolean enable = menu.tile != null && !menu.tile.isFixedSide(dir);
			EditBox box = new EditBox(font, leftPos+16+15, topPos+this.buttonY(idx)-4, 80, 16, Component.empty());
			box.setMaxLength(10);
			box.setFilter(s -> s.matches("[0-9]*"));
			box.setEditable(enable);
			box.setValue(String.valueOf(menu.tile != null ? menu.tile.getTorqueRequest(dir) : 0));
			int side = idx;
			box.setResponder(s -> {
				int v = s.isEmpty() ? 0 : (int)Math.min(DistributionClutchMenu.MAX_REQUEST, Long.parseLong(s));
				if (minecraft != null && minecraft.gameMode != null)
					minecraft.gameMode.handleInventoryButtonClick(menu.containerId, DistributionClutchMenu.requestButton(side, v));
			});
			amounts[idx] = box;
			this.addRenderableWidget(box);
		}
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		for (int idx = 0; idx < 4; idx++) {
			int x = leftPos+16, y = topPos+this.buttonY(idx);
			if (mx >= x && mx < x+9 && my >= y && my < y+9 && minecraft != null && minecraft.gameMode != null) {
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, idx);
				return true;
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		for (EditBox b : amounts)
			if (b.isFocused() && key != 256) {
				b.keyPressed(key, scan, mods);
				return true;
			}
		return super.keyPressed(key, scan, mods);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		g.blit(TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		if (menu.tile == null)
			return;
		for (int idx = 0; idx < 4; idx++) {
			Direction dir = DistributionClutchMenu.SIDES[idx];
			int u = 9, v = 54;
			if (menu.tile.isFixedSide(dir))
				u = 0;
			else if (menu.tile.isSideEnabled(dir)) {
				u = 0;
				v = 63;
			}
			g.blit(BUTTONS, leftPos+16, topPos+this.buttonY(idx), u, v, 9, 9);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
		for (int i = 0; i < 4; i++)
			g.drawString(font, DistributionClutchMenu.SIDES[i].getName().toUpperCase(java.util.Locale.ROOT), 119, 29+i*20, 4210752, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
