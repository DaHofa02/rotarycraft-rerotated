/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.LavaMakerBlockEntity;
import reika.rotarycraft.content.menu.LavaMakerMenu;

/**
 * Original: GuiRockMelter (lavamakergui.png, a GuiPowerOnlyMachine) - the 3x3 rock slots, the liquid column (16x64) with
 * "name: level/capacity mB" on hover and the one-bar power tab.
 */
public class LavaMakerScreen extends AbstractContainerScreen<LavaMakerMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/lavamakergui.png");
	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");

	public LavaMakerScreen(LavaMakerMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		LavaMakerBlockEntity te = menu.tile;
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		if (!te.getTank().isEmpty()) {
			int i2 = te.getTank().getFluidAmount()*64/LavaMakerBlockEntity.CAPACITY;
			FluidGui.drawFluid(g, te.getTank().getFluid().getFluid(), j+134, k+73-i2+1, 16, i2);
		}
		g.blit(POWERTAB, j+imageWidth, k+4, 0, 4, 42, 24);
		g.blit(POWERTAB, j+imageWidth, k+4+23, 0, 157, 42, 6);
		long frac = Math.min(29, (te.power*29L)/te.MINPOWER);
		g.blit(POWERTAB, j+imageWidth+5, k+22, 0, 0, (int)frac, 4);
		g.drawString(font, "Power:", j+imageWidth+20-font.width("Power:")/2, k+9, 0xff000000, false);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
		g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 4210752, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
		int j = leftPos, k = topPos;
		LavaMakerBlockEntity te = menu.tile;
		if (!te.getTank().isEmpty() && mx >= j+133 && mx <= j+151 && my >= k+9 && my <= k+75) {
			String s = String.format("%s: %d/%d mB", te.getTank().getFluid().getHoverName().getString(), te.getTank().getFluidAmount(),
					LavaMakerBlockEntity.CAPACITY);
			g.renderTooltip(font, Component.literal(s), mx, my);
		}
	}
}
