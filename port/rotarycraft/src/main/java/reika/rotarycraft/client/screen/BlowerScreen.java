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
import reika.rotarycraft.content.blockentity.BlowerBlockEntity;
import reika.rotarycraft.content.menu.BlowerMenu;

/** Original: GuiBlower (blowergui.png) - the 18 filter items, the four switches with their tooltips and the power tab. */
public class BlowerScreen extends AbstractContainerScreen<BlowerMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/blowergui.png");
	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");
	private static final String[][] TIPS = {{"Blacklist", "Whitelist"}, {"Ignore Metadata", "Use Metadata"}, {"Ignore NBT", "Use NBT"}, {"Match Exact", "Use Ore Dictionary"}};

	public BlowerScreen(BlowerMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 192;
		inventoryLabelY = imageHeight-94;
	}

	private boolean control(int i) {
		BlowerBlockEntity te = menu.tile;
		return switch (i) {
			case 0 -> te.isWhitelist;
			case 1 -> te.checkMeta;
			case 2 -> te.checkNBT;
			default -> te.useOreDict;
		};
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		for (int i = 0; i < 4; i++)
			g.blit(TEX, j+25+36*i, k+64, 176+(this.control(i) ? 18 : 0), 54+18*i, 18, 18);
		BlowerBlockEntity te = menu.tile;
		g.blit(POWERTAB, j+imageWidth, k+4, 0, 4, 42, imageHeight-4);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-144, 0, 0, (int)Math.min(29, te.power*29L/te.MINPOWER), 4);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-84, 0, 0, (int)Math.min(29, te.omega*29L/te.MINSPEED), 4);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-24, 0, 0, (int)Math.min(29, te.torque*29L/te.MINTORQUE), 4);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		super.renderLabels(g, mx, my);
		for (int i = 0; i < 18; i++) {
			ItemStack is = menu.tile.matchingItems[i];
			if (!is.isEmpty())
				g.renderItem(is, 8+i%9*18, 21+i/9*18);
		}
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		for (int i = 0; i < 4; i++)
			if (mx >= leftPos+25+36*i && mx < leftPos+43+36*i && my >= topPos+64 && my < topPos+82) {
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
				return true;
			}
		for (int i = 0; i < 18; i++) {
			int x = leftPos+8+i%9*18, y = topPos+21+i/9*18;
			if (mx >= x && mx < x+16 && my >= y && my < y+16) {
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 10+i);
				return true;
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
		for (int i = 0; i < 4; i++)
			if (mx >= leftPos+25+36*i && mx < leftPos+43+36*i && my >= topPos+64 && my < topPos+82)
				g.renderTooltip(font, Component.literal(TIPS[i][this.control(i) ? 1 : 0]), mx, my);
	}
}
