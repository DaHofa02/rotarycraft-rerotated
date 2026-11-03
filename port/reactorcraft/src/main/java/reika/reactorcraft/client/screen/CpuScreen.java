/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import net.neoforged.neoforge.network.PacketDistributor;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.menu.CpuMenu;
import reika.reactorcraft.network.ReactorPayloads;
import reika.reactorcraft.reactor.ControlRodBlockEntity;
import reika.reactorcraft.reactor.CpuBlockEntity;

/**
 * Original: GuiCPU (control2.png, 176x210) - "Retract All" / "Insert All", a map of the rods in one layer (3 px squares every 5 px around
 * the CPU: green inserted, red retracted, grey without enough power), the layer chosen with the two side buttons; clicking a rod toggles its stack.
 */
public class CpuScreen extends AbstractContainerScreen<CpuMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "textures/gui/control2.png");
	private static final ResourceLocation BUTTONS = ResourceLocation.fromNamespaceAndPath("rotarycraft", "textures/gui/buttons.png");

	public static final int BUTTON_SIZE = 3;
	public static final int BUTTON_SPACE = 5;
	private int offsetY = 0;

	public CpuScreen(CpuMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 210;
	}

	@Override
	protected void init() {
		super.init();
		int j = leftPos, k = topPos;
		this.addRenderableWidget(Button.builder(Component.literal("Retract All"), b -> this.send(ReactorPayloads.CpuAction.RAISE, BlockPos.ZERO))
				.bounds(j+8, -1+k+19, 72, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Insert All"), b -> this.send(ReactorPayloads.CpuAction.LOWER, BlockPos.ZERO))
				.bounds(j+96, -1+k+19, 72, 20).build());
		this.addRenderableWidget(new ImageButton(j+7, k+84, 90, 60, () -> {
			if (menu.tile.getMinY() < offsetY)
				offsetY--;
		}));
		this.addRenderableWidget(new ImageButton(j+157, k+84, 90, 108, () -> {
			if (menu.tile.getMaxY() > offsetY)
				offsetY++;
		}));
	}

	private void send(ReactorPayloads.CpuAction a, BlockPos rod) {
		PacketDistributor.sendToServer(new ReactorPayloads.CpuControl(menu.tile.getBlockPos(), a.ordinal(), rod));
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		g.blit(TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
		CpuBlockEntity tile = menu.tile;
		int r = BUTTON_SIZE;
		int s = BUTTON_SPACE;
		int ox = 1+imageWidth/2-s/2-1;
		int oy = imageHeight/2-s/2+5;
		for (int a = tile.getMinX(); a <= tile.getMaxX(); a++) {
			for (int b = tile.getMinZ(); b <= tile.getMaxZ(); b++) {
				if (a != 0 || b != 0) {
					int c = tile.getDisplayColorAtRelativePosition(a, offsetY, b);
					int x = ox+a*s;
					int y = oy+b*s;
					g.fill(x, y, x+r, y+r, 0xff000000 | c);
				}
			}
		}
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		boolean ret = super.mouseClicked(x, y, button);
		int s = BUTTON_SPACE;
		int ox = 1+leftPos+imageWidth/2-s/2-1;
		int oy = topPos+imageHeight/2-s/2+5;
		int a = Mth.floor((x-ox)/s);
		int b = Mth.floor((y-oy)/s);
		ControlRodBlockEntity rod = menu.tile.getControlRodAtRelativePosition(a, offsetY, b);
		if (rod != null) {
			this.send(ReactorPayloads.CpuAction.TOGGLE, new BlockPos(a, offsetY, b));
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 0.9F, 0.5F));
			return true;
		}
		return ret;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}

	/** Original ImagedGuiButton with RotaryCraft's buttons.png (12x48 at u,v, no hover image). */
	private static final class ImageButton extends Button {
		private final int u, v;

		ImageButton(int x, int y, int u, int v, Runnable action) {
			super(x, y, 12, 48, Component.empty(), b -> action.run(), DEFAULT_NARRATION);
			this.u = u;
			this.v = v;
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
			g.blit(BUTTONS, this.getX(), this.getY(), u, v, width, height, 256, 256);
		}
	}
}
