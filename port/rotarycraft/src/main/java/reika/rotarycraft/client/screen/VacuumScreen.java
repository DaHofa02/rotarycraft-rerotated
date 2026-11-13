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
import reika.rotarycraft.content.menu.VacuumMenu;

/** Original: GuiBasicStorage (basicstorage.png) for six rows of 9 slots, with the "Get XP" button and the stored XP, with the power-only tab. */
public class VacuumScreen extends AbstractContainerScreen<VacuumMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/basicstorage.png");
	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");

	public VacuumScreen(VacuumMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 114+108;
		inventoryLabelY = imageHeight-94;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, 108+17);
		g.blit(TEX, j, k+108+17, 0, 126, imageWidth, 96);
		g.blit(POWERTAB, j+imageWidth, k+4, 0, 4, 42, 24);
		g.blit(POWERTAB, j+imageWidth, k+4+23, 0, 157, 42, 6);
		g.blit(POWERTAB, j+imageWidth+5, k+22, 0, 0, (int)menu.tile.getScaledPower(29), 4);
		g.drawString(font, "Power:", j+imageWidth+20-font.width("Power:")/2, k+9, 0xff000000, false);
	}

	@Override
	protected void init() {
		super.init();
		this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("Get XP"), b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0))
				.bounds(leftPos+imageWidth-1, topPos+32, 43, 20).build());
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		super.renderLabels(g, mx, my);
		String xp = String.valueOf(menu.tile.getExperience());
		g.drawString(font, "XP: "+xp, 150-font.width(xp), 6, 4210752, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
