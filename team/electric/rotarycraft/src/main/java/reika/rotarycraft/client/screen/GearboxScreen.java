/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import reika.rotarycraft.content.blockentity.GearboxBlockEntity;
import reika.rotarycraft.content.item.PowerFormat;
import reika.rotarycraft.content.menu.MachineMenu;

/** Original: Reika.RotaryCraft.GUIs.Machine.GuiGearbox - lubricant gauge, ratio, mode, power and wear. */
public class GearboxScreen extends MachineScreen {

	public GearboxScreen(MachineMenu m, Inventory inv, Component t) {
		super(m, inv, t, "gearboxgui", 176, 84);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		super.renderBg(g, partial, mx, my);
		if (menu.tile instanceof GearboxBlockEntity gbx) {
			int i2 = gbx.getLubricantScaled(55);
			blitTex(g, leftPos + 24, imageHeight/2+topPos+34-i2, 176, 126-i2, 8, i2);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		super.renderLabels(g, mx, my);
		if (!(menu.tile instanceof GearboxBlockEntity gbx))
			return;
		g.drawString(font, "Lubricant", 5, 12, 4210752, false);
		g.drawString(font, "Damage:", 68, 60, 0x000000, false);
		int damage = gbx.getDamagePercent();
		int c = damage < 10 ? 0x00ff00 : damage < 25 ? 0x55ff00 : damage < 50 ? 0xffff00 : damage < 80 ? 0xff5500 : 0xff0000;
		g.drawString(font, String.format("%5d%s", damage, "%"), 122, 60, c, false);
		g.drawString(font, "Ratio:", 80, 24, 0x000000, false);
		g.drawString(font, "Mode:", 80, 36, 0x000000, false);
		g.drawString(font, "Power:", 74, 48, 0x000000, false);
		g.drawString(font, String.format("%5d ", gbx.getRatioFromMetadata()), 127, 24, 0x000000, false);
		g.drawString(font, gbx.reduction ? "Torque" : " Speed", 115, 36, 0x000000, false);
		String pw = PowerFormat.formatPower(gbx.power);
		g.drawString(font, pw, 150-font.width(pw), 48, 0x000000, false);
	}

	@Override
	protected void renderExtraTooltips(GuiGraphics g, int mx, int my) {
		if (menu.tile instanceof GearboxBlockEntity gbx && mx >= leftPos+23 && mx <= leftPos+32 && my >= topPos+20 && my <= topPos+76)
			g.renderTooltip(font, Component.literal(String.format("%.1f/%d", gbx.getLubricant()/1000F, gbx.getMaxLubricant()/1000)), mx, my);
	}
}
