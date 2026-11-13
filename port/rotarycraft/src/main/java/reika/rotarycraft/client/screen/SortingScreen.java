/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.SortingBlockEntity;
import reika.rotarycraft.content.menu.SortingMenu;

/** Original: GuiSorter (sortergui.png, 176x180) - the 27 mapped items in three rows and the power-only tab. */
public class SortingScreen extends AbstractContainerScreen<SortingMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/sortergui.png");
	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");

	public SortingScreen(SortingMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 180;
		inventoryLabelY = imageHeight-94;
	}

	private static int slotX(int k) {
		return 8+k%SortingBlockEntity.LENGTH*18;
	}

	private static int slotY(int k) {
		return 18+k/SortingBlockEntity.LENGTH*22;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		g.blit(POWERTAB, j+imageWidth, k+4, 0, 4, 42, 24);
		g.blit(POWERTAB, j+imageWidth, k+4+23, 0, 157, 42, 6);
		g.blit(POWERTAB, j+imageWidth+5, k+22, 0, 0, (int)menu.tile.getScaledPower(29), 4);
		g.drawString(font, "Power:", j+imageWidth+20-font.width("Power:")/2, k+9, 0xff000000, false);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		super.renderLabels(g, mx, my);
		for (int i = 0; i < 27; i++) {
			ItemStack is = menu.tile.getMapping(i);
			if (!is.isEmpty())
				g.renderItem(is, slotX(i), slotY(i));
		}
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		for (int i = 0; i < 27; i++) {
			int x = leftPos+slotX(i), y = topPos+slotY(i);
			if (mx >= x && mx < x+16 && my >= y && my < y+16) {
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
				return true;
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
