/*******************************************************************************
 * @author Reika Kalseki (original ElectriCraft). Private NeoForge 1.21.1 port; not for distribution.
 ******************************************************************************/
package reika.electricraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import net.neoforged.neoforge.network.PacketDistributor;

import reika.electricraft.ElectriCraft;
import reika.electricraft.content.blockentity.TransformerBlockEntity;
import reika.electricraft.content.menu.TransformerMenu;
import reika.electricraft.network.ElectriPayloads;

/**
 * Original: GuiTransformer - two 4-digit fields for the turns n1 and n2 (digits only), sent to the server as they are typed; while a
 * field is not focused it shows the current value; below, the efficiency.
 */
public class TransformerScreen extends AbstractContainerScreen<TransformerMenu> {

	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ElectriCraft.MOD_ID, "textures/gui/transformergui.png");

	private EditBox input;
	private EditBox input2;

	public TransformerScreen(TransformerMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		imageWidth = 197;
		imageHeight = 103;
	}

	@Override
	protected void init() {
		super.init();
		int j = (width-imageWidth)/2+8;
		int k = (height-imageHeight)/2-12;
		input = new EditBox(font, j+imageWidth/2-65, k+40, 40, 16, Component.empty());
		input.setMaxLength(4);
		input.setFilter(s -> s.matches("[0-9 ]*"));
		input.setResponder(s -> this.send());
		input2 = new EditBox(font, j+imageWidth/2-10, k+40, 40, 16, Component.empty());
		input2.setMaxLength(4);
		input2.setFilter(s -> s.matches("[0-9 ]*"));
		input2.setResponder(s -> this.send());
		this.addRenderableWidget(input);
		this.addRenderableWidget(input2);
	}

	private static int parse(String s) {
		try {
			return Math.max(0, Integer.parseInt(s.replace(" ", "")));
		}
		catch (NumberFormatException e) {
			return -1;
		}
	}

	/** Original updateScreen/sendData: an empty field keeps its current value. */
	private void send() {
		TransformerBlockEntity te = menu.tile;
		if (te == null)
			return;
		int n1 = input.getValue().isEmpty() ? te.getN1() : parse(input.getValue());
		int n2 = input2.getValue().isEmpty() ? te.getN2() : parse(input2.getValue());
		if (n1 < 0 || n2 < 0)
			return;
		PacketDistributor.sendToServer(new ElectriPayloads.TransformerRatio(te.getBlockPos(), n1, n2));
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if ((input.isFocused() || input2.isFocused()) && key != 256) { //Escape still closes; other keys go to the fields
			if (this.getFocused() != null)
				this.getFocused().keyPressed(key, scan, mods);
			return true;
		}
		return super.keyPressed(key, scan, mods);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mx, int my) {
		TransformerBlockEntity te = menu.tile;
		if (te == null)
			return;
		int dx = imageWidth/2;
		int dy = imageHeight/2-4;
		String[] s = te.getRatioForDisplay().split(":");
		if (!input.isFocused() && input.getValue().isEmpty())
			g.drawString(font, s[0], dx-53, dy-15, 0xffffff, false);
		if (!input2.isFocused() && input2.getValue().isEmpty())
			g.drawString(font, s[1], dx+2, dy-15, 0xffffff, false);
		centered(g, ":", dx-9, dy-15);
		centered(g, String.format("Efficiency: %.2f%%", 100*te.getEfficiency()), dx-10, dy+6);
		centered(g, "Transformer", dx-8, 6);
	}

	private void centered(GuiGraphics g, String s, int x, int y) {
		g.drawString(font, s, x-font.width(s)/2, y, 4210752, false);
	}
}
