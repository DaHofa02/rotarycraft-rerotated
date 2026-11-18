/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import reika.rotarycraft.content.blockentity.MagneticEngineBlockEntity;
import reika.rotarycraft.content.menu.MagneticEngineMenu;

/** Own layout (drawn): tier, speed setting with -/+ buttons, redstone mode, energy and coolant bars, temperature. */
public class MagneticEngineScreen extends AbstractContainerScreen<MagneticEngineMenu> {

	public MagneticEngineScreen(MagneticEngineMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 110;
	}

	@Override
	protected void init() {
		super.init();
		this.addRenderableWidget(Button.builder(Component.literal("-"), b -> this.click(0)).bounds(leftPos+8, topPos+40, 20, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("+"), b -> this.click(1)).bounds(leftPos+90, topPos+40, 20, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Redstone"), b -> this.click(2)).bounds(leftPos+8, topPos+80, 70, 20).build());
	}

	private void click(int id) {
		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int x = leftPos, y = topPos;
		MagneticEngineBlockEntity te = menu.tile;
		g.fill(x, y, x+imageWidth, y+imageHeight, 0xff2b2d33);
		g.fill(x+2, y+2, x+imageWidth-2, y+imageHeight-2, 0xff8f9aa8);
		bar(g, x+140, y+16, te.getStoredEnergy(), te.getMaxStorage(), 0xffc03030);
		bar(g, x+156, y+16, te.getCoolant(), 24000, 0xff9ad8f0);
	}

	private static void bar(GuiGraphics g, int x, int y, int val, int max, int col) {
		int h = 84;
		g.fill(x, y, x+10, y+h, 0xff30323a);
		int f = max <= 0 ? 0 : (int)((long)h*Math.min(val, max)/max);
		g.fill(x+1, y+h-f, x+9, y+h, col);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		MagneticEngineBlockEntity te = menu.tile;
		g.drawString(font, title, 8, 6, 0xff202020, false);
		g.drawString(font, "Tier "+te.getTier()+(te.isEfficient() ? " (efficient)" : ""), 8, 20, 0xff202020, false);
		int s = te.getTargetSpeed();
		g.drawString(font, s > 0 ? s+" rad/s" : "Off", 34, 46, 0xff202020, false);
		g.drawString(font, MagneticEngineBlockEntity.torqueOf(te.getTier())+" Nm, "+te.getConsumption()+" FE/t", 8, 66, 0xff202020, false);
		g.drawString(font, switch (te.getRedstoneMode()) { case IGNORE -> "Always"; case ON_SIGNAL -> "With signal"; case WITHOUT_SIGNAL -> "Without signal"; }, 82, 86, 0xff202020, false);
		g.drawString(font, te.getTemperature()+"C", 112, 6, 0xff202020, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
