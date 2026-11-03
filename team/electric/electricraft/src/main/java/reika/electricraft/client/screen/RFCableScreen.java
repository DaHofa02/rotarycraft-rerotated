/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import net.neoforged.neoforge.network.PacketDistributor;

import reika.electricraft.ElectriCraft;
import reika.electricraft.content.menu.RFCableMenu;
import reika.electricraft.network.ElectriPayloads;
import reika.rotarycraft.lib.math.ReikaEngLibrary;
import reika.rotarycraft.lib.math.ReikaMathLibrary;

/**
 * Original: GuiRFCable - six rows of +/- buttons for 10^0..10^5 RF/t (10^3..10^8 with shift held), a reset button, and the network
 * limit. Each click sends the new limit to the server.
 */
public class RFCableScreen extends AbstractContainerScreen<RFCableMenu> {

	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ElectriCraft.MOD_ID, "textures/gui/rfcablegui.png");
	private static final ResourceLocation BUTTONS = ResourceLocation.fromNamespaceAndPath("rotarycraft", "textures/gui/buttons.png");

	private int limit;

	public RFCableScreen(RFCableMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 197;
		imageHeight = 103;
		limit = menu.tile != null ? menu.tile.getRFLimit() : 0;
	}

	private static int getPower(int base) {
		return Math.abs(base)+(Screen.hasShiftDown() ? 3 : 0);
	}

	@Override
	protected void init() {
		super.init();
		this.clearWidgets();
		for (int i = 0; i < 6; i++) {
			int dx = imageWidth/2-12;
			int dy = 20+i*12;
			int w = 50;
			final int id = i;
			final int nve = i != 0 ? -i : -40;
			this.addRenderableWidget(new ImageButton(leftPos+dx-w, topPos+dy, 18, 54, () -> this.change(id)));
			this.addRenderableWidget(new ImageButton(leftPos+dx+w, topPos+dy, 42, 54, () -> this.change(nve)));
		}
		this.addRenderableWidget(Button.builder(Component.literal("Reset"), b -> this.reset()).bounds(leftPos+imageWidth/2-20, topPos+72, 40, 20).build());
	}

	private void reset() {
		limit = 0;
		this.send();
	}

	/** Original actionPerformed: id 0..5 add 10^id, -1..-5 and -40 (for 0) subtract; never below 0. */
	private void change(int id) {
		int amt = id == -40 ? 1 : (int)Math.pow(10, getPower(id));
		if (id < 0)
			amt = -amt;
		limit = (int)Math.max(0, Math.min(Integer.MAX_VALUE, (long)limit+amt));
		this.send();
	}

	private void send() {
		if (menu.tile != null) {
			menu.tile.setRFLimit(limit);
			PacketDistributor.sendToServer(new ElectriPayloads.RFCableLimit(menu.tile.getBlockPos(), limit));
		}
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		int dx = imageWidth/2;
		int dy = imageHeight/2-4;
		centered(g, "Network Limit:", dx, dy-6);
		centered(g, String.format("%.3f %sRF/t", ReikaMathLibrary.getThousandBase(limit), ReikaEngLibrary.getSIPrefix(limit)), dx, dy+6);
		for (int i = 0; i < 6; i++) {
			int dx2 = imageWidth/2-12;
			int dy2 = 20+i*12+3;
			int w = 53;
			int n = (int)Math.pow(10, getPower(i));
			String sl = String.format("+%d%s", (int)ReikaMathLibrary.getThousandBase(n), ReikaEngLibrary.getSIPrefix(n));
			String sr = String.format("-%d%s", (int)ReikaMathLibrary.getThousandBase(n), ReikaEngLibrary.getSIPrefix(n));
			g.drawString(font, sl, dx2-w-font.width(sl), dy2, 0, false);
			g.drawString(font, sr, dx2+w+24, dy2, 0, false);
		}
		centered(g, "RF Cable Network", dx, 6);
	}

	private void centered(GuiGraphics g, String s, int x, int y) {
		g.drawString(font, s, x-font.width(s)/2, y, 4210752, false);
	}

	/** Original ImagedGuiButton with RotaryCraft's buttons.png (24x12 at u,v, no hover image). */
	private static final class ImageButton extends Button {
		private final int u, v;

		ImageButton(int x, int y, int u, int v, Runnable action) {
			super(x, y, 24, 12, Component.empty(), b -> action.run(), DEFAULT_NARRATION);
			this.u = u;
			this.v = v;
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
			g.blit(BUTTONS, this.getX(), this.getY(), u, v, width, height, 256, 256);
		}
	}
}
