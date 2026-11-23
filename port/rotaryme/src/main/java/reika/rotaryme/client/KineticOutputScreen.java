/*******************************************************************************
 * RotaryCraft ME Kinetics: own addon for the private RotaryCraft port; not for distribution.
 ******************************************************************************/
package reika.rotaryme.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import reika.rotaryme.KineticOutputBlockEntity;
import reika.rotaryme.KineticOutputMenu;
import reika.rotaryme.RotaryME;
import reika.rotaryme.SetOutputPayload;

/** Own drawn layout: speed (rad/s) and torque (Nm) fields with a Set button, the resulting power (W), network energy, lubricant and state. */
public class KineticOutputScreen extends AbstractContainerScreen<KineticOutputMenu> {

	private EditBox speed;
	private EditBox torque;
	private Button redstone;

	public KineticOutputScreen(KineticOutputMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 196;
		imageHeight = KineticOutputMenu.OUTPUT_INV_Y+82;
		inventoryLabelY = KineticOutputMenu.OUTPUT_INV_Y-11;
	}

	@Override
	protected void init() {
		super.init();
		KineticOutputBlockEntity te = menu.tile;
		speed = new EditBox(font, leftPos+40, topPos+20, 80, 16, Component.literal("Speed"));
		speed.setFilter(s -> s.matches("\\d{0,10}"));
		speed.setValue(String.valueOf(te.getSetSpeed()));
		torque = new EditBox(font, leftPos+40, topPos+40, 80, 16, Component.literal("Torque"));
		torque.setFilter(s -> s.matches("\\d{0,10}"));
		torque.setValue(String.valueOf(te.getSetTorque()));
		this.addRenderableWidget(speed);
		this.addRenderableWidget(torque);
		this.addRenderableWidget(Button.builder(Component.translatable("gui.rotaryme.set"), b -> this.send()).bounds(leftPos+124, topPos+30, 40, 20).build());
		redstone = Button.builder(Component.empty(), b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0)).bounds(leftPos+118, topPos+4, 50, 14).build();
		this.addRenderableWidget(redstone);
	}

	private void send() {
		int s = speed.getValue().isEmpty() ? 0 : (int)Math.min(Integer.MAX_VALUE, Long.parseLong(speed.getValue()));
		int p = torque.getValue().isEmpty() ? 0 : (int)Math.min(Integer.MAX_VALUE, Long.parseLong(torque.getValue()));
		PacketDistributor.sendToServer(new SetOutputPayload(s, p));
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if ((speed.isFocused() || torque.isFocused()) && key != 256) //typing goes to the fields, not the inventory key
			return speed.keyPressed(key, scan, mods) || torque.keyPressed(key, scan, mods) || true;
		return super.keyPressed(key, scan, mods);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int x = leftPos, y = topPos;
		g.fill(x, y, x+imageWidth, y+imageHeight, 0xff23262e);
		g.fill(x+2, y+2, x+imageWidth-2, y+imageHeight-2, 0xff8a93a6);
		int h = 44, l = menu.tile.getLubricant();
		g.fill(x+175, y+100, x+185, y+100+h, 0xff30323a); //lubricant tank, under the card column
		int f = h*l/KineticOutputBlockEntity.TANK;
		g.fill(x+176, y+100+h-f, x+184, y+100+h, 0xffd8b040);
		KineticInputScreen.slots(g, x, y, menu.slots);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		KineticOutputBlockEntity te = menu.tile;
		int c = 0xff202020;
		g.drawString(font, title, 8, 6, c, false);
		g.drawString(font, "rad/s", 8, 24, c, false);
		g.drawString(font, "Nm", 8, 44, c, false);
		g.drawString(font, "Power: "+String.format(java.util.Locale.ROOT, "%,d", te.getSetPower())+" W", 8, 64, c, false);
		int red = 0xff902020;
		g.drawString(font, "Network: "+RotaryME.formatJoules(te.networkEnergyClient), 8, 76, c, false);
		g.drawString(font, Component.translatable("gui.rotaryme.lube.use", String.format(java.util.Locale.ROOT, "%.2f", te.getLubricantPerTick()*20),
				Math.round(te.getLubricantFactor()*100)), 8, 90, c, false);
		g.drawString(font, Component.translatable("gui.rotaryme.lube.tank", te.getLubricant()), 8, 100, c, false);
		long net = te.networkLubricantClient;
		Component me = net < 0 ? Component.translatable("gui.rotaryme.lube.me.none") : net == 0 ? Component.translatable("gui.rotaryme.lube.me.empty")
				: Component.translatable("gui.rotaryme.lube.me", String.format(java.util.Locale.ROOT, "%,d", net));
		g.drawString(font, me, 8, 110, net > 0 ? c : red, false);
		boolean tank = te.usesOwnTank();
		g.drawString(font, Component.translatable(tank ? "gui.rotaryme.lube.from.tank" : "gui.rotaryme.lube.from.me"), 8, 120, tank || net > 0 ? c : red, false);
		g.drawString(font, Component.translatable("gui.rotaryme.state."+te.problem), 8, 132, te.problem == 0 ? c : red, false);
		g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, c, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		KineticOutputBlockEntity te = menu.tile;
		redstone.visible = te.hasRedstoneCard(); //the mode button only shows with a redstone card in
		redstone.setMessage(Component.translatable(te.runsWithSignal() ? "gui.rotaryme.redstone.on" : "gui.rotaryme.redstone.off"));
		redstone.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(te.runsWithSignal() ? "gui.rotaryme.redstone.on.tip" : "gui.rotaryme.redstone.off.tip")));
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
