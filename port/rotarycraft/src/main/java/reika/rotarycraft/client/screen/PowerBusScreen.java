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
import reika.rotarycraft.content.menu.PowerBusMenu;

/**
 * Original: Reika.RotaryCraft.GUIs.Machine.Inventory.GuiPowerBus (220x220, bus.png) - each usable side's slot tinted in its colour with
 * an 18x18 mode button beside it (buttons.png: torque at u 36, speed at u 54); unusable sides get a blank slot cover.
 */
public class PowerBusScreen extends AbstractContainerScreen<PowerBusMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/bus.png");
	private static final ResourceLocation BUTTONS = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/buttons.png");
	private static final int[] BX = {103, 103, 49, 157}, BY = {26, 134, 80, 80};

	public PowerBusScreen(PowerBusMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 220;
		imageHeight = 220;
		inventoryLabelY = imageHeight+100; //the original draws no inventory label
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (menu.tile != null) {
			for (int i = 0; i < 4; i++) {
				int x = leftPos-2+BX[i], y = topPos-12+BY[i];
				if (menu.tile.canHaveItemInSlot(PowerBusMenu.SIDES[i]) && mx >= x && mx < x+18 && my >= y && my < y+18 && minecraft != null && minecraft.gameMode != null) {
					minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
					return true;
				}
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		g.blit(TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		if (menu.tile == null)
			return;
		for (int i = 0; i < 4; i++) {
			int x = leftPos+PowerBusMenu.SLOT_X[i]-1, y = topPos+PowerBusMenu.SLOT_Y[i]-1;
			if (menu.tile.canHaveItemInSlot(PowerBusMenu.SIDES[i])) {
				g.fill(x, y, x+18, y+18, colorFor(i));
				int u = menu.tile.isSideSpeedMode(PowerBusMenu.SIDES[i]) ? 54 : 36;
				g.blit(BUTTONS, leftPos-2+BX[i], topPos-12+BY[i], u, 36, 18, 18);
			}
			else
				g.blit(TEX, x, y, 8, 8, 18, 18);
		}
	}

	/** Original getColorForSide (translucent: north, south, west, east). */
	private static int colorFor(int i) {
		return switch (i) {
			case 0 -> 1140850943; //north
			case 1 -> 1157562368; //south
			case 2 -> 1140915968; //west
			default -> 1157627648; //east
		};
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
