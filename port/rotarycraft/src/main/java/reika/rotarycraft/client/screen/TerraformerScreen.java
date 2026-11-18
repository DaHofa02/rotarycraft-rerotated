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

import reika.rotarycraft.content.blockentity.TerraformerBlockEntity;
import reika.rotarycraft.content.menu.TerraformerMenu;

/** Own layout (drawn): target and radius controls, what the target needs, the item slots. */
public class TerraformerScreen extends AbstractContainerScreen<TerraformerMenu> {

	public TerraformerScreen(TerraformerMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 184;
		inventoryLabelY = TerraformerMenu.INV_Y-11;
	}

	private void click(int id) {
		if (minecraft != null && minecraft.gameMode != null)
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
	}

	@Override
	protected void init() {
		super.init();
		this.addRenderableWidget(Button.builder(Component.literal("Target"), b -> this.click(0)).bounds(leftPos+8, topPos+16, 50, 16).build());
		this.addRenderableWidget(Button.builder(Component.literal("-"), b -> this.click(1)).bounds(leftPos+118, topPos+16, 16, 16).build());
		this.addRenderableWidget(Button.builder(Component.literal("+"), b -> this.click(2)).bounds(leftPos+152, topPos+16, 16, 16).build());
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int x = leftPos, y = topPos;
		g.fill(x, y, x+imageWidth, y+imageHeight, 0xff2b2d33);
		g.fill(x+2, y+2, x+imageWidth-2, y+imageHeight-2, 0xff8ea086);
		for (Slot s : menu.slots)
			g.fill(x+s.x-1, y+s.y-1, x+s.x+17, y+s.y+17, s.index < 18 ? 0xff6a7a5a : 0xff6e727a);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		TerraformerBlockEntity te = menu.tile;
		TerraformerBlockEntity.Target t = te.getTarget();
		g.drawString(font, title, 8, 5, 0xff202020, false);
		g.drawString(font, t.name().toLowerCase().replace('_', ' '), 62, 20, 0xff202020, false);
		g.drawString(font, String.valueOf(te.getRadius()), 138, 20, 0xff202020, false);
		g.drawString(font, "Needs "+t.item.getDescription().getString()+(t.water > 0 ? ", "+t.water+" mB water" : "")+", "+t.power+" W", 8, 36, 0xff303030, false);
		g.drawString(font, "Water "+te.getWater()+" mB", 8, 45, 0xff303030, false);
		g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xff404040, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
