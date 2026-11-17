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
import reika.rotarycraft.content.blockentity.DefoliatorBlockEntity;
import reika.rotarycraft.content.menu.DefoliatorMenu;

/** Original: Reika.RotaryCraft.GUIs.Machine.Inventory.GuiDefoliator (defoliatorgui.png) - the slots and the poison gauge with its tooltip. */
public class DefoliatorScreen extends AbstractContainerScreen<DefoliatorMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/defoliatorgui.png");

	public DefoliatorScreen(DefoliatorMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		g.blit(TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		if (menu.tile != null) {
			int i1 = menu.tile.getPoisonScaled(52);
			g.blit(TEX, leftPos+134, topPos+69-i1, 177, 69-i1, 16, i1);
		}
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		if (menu.tile != null && mx >= leftPos+133 && mx <= leftPos+150 && my >= topPos+16 && my <= topPos+69)
			g.renderTooltip(font, Component.literal(String.format("Poison: %d/%d", menu.tile.getPoison(), DefoliatorBlockEntity.CAPACITY)), mx, my);
		this.renderTooltip(g, mx, my);
	}
}
