/*******************************************************************************
 * @author Reika Kalseki (original RotaryCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.rotarycraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.WinderBlockEntity;
import reika.rotarycraft.content.menu.WinderMenu;
import reika.rotarycraft.network.RotaryNetwork;

/**
 * Original: GuiWinder (GuiOneSlotInv, basic_gui_oneslot.png) - the coil slot, a button switching between input (winding) and output
 * (unwinding) mode, and the power tab.
 */
public class WinderScreen extends AbstractContainerScreen<WinderMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/basic_gui_oneslot.png");
	private static final ResourceLocation POWERTAB = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/powertab.png");

	private boolean input;

	public WinderScreen(WinderMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
		input = menu.tile.winding;
	}

	@Override
	protected void init() {
		super.init();
		this.clearWidgets();
		this.addRenderableWidget(Button.builder(Component.literal(input ? "Input Mode" : "Output Mode"), b -> {
			PacketDistributor.sendToServer(new RotaryNetwork.WinderToggle(menu.tile.getBlockPos()));
			input = !input;
			this.init();
		}).bounds(leftPos+imageWidth/2-35, topPos+imageHeight/2-26, 65, 20).build());
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		WinderBlockEntity te = menu.tile;
		g.blit(POWERTAB, j+imageWidth, k+4, 0, 4, 42, imageHeight-4);
		if (te.MINPOWER > 0) { //original: bars only make sense with requirements; the winder has none, so they stay empty
			g.blit(POWERTAB, j+imageWidth+5, imageHeight+k-144, 0, 0, (int)Math.min(29, te.power*29L/te.MINPOWER), 4);
		}
		g.drawString(font, "Power:", j+imageWidth+20-font.width("Power:")/2, k+9, 0xff000000, false);
		g.drawString(font, "Speed:", j+imageWidth+20-font.width("Speed:")/2, k+69, 0xff000000, false);
		g.drawString(font, "Torque:", j+imageWidth+20-font.width("Torque:")/2, k+129, 0xff000000, false);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
		g.drawString(font, playerInventoryTitle, 8, imageHeight-96+2, 4210752, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
