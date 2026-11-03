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
import net.minecraft.world.inventory.Slot;

import net.neoforged.neoforge.network.PacketDistributor;

import reika.rotarycraft.RotaryCraft;
import reika.rotarycraft.content.blockentity.BlastFurnaceBlockEntity;
import reika.rotarycraft.content.menu.BlastFurnaceMenu;
import reika.rotarycraft.network.RotaryNetwork;

/**
 * Original: GuiBlastFurnace (blastfurngui2.png): temperature in the corner and as a bar, progress arrow, a red (locked) or green
 * overlay on the input slots, and the "Leave one item" / "Consume all items" toggle.
 */
public class BlastFurnaceScreen extends AbstractContainerScreen<BlastFurnaceMenu> {

	private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/blastfurngui2.png");
	private static final ResourceLocation BUTTONS = ResourceLocation.fromNamespaceAndPath(RotaryCraft.MOD_ID, "textures/gui/buttons.png");

	public BlastFurnaceScreen(BlastFurnaceMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 176;
		imageHeight = 166;
	}

	private BlastFurnaceBlockEntity blast() {
		return menu.tile;
	}

	@Override
	protected void init() {
		super.init();
		this.clearWidgets();
		boolean leave = blast().leaveLastItem;
		int u = leave ? 54 : 42;
		Component tip = Component.literal(leave ? "Leave one item" : "Consume all items");
		AbstractButton b = new AbstractButton(leftPos+124, topPos+20, 12, 12, Component.empty()) {
			@Override
			public void onPress() {
				boolean now = !blast().leaveLastItem;
				blast().leaveLastItem = now;
				PacketDistributor.sendToServer(new RotaryNetwork.BlastLeaveOne(blast().getBlockPos(), now));
				BlastFurnaceScreen.this.init();
			}

			@Override
			protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
				g.blit(BUTTONS, this.getX(), this.getY(), u, 96, 12, 12);
			}

			@Override
			protected void updateWidgetNarration(NarrationElementOutput out) {
				this.defaultButtonNarrationText(out);
			}
		};
		b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tip));
		this.addRenderableWidget(b);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int j = leftPos, k = topPos;
		g.blit(TEX, j, k, 0, 0, imageWidth, imageHeight);
		int i1 = blast().getCookScaled(24);
		g.blit(TEX, j+119, k+34, 176, 14, i1+1, 16);
		int i2 = blast().getTemperatureScaled(54);
		g.blit(TEX, j+11, k+70-i2, 176, 86-i2, 10, i2);

		for (int i = 0; i < menu.slots.size(); i++) {
			if (menu.isLockable(i)) {
				Slot s = menu.slots.get(i);
				int clr = 0x50000000 | (blast().lockedSlots[menu.machineSlotOf(i)] ? 0xff0000 : 0x00b000);
				g.fill(j+s.x, k+s.y, j+s.x+16, k+s.y+16, clr);
			}
		}
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		g.drawString(font, title, imageWidth/2-font.width(title)/2, 5, 4210752, false);
		g.drawString(font, playerInventoryTitle, imageWidth-58, imageHeight-96+3, 4210752, false);
		int c = blast().getTemperature() >= 1000 ? 1 : 0;
		String t = blast().getTemperature()+"C";
		g.drawString(font, t, 17+c-font.width(t)/2, 6, 4210752, false);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		this.renderTooltip(g, mx, my);
	}
}
