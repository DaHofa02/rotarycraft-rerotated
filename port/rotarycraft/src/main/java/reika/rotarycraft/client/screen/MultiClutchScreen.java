/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.MultiClutchBlockEntity;
import reika.rotarycraft.content.menu.MultiClutchMenu;

/**
 * Original: GuiMultiClutch (multigui.png, 176x148) - two columns of eight redstone levels, each with a dust icon (glowstone for the
 * level it gets now) and a button in the colour of its output side (RotaryAux.sideColors) that moves it on to the next side.
 */
public class MultiClutchScreen extends AbstractContainerScreen<MultiClutchMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/multigui.png");
	/** Original RotaryAux.sideColors: cyan, blue, yellow, black, orange, magenta. */
	private static final int[] SIDE_COLORS = {0x00FFFF, 0x0000FF, 0xFFFF00, 0x000000, 0xFF7800, 0xFF00FF};
	private static final String[] SIDE_NAMES = {"Down", "Up", "North", "South", "West", "East"};

	public MultiClutchScreen(MultiClutchMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 148;
	}

	@Override
	protected void init() {
		super.init();
		for (int i = 0; i < 16; i++) {
			int id = i;
			int x = 18+70*(i/8)+15, y = 20+16*(i%8)-1;
			this.addRenderableWidget(new AbstractButton(leftPos+x, topPos+y, 35, 9, Component.literal(String.valueOf(i))) {
				@Override
				public void onPress() {
					minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
				}

				@Override
				protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
					int idx = menu.tile.getSideOfState(id);
					g.fill(this.getX()-1, this.getY()-1, this.getX()+36, this.getY()+10, idx == 3 ? 0xFFFFFFFF : 0xFF000000);
					g.fill(this.getX(), this.getY(), this.getX()+35, this.getY()+9, 0xFF000000 | SIDE_COLORS[idx]);
					g.drawString(font, SIDE_NAMES[idx], this.getX()+1, this.getY()+1, idx != 0 && idx != 2 ? 0xFFFFFF : 0, false);
				}

				@Override
				protected void updateWidgetNarration(NarrationElementOutput out) {
					this.defaultButtonNarrationText(out);
				}
			});
		}
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		g.blit(TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
		MultiClutchBlockEntity te = menu.tile;
		int pwr = te.getLevel() != null ? te.getLevel().getBestNeighborSignal(te.getBlockPos()) : 0;
		for (int i = 0; i < 16; i++) {
			g.renderItem(new ItemStack(i == pwr ? Items.GLOWSTONE_DUST : Items.REDSTONE), 3+70*(i/8), 15+16*(i%8));
			g.drawString(font, String.valueOf(i), 18+70*(i/8), 20+16*(i%8), 0, false);
		}
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
