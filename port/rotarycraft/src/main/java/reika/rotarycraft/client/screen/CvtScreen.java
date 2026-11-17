/*******************************************************************************
 * Private NeoForge 1.21.1 port of RotaryCraft; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import reika.rotarycraft.content.blockentity.AdvancedGearBlockEntity;
import reika.rotarycraft.content.menu.CvtMenu;

/** Own layout (drawn, no texture): belt slots on the left, the required belt and the mode controls on the right. */
public class CvtScreen extends AbstractContainerScreen<CvtMenu> {

	private EditBox target;
	private AdvancedGearBlockEntity.CvtMode shownMode;

	public CvtScreen(CvtMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 216;
		imageHeight = 184;
		inventoryLabelY = CvtMenu.INV_Y-11;
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
		this.clearWidgets();
		target = null;
		AdvancedGearBlockEntity te = menu.tile;
		shownMode = te.getCvtMode();
		int x = leftPos+152, y = topPos+40;
		this.addRenderableWidget(Button.builder(Component.literal(shownMode.name()), b -> this.click(0)).bounds(x, y, 60, 18).build());
		switch (shownMode) {
			case MANUAL -> {
				this.addRenderableWidget(Button.builder(Component.literal("-"), b -> this.click(10+Math.max(1, Math.abs(te.getCvtRatio())/2))).bounds(x, y+22, 18, 18).build());
				this.addRenderableWidget(Button.builder(Component.literal("+"), b -> this.click(10+Math.min(32, Math.abs(te.getCvtRatio())*2))).bounds(x+42, y+22, 18, 18).build());
				this.addRenderableWidget(Button.builder(Component.literal("Spd/Trq"), b -> this.click(3)).bounds(x, y+42, 60, 18).build());
			}
			case REDSTONE -> {
				this.addRenderableWidget(Button.builder(Component.literal("Off"), b -> this.click(1)).bounds(x, y+22, 28, 18).build());
				this.addRenderableWidget(Button.builder(Component.literal("On"), b -> this.click(2)).bounds(x+32, y+22, 28, 18).build());
			}
			case AUTO -> {
				target = new EditBox(font, x, y+30, 60, 16, Component.empty());
				target.setMaxLength(9);
				target.setFilter(s -> s.matches("[0-9]*"));
				target.setValue(String.valueOf(te.getTargetTorque()));
				target.setResponder(s -> {
					if (!s.isEmpty())
						this.click(100+(int)Math.min(Integer.MAX_VALUE-100, Long.parseLong(s)));
				});
				this.addRenderableWidget(target);
			}
		}
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		if (menu.tile.getCvtMode() != shownMode)
			this.rebuild();
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (target != null && target.isFocused() && key != 256) //not Escape: typing must not close the GUI with the inventory key
			return target.keyPressed(key, scan, mods) || target.canConsumeInput() || super.keyPressed(key, scan, mods);
		return super.keyPressed(key, scan, mods);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int x = leftPos, y = topPos;
		g.fill(x, y, x+imageWidth, y+imageHeight, 0xff2b2d33);
		g.fill(x+2, y+2, x+imageWidth-2, y+imageHeight-2, 0xffa8acb4);
		for (Slot s : menu.slots)
			g.fill(x+s.x-1, y+s.y-1, x+s.x+17, y+s.y+17, s.index == 31 ? 0xff8a6a2a : 0xff6e727a);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		AdvancedGearBlockEntity te = menu.tile;
		g.drawString(font, title, 8, 5, 0xff202020, false);
		g.drawString(font, "Max "+te.getMaxRatio()+":1", 120, 5, 0xff202020, false);
		String r = switch (te.getCvtMode()) {
			case MANUAL -> (te.getCvtRatio() > 0 ? "Speed " : "Torque ")+Math.abs(te.getCvtRatio())+":1";
			case REDSTONE -> "Off "+te.getCvtState(false)+" / On "+te.getCvtState(true);
			case AUTO -> "Target torque";
		};
		g.drawString(font, r, 152, te.getCvtMode() == AdvancedGearBlockEntity.CvtMode.MANUAL ? 66 : 84, 0xff202020, false);
		if (te.getLubricant() <= 0)
			g.drawString(font, "No lubricant", 8, 92, 0xffa02020, false);
		g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xff404040, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
