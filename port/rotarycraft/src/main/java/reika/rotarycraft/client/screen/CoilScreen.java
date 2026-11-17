/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import reika.rotarycraft.content.menu.CoilMenu;

/** Own layout (drawn, no texture): output speed and torque fields, stored energy and charging thresholds. */
public class CoilScreen extends AbstractContainerScreen<CoilMenu> {

	private EditBox speed;
	private EditBox torque;

	public CoilScreen(CoilMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 110;
	}

	@Override
	protected void init() {
		super.init();
		speed = this.field(28, menu.tile.getReleaseOmega(), false);
		torque = this.field(56, menu.tile.getReleaseTorque(), true);
	}

	private EditBox field(int y, int value, boolean isTorque) {
		EditBox e = new EditBox(font, leftPos+imageWidth/2-10, topPos+y, 56, 16, Component.empty());
		e.setMaxLength(9);
		e.setFilter(s -> s.matches("[0-9]*"));
		e.setValue(String.valueOf(value));
		e.setResponder(s -> {
			if (s.isEmpty() || minecraft == null || minecraft.gameMode == null)
				return;
			long v = Long.parseLong(s);
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, (int)Math.min(Integer.MAX_VALUE/2, v)*2+(isTorque ? 1 : 0));
		});
		return this.addRenderableWidget(e);
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		for (EditBox e : new EditBox[] {speed, torque})
			if (e.isFocused() && key != 256) //not Escape: typing must not close the GUI with the inventory key
				return e.keyPressed(key, scan, mods) || e.canConsumeInput() || super.keyPressed(key, scan, mods);
		return super.keyPressed(key, scan, mods);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int x = leftPos, y = topPos;
		g.fill(x, y, x+imageWidth, y+imageHeight, 0xff2b2d33);
		g.fill(x+2, y+2, x+imageWidth-2, y+imageHeight-2, 0xff5a5e66);
		g.fill(x+4, y+16, x+imageWidth-4, y+imageHeight-4, 0xff3c3f46);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, 8, 5, 0xffe0e0e0, false);
		g.drawString(font, "Output Speed", 8, 32, 0xffd0d0d0, false);
		g.drawString(font, "rad/s", imageWidth/2+50, 32, 0xffd0d0d0, false);
		g.drawString(font, "Output Torque", 8, 60, 0xffd0d0d0, false);
		g.drawString(font, "Nm", imageWidth/2+50, 60, 0xffd0d0d0, false);
		g.drawString(font, String.format("Stored: %.3e J", menu.tile.getEnergy()/20D), 8, 82, 0xffffd060, false);
		g.drawString(font, "Charge needs "+menu.tile.getChargingTorque()+" Nm, "+menu.tile.getChargingPower()+" W", 8, 94, 0xffb0b0b0, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
