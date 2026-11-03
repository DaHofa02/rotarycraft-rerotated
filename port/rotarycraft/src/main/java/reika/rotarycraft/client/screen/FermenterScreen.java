/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.FermenterBlockEntity;
import reika.rotarycraft.content.item.CraftingPart;
import reika.rotarycraft.content.menu.FermenterMenu;
import reika.rotarycraft.registry.RotaryItems;

/**
 * Original: GuiFermenter (fermentergui.png) - progress arrow, temperature bar, water level in the input box, the current target
 * (yeast, or sludge with a redstone signal) and the power tab.
 */
public class FermenterScreen extends AbstractContainerScreen<FermenterMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/fermentergui.png");
	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");

	public FermenterScreen(FermenterMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		FermenterBlockEntity ferm = menu.tile;
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		int i1 = Math.min(48, ferm.getCookProgressScaled(48));
		g.blit(TEX, j+79, k+34, 176, 14, i1+1, 16);
		int i2 = Math.min(54, ferm.getTemperatureScaled(54));
		g.blit(TEX, j+24, k+70-i2, 177, 86-i2, 9, i2);
		int h = 16*ferm.getWater()/FermenterBlockEntity.CAPACITY;
		FluidGui.drawFluid(g, Fluids.WATER, j+55, k+35+16-h, 16, h);
		this.drawPowerTab(g, j, k);
	}

	/** Original GuiFermenter.drawPowerTab. */
	private void drawPowerTab(GuiGraphics g, int j, int k) {
		FermenterBlockEntity ferm = menu.tile;
		g.blit(POWERTAB, j+imageWidth, k+4, 0, 4, 42, imageHeight-4);
		long frac = Math.min(29, (ferm.power*29L)/ferm.MINPOWER);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-144, 0, 0, (int)frac, 4);
		frac = Math.min(29, (ferm.omega*29L)/ferm.MINSPEED);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-84, 0, 0, (int)frac, 4);
		frac = Math.min(29, (ferm.torque*29L)/ferm.MINTORQUE);
		g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-24, 0, 0, (int)frac, 4);
		g.drawString(font, "Power:", j+imageWidth+20-font.width("Power:")/2, k+9, 0xff000000, false);
		g.drawString(font, "Speed:", j+imageWidth+20-font.width("Speed:")/2, k+69, 0xff000000, false);
		g.drawString(font, "Torque:", j+imageWidth+20-font.width("Torque:")/2, k+129, 0xff000000, false);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
		g.drawString(font, playerInventoryTitle, imageWidth-58, imageHeight-96+3, 4210752, false);
		ItemStack target = menu.tile.hasRedstoneSignal() ? new ItemStack(RotaryItems.part(CraftingPart.SLUDGE)) : new ItemStack(RotaryItems.YEAST.get());
		g.renderItem(target, 154, 6);
		g.drawString(font, "Target", 119, 10, 0, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
		int j = leftPos, k = topPos;
		if (mx >= j+154 && mx <= j+170 && my >= k+6 && my <= k+22)
			g.renderTooltip(font, Component.literal("This controls automation."), mx, my);
		if (mx >= j+54 && mx <= j+72 && my >= k+34 && my <= k+52)
			g.renderTooltip(font, Component.literal(String.format("Water: %d/%d mB", menu.tile.getWater(), FermenterBlockEntity.CAPACITY)), mx, my);
	}
}
