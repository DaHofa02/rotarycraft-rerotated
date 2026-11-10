/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BorerBlockEntity;
import reika.rotarycraft.content.menu.BorerMenu;

/**
 * Original: Reika.RotaryCraft.GUIs.Machine.GuiBorer (borergui2.png, 176x169): the 7x5 cut pattern as image buttons from buttons.png
 * (dark: cut; the bottom centre cell - the machine's own column at its height - has its own image), "Reset Pos'n", "Drops On/Off",
 * "Toggle All", and the power tab. Clicks go to the server as menu button clicks (see {@link BorerMenu}); the screen shows its own copy at
 * once as the original did.
 */
public class BorerScreen extends AbstractContainerScreen<BorerMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/borergui2.png");
	private static final ResourceLocation BUTTONS = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/buttons.png");
	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");

	private final boolean[][] dig = new boolean[BorerBlockEntity.WIDTH][BorerBlockEntity.HEIGHT];
	private boolean drops;

	public BorerScreen(BorerMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 169;
		if (menu.tile != null) {
			drops = menu.tile.drops;
			for (int i = 0; i < BorerBlockEntity.WIDTH; i++)
				for (int j = 0; j < BorerBlockEntity.HEIGHT; j++)
					dig[i][j] = menu.tile.cutShape[i][j];
		}
	}

	@Override
	protected void init() {
		super.init();
		this.rebuild();
	}

	private void rebuild() {
		this.clearWidgets();
		int j = leftPos, k = topPos;
		for (int i = 0; i < BorerBlockEntity.WIDTH; i++) {
			for (int l = 0; l < BorerBlockEntity.HEIGHT; l++) {
				int u = i == 3 && l == 4 ? 36 : 0;
				final int ci = i, cl = l;
				this.addRenderableWidget(new CellButton(j+25+18*i, k+16+18*l, dig[i][l] ? u : u+18, () -> {
					dig[ci][cl] = !dig[ci][cl];
					this.send(BorerMenu.cellButton(ci, cl));
				}));
			}
		}
		this.addRenderableWidget(Button.builder(Component.literal("Reset Pos'n"), b -> this.send(BorerMenu.RESET)).bounds(j+14, k+115, 72, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Toggle All"), b -> {
			for (int i = 0; i < BorerBlockEntity.WIDTH; i++)
				for (int l = 0; l < BorerBlockEntity.HEIGHT; l++)
					dig[i][l] = !dig[i][l];
			this.send(BorerMenu.TOGGLE_ALL);
		}).bounds(j+14, k+140, 148, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal(drops ? "Drops On" : "Drops Off"), b -> {
			drops = !drops;
			this.send(BorerMenu.DROPS);
		}).bounds(j+90, k+115, 72, 20).build());
	}

	private void send(int button) {
		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
		this.rebuild();
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		this.drawPowerTab(g, j, k);
	}

	/** Original GuiBorer.drawPowerTab (its minimum values are all 1: the bars are full with any power). */
	private void drawPowerTab(GuiGraphics g, int j, int k) {
		BorerBlockEntity b = menu.tile;
		if (b == null)
			return;
		g.blit(POWERTAB, j+imageWidth, k+5, 0, 4, 42, 159);
		long frac = Math.min(29, b.power*29L/b.MINPOWER);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-146, 0, 0, (int)frac, 4);
		frac = Math.min(29, (long)b.omega*29L/b.MINSPEED);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-86, 0, 0, (int)frac, 4);
		frac = Math.min(29, (long)b.torque*29L/b.MINTORQUE);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-26, 0, 0, (int)frac, 4);
		g.drawString(font, "Power:", j+imageWidth+20-font.width("Power:")/2, k+12, 0xff000000, false);
		g.drawString(font, "Speed:", j+imageWidth+20-font.width("Speed:")/2, k+71, 0xff000000, false);
		g.drawString(font, "Torque:", j+imageWidth+20-font.width("Torque:")/2, k+130, 0xff000000, false);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}

	/** Original: DragonAPI ImagedGuiButton - an 18x18 image from buttons.png. */
	private static class CellButton extends AbstractButton {

		private final int u;
		private final Runnable action;

		CellButton(int x, int y, int u, Runnable action) {
			super(x, y, 18, 18, Component.empty());
			this.u = u;
			this.action = action;
		}

		@Override
		public void onPress() {
			action.run();
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
			g.blit(BUTTONS, this.getX(), this.getY(), u, 0, 18, 18);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput out) {
			this.defaultButtonNarrationText(out);
		}
	}
}
