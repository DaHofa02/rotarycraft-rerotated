/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.engine.EngineBlockEntity;
import reika.rotarycraft.content.menu.MachineMenu;

/**
 * Original: Reika.RotaryCraft.Base.GuiMachine / GuiNonPoweredMachine / GuiEngine. Background, centred title and inventory label are
 * drawn as before. The "?" handbook tab is not ported yet (the handbook comes in step 5).
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {

	private final ResourceLocation texture;

	public MachineScreen(MachineMenu menu, Inventory inv, Component title, String guiTexture, int xSize, int ySize) {
		super(menu, inv, title);
		texture = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/"+guiTexture+".png");
		imageWidth = xSize;
		imageHeight = ySize;
	}

	protected boolean labelInventory() {
		return menu.layout != MachineMenu.Layout.STEAM && menu.tile instanceof EngineBlockEntity;
	}

	protected void blitTex(GuiGraphics g, int x, int y, int u, int v, int w, int h) {
		g.blit(texture, x, y, u, v, w, h);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		blitTex(g, leftPos, topPos, 0, 0, imageWidth, imageHeight);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
		if (this.labelInventory())
			g.drawString(font, Component.translatable("container.inventory"), imageWidth-58, imageHeight-96+3, 4210752, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
		this.renderExtraTooltips(g, mx, my);
	}

	protected void renderExtraTooltips(GuiGraphics g, int mx, int my) {
		if (menu.tile instanceof EngineBlockEntity eng && eng.getEngineType().burnsFuel()) {
			int[] bar = this.getFuelBar();
			if (bar != null && mx >= leftPos+bar[0] && mx <= leftPos+bar[0]+bar[2] && my >= topPos+bar[1] && my <= topPos+bar[1]+bar[3]) {
				int time = eng.getFuelDuration();
				ChatFormatting color = time == 0 ? ChatFormatting.GRAY : time < 30 ? ChatFormatting.RED : time < 45 ? ChatFormatting.GOLD : time < 60 ? ChatFormatting.YELLOW : ChatFormatting.WHITE;
				int hour = time*20/72000, minute = (time*20-hour*72000)/1200, second = (time*20-hour*72000-minute*1200)/20;
				g.renderTooltip(font, Component.literal(String.format("Fuel: %02d:%02d:%02d", hour, minute, second)).withStyle(color), mx, my);
			}
		}
	}

	/** Original GuiEngine.getFuelBarX/YPos and X/YSize. */
	protected int[] getFuelBar() {
		return null;
	}
}
