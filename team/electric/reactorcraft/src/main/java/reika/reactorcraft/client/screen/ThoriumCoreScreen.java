/*******************************************************************************
 * @author Reika Kalseki (original ReactorCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.reactorcraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import reika.reactorcraft.ReactorCraft;
import reika.reactorcraft.content.menu.ThoriumCoreMenu;
import reika.rotarycraft.client.screen.FluidGui;

/** Original: GuiThoriumCore (fuelpool.png, 100 high) - fuel salts, heated fuel salts and waste as 16x70 tank displays with tooltips. */
public class ThoriumCoreScreen extends AbstractContainerScreen<ThoriumCoreMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(ReactorCraft.MOD_ID, "textures/gui/fuelpool.png");

	public ThoriumCoreScreen(ThoriumCoreMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 100;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		g.blit(TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		this.tank(g, menu.tile.getFuelTank(), leftPos+53);
		this.tank(g, menu.tile.getOutputTank(), leftPos+80);
		this.tank(g, menu.tile.getWasteTank(), leftPos+107);
	}

	private void tank(GuiGraphics g, FluidTank t, int x) {
		if (t.isEmpty())
			return;
		int h = t.getFluidAmount()*70/t.getCapacity();
		FluidGui.drawFluid(g, t.getFluid().getFluid(), x, topPos+23+70-h, 16, h);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		int x = mx-leftPos, y = my-topPos;
		if (y >= 22 && y <= 72) {
			if (x >= 52 && x <= 70)
				g.renderTooltip(font, Component.literal("Fuel Salts"), mx, my);
			else if (x >= 79 && x <= 97)
				g.renderTooltip(font, Component.literal("Heated Fuel Salts"), mx, my);
			else if (x >= 106 && x <= 128)
				g.renderTooltip(font, Component.literal("Waste"), mx, my);
		}
	}
}
